package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.config.UdcConfiguration;
import com.ebay.behavior.gds.mdm.common.exception.ExternalCallException;
import com.ebay.behavior.gds.mdm.common.exception.PartialSuccessException;
import com.ebay.behavior.gds.mdm.common.exception.UdcException;
import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.model.VersionedIdWithStatus;
import com.ebay.behavior.gds.mdm.signal.common.model.Plan;
import com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus;
import com.ebay.behavior.gds.mdm.signal.common.model.PlanUserAction;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.service.MetadataWriteService;
import com.ebay.behavior.gds.mdm.signal.repository.UnstagedSignalRepository;
import com.ebay.behavior.gds.mdm.signal.util.ValidationUtils;

import com.google.common.annotations.VisibleForTesting;
import io.vavr.control.Either;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.Validate;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.ebay.behavior.gds.mdm.common.model.Environment.STAGING;
import static com.ebay.behavior.gds.mdm.common.model.Environment.UNSTAGED;
import static com.ebay.behavior.gds.mdm.signal.common.model.PlanUserAction.PROMOTE_TO_PROD;
import static com.ebay.behavior.gds.mdm.signal.common.model.PlanUserAction.PROMOTE_TO_STAGING;

/*
 * This is a summary of the promote process for different environments:
 * Staging:
 *    local:
 *       write to unstaged - n/a
 *       update unstaged status - finalizePromotedSignals/updatePromotedUnstagedSignals
 *        write to staged - finalizePromotedSignals/updatePromotedStagedSignals/importStagedSignal
 *   remote:
 *       write to remote unstaged - StagingUdcInjectionResource/importUnstaged
 *       write to remote staged - StagingUdcInjectionResource/importStaged
 *       update unstaged status - finalizePromotedSignals/updatePromotedUnstagedSignals
 *       write to staged - finalizePromotedSignals/updatePromotedStagedSignals/importStagedSignal
 *
 * Production:
 *   local:
 *       write to unstaged - no need
 *       update unstaged status - finalizePromotedSignals/updatePromotedUnstagedSignals
 *       update staged status - finalizePromotedSignals/updatePromotedStagedSignals/importStagedSignal
 *   remote: n/a
 */

@Slf4j
public abstract class PromoteService {

    @Autowired
    protected PlanService planService;

    @Autowired
    private PlanActionService planActionService;

    @Autowired
    private UnstagedSignalService unstagedSignalService;

    @Autowired
    private StagedSignalService stagedSignalService;

    @Autowired
    private MetadataWriteService metadataWriteService;

    @Autowired
    private SignalImportService importService;

    @Autowired
    private UnstagedSignalRepository unstagedSignalRepository;

    @Autowired
    private UdcConfiguration config;

    @Autowired
    private EntityManager entityManager;

    public abstract List<@Valid VersionedIdWithStatus> localPromote(@PositiveOrZero long planId);

    protected List<UnstagedSignal> getDetachedSignals(Plan plan, PlanUserAction action) {
        val planId = plan.getId();
        planActionService.validateAction(plan, action);
        val currentEnv = PROMOTE_TO_STAGING.equals(action) ? UNSTAGED : STAGING;

        val signals = planService.getSignals(planId).stream()
                .peek(ValidationUtils::validateCompleted)
                .filter(signal -> signal.getEnvironment().equals(currentEnv)) // we can redo the promotion and skip already promoted signals
                .map(signal -> unstagedSignalService.getByIdWithAssociationsRecursive(signal.getSignalId()))
                .peek(signal -> Hibernate.initialize(signal.getPlatform()))
                .peek(entityManager::detach) // from this point on, we don't want to modify the entities in the persistence context, until handleResponse()
                .toList();

        Validate.isTrue(!signals.isEmpty(), "No signals found for plan with id %d", planId);
        return signals;
    }

