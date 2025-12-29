package com.ebay.behavior.gds.mdm.signal.usecase;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.VersionedIdWithStatus;
import com.ebay.behavior.gds.mdm.signal.common.model.PlanUserAction;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.service.MetricsService;
import com.ebay.behavior.gds.mdm.signal.service.PromoteService;
import com.ebay.behavior.gds.mdm.signal.service.StagingSyncClient;

import io.vavr.control.Either;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.ebay.behavior.gds.mdm.common.model.Environment.PRODUCTION;

@Slf4j
@Service
@Validated
public class PromoteToProductionUsecase extends PromoteService {

    @Autowired
    private StagingSyncClient stagingInjector;

    @Autowired
    private MetricsService metricsService;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<VersionedIdWithStatus> localPromote(@PositiveOrZero long planId) {
        return promote(planId, this::localInjectMetadata, this::remoteUpdateToProductionEnvironment);
    }

    public Either<VersionedIdWithStatus, VersionedIdWithStatus> remoteUpdateToProductionEnvironment(UnstagedSignal signal) {
        try {
            val signalId = stagingInjector.patch(null, null, signal.getSignalId(), VersionedId.class);
            return Either.right(VersionedIdWithStatus.okStatus(signalId));
        } catch (Exception ex) {
            val error = ex.getMessage();
            log.error(String.format("Failed to remotely update Signal environment (id: %d:, ver: %d). Error: %s", signal.getId(), signal.getVersion(), error));
            return Either.left(VersionedIdWithStatus.failedStatus(signal.getId(), signal.getVersion(), error));
        }
    }

    private List<VersionedIdWithStatus> promote(long planId, Function<UnstagedSignal, Either<VersionedIdWithStatus, VersionedIdWithStatus>> localInjectFunction,
                                                Function<UnstagedSignal, Either<VersionedIdWithStatus, VersionedIdWithStatus>> remoteUpdateFunction) {
        val plan = planService.getById(planId);
        val signals = getDetachedSignals(plan, PlanUserAction.PROMOTE_TO_PROD);
        log.info(String.format("Prepared %d signals to be promoted into a production environment for a plan: id %s", signals.size(), planId));

        val promoted = new ArrayList<VersionedIdWithStatus>();
        val failed = new ArrayList<VersionedIdWithStatus>();

        signals.forEach(signal -> {
            signal.setEnvironment(PRODUCTION);
            val localResult = localInjectFunction.apply(signal);
            var result = localResult;

            if (localResult.isRight()) {
                result = remoteUpdateFunction.apply(signal);
            }

            result.peek(promoted::add).peekLeft(failed::add);
        });
        metricsService.getPromoteToProductionCounter().increment();
        return finalizePromotedSignals(plan.getId(), PRODUCTION, failed, promoted);
    }
}
