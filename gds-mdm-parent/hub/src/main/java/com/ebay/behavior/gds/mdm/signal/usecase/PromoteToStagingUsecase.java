package com.ebay.behavior.gds.mdm.signal.usecase;

import com.ebay.behavior.gds.mdm.common.config.UdcConfiguration;
import com.ebay.behavior.gds.mdm.common.model.QueryParam;
import com.ebay.behavior.gds.mdm.common.model.VersionedIdWithStatus;
import com.ebay.behavior.gds.mdm.signal.common.model.PlanUserAction;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.service.MetricsService;
import com.ebay.behavior.gds.mdm.signal.service.PromoteService;
import com.ebay.behavior.gds.mdm.signal.service.StagingSyncClient;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.vavr.control.Either;
import jakarta.validation.constraints.PositiveOrZero;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static com.ebay.behavior.gds.mdm.common.model.Environment.STAGING;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.DATA_SOURCE;

@Slf4j
@Service
@Validated
public class PromoteToStagingUsecase extends PromoteService {

    @Autowired
    private StagingSyncClient stagingInjector;

    @Autowired
    private UdcConfiguration config;

    @Autowired
    private MetricsService metricsService;

    private TimeLimiter timeLimiter;
    private ExecutorService executorService;

    @PostConstruct
    private void init() {
        val config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(45))
                .build();

        val registry = TimeLimiterRegistry.of(config);
        timeLimiter = registry.timeLimiter("remoteInjectMetadataTimeLimiter");
        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<VersionedIdWithStatus> localPromote(@PositiveOrZero long planId) {
        // Staged tables not updated by design, since this flow is not initiated by a production portal
        return promote(planId, this::localInjectMetadata);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<VersionedIdWithStatus> remotePromote(@PositiveOrZero long planId) {
        return promote(planId, this::remoteInjectMetadata);
    }

    private List<VersionedIdWithStatus> promote(long planId, Function<UnstagedSignal, Either<VersionedIdWithStatus, VersionedIdWithStatus>> injectFunction) {
        val plan = planService.getById(planId);
        val signals = getDetachedSignals(plan, PlanUserAction.PROMOTE_TO_STAGING);
        log.info(String.format("Prepared %d signals to be promoted into a staging environment for a plan: id %s", signals.size(), planId));

        val promoted = new ArrayList<VersionedIdWithStatus>();
        val failed = new ArrayList<VersionedIdWithStatus>();

        signals.forEach(signal -> {
            signal.setOwners(plan.getOwners());
            signal.setDomain(plan.getDomain());
            signal.setEnvironment(STAGING);
            val result = injectFunction.apply(signal);
            result.peek(promoted::add).peekLeft(failed::add);
        });
        metricsService.getPromoteToStagingCounter().increment();
        return finalizePromotedSignals(planId, STAGING, failed, promoted);
    }

    /**
     * Call a staging host, so it will promote a signal to UDC.
     * Uses a timeout to ensure the operation does not hang indefinitely.
     *
     * @param signal An UnstagedSignal to be promoted.
     * @return Either A successful VersionedIdWithStatus or a failed VersionedIdWithStatus.
     */
    public Either<VersionedIdWithStatus, VersionedIdWithStatus> remoteInjectMetadata(UnstagedSignal signal) {
        try {
            val future = CompletableFuture.supplyAsync(() -> remoteInjectMetadataSupplier(signal), executorService);
            return Either.right(timeLimiter.executeFutureSupplier(() -> future)); // Use the TimeLimiter to enforce the timeout
        } catch (TimeoutException ex) {
            val msg = "Timeout occurred";
            log.error(String.format("Failed to remotely inject Signal with timeout (id: %d, ver: %d). Error: %s", signal.getId(), signal.getVersion(), msg));
            return Either.left(VersionedIdWithStatus.failedStatus(signal.getId(), signal.getVersion(), msg));
        } catch (Exception ex) {
            val msg = ex.getMessage();
            log.error(String.format("Failed to remotely inject Signal with timeout (id: %d, ver: %d). Error: %s", signal.getId(), signal.getVersion(), msg));
            return Either.left(VersionedIdWithStatus.failedStatus(signal.getId(), signal.getVersion(), msg));
        }
    }

    private VersionedIdWithStatus remoteInjectMetadataSupplier(UnstagedSignal signal) {
        val queryParam = new QueryParam(DATA_SOURCE, config.getDataSource().getValue());
        val signalEntityId = stagingInjector.post(null, List.of(queryParam), signal, String.class);
        return VersionedIdWithStatus.okStatus(signal.getId(), signal.getVersion(), signalEntityId);
    }
}