    /**
     * Promotes a signal to UDC.
     * Ingestion order per signal: events, attributes, signal, fields
     *
     * @param signal An UnstagedSignal to be promoted.
     * @return Either A successful VersionedIdWithStatus or a failed VersionedIdWithStatus.
     */
    public Either<VersionedIdWithStatus, VersionedIdWithStatus> localInjectMetadata(UnstagedSignal signal) {
        val signalId = signal.getSignalId();
        String signalEntityId;
        try {
            signalEntityId = metadataWriteService.upsert(signal, config.getDataSource());
            log.info(String.format("Signal with id %s successfully injected to UDC Portal", signalId));
            return Either.right(VersionedIdWithStatus.okStatus(signalId.getId(), signalId.getVersion(), signalEntityId));
        } catch (UdcException ex) {
            val error = ex.getMessage();
            log.error(String.format("Failed to inject Signal (id: %d:, ver: %d). Error: %s", signalId.getId(), signalId.getVersion(), error));
            return Either.left(VersionedIdWithStatus.failedStatus(signalId.getId(), signalId.getVersion(), error));
        }
    }

    @VisibleForTesting
    public List<VersionedIdWithStatus> finalizePromotedSignals(long planId, Environment env,
                                                               List<VersionedIdWithStatus> failed, List<VersionedIdWithStatus> promoted) {
        Validate.isTrue(STAGING.equals(env) || Environment.PRODUCTION.equals(env), "Promote to %s is not supported", env);

        if (promoted.isEmpty() && failed.isEmpty()) {
            var message = String.format("Promoting plan with id %d failed: both promoted and failed lists are empty", planId);
            log.error(message);
            throw new IllegalStateException(message);
        }

        val plan = planService.getById(planId);
        updatePromotedUnstagedSignals(promoted, env, plan.getDomain(), plan.getOwners());
        updatePromotedStagedSignals(promoted, env);

        val planStatus = STAGING.equals(env) ? PlanStatus.STAGING : PlanStatus.PRODUCTION;
        val planAction = STAGING.equals(env) ? PROMOTE_TO_STAGING : PROMOTE_TO_PROD;
        plan.setStatus(planStatus);

        if (failed.isEmpty()) {
            log.info("Promoting plan with id {} successfully completed", planId);
            promoted.forEach(res -> log.info(String.format("Promoted signal EntityId: %s", res.getMessage())));
            planService.update(plan, planAction);
            return promoted;
        }

        if (promoted.isEmpty()) {
            val message = String.format("Promoting plan with id %d failed: [%s]",
                    planId, failed.stream().map(VersionedIdWithStatus::getMessage).collect(Collectors.joining(", ")));
            log.error(message);
            throw new ExternalCallException(message);
        }

        planService.update(plan, planAction);
        throw new PartialSuccessException(String.format("Promoting plan with id %s partially failed", planId), ListUtils.union(promoted, failed));
    }

    private void updatePromotedUnstagedSignals(List<VersionedIdWithStatus> promoted, Environment env, String domain, String owners) {
        if (CollectionUtils.isEmpty(promoted)) {
            return;
        }

        val signalIds = promoted.stream()
                .map(VersionedIdWithStatus::toVersionedId)
                .toList();

        val signals = unstagedSignalRepository.findAllById(signalIds);

        signals.forEach(signal -> {
            signal.setEnvironment(env);
            signal.setDataSource(config.getDataSource());
            if (Objects.nonNull(domain) && !Environment.PRODUCTION.equals(env)) {
                signal.setDomain(domain);
            }
            if (Objects.nonNull(owners) && !Environment.PRODUCTION.equals(env)) {
                signal.setOwners(owners);
            }
        });

        unstagedSignalRepository.saveAll(signals);
    }

    private void updatePromotedStagedSignals(List<VersionedIdWithStatus> promoted, Environment env) {
        if (CollectionUtils.isEmpty(promoted)) {
            return;
        }

        val signalIds = promoted.stream()
                .map(VersionedIdWithStatus::toVersionedId)
                .toList();

        signalIds.forEach(signalId -> {
            val unstagedSignal = unstagedSignalService.getByIdWithAssociationsRecursive(signalId);
            Validate.isTrue(unstagedSignal.getEnvironment() == env, "Unstaged signal EntityId: %s environment mismatch", signalId);

            if (env == STAGING) {
                entityManager.detach(unstagedSignal);
                importService.importStagedSignal(unstagedSignal);
            } else {
                stagedSignalService.updateToProductionEnvironment(signalId);
            }
        });
    }
}
