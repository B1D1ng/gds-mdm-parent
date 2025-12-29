package com.ebay.behavior.gds.mdm.signal.service.migration;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.VersionedIdWithStatus;
import com.ebay.behavior.gds.mdm.common.util.TimeUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.LegacySignalRecord;
import com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.datatype.SignalDefinition;
import com.ebay.behavior.gds.mdm.signal.common.model.migration.MigrationStepStatus;
import com.ebay.behavior.gds.mdm.signal.common.model.migration.SignalMigrationStatus;
import com.ebay.behavior.gds.mdm.signal.service.DomainLookupService;
import com.ebay.behavior.gds.mdm.signal.service.MetricsService;
import com.ebay.behavior.gds.mdm.signal.service.PlanService;
import com.ebay.behavior.gds.mdm.signal.service.PlatformLookupService;
import com.ebay.behavior.gds.mdm.signal.service.SignalImportService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedSignalService;
import com.ebay.behavior.gds.mdm.signal.usecase.PromoteToProductionUsecase;
import com.ebay.behavior.gds.mdm.signal.usecase.PromoteToStagingUsecase;
import com.ebay.behavior.gds.mdm.signal.usecase.UnstageUsecase;
import com.ebay.com.google.common.collect.Lists;

import com.google.common.annotations.VisibleForTesting;
import io.vavr.control.Either;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static com.ebay.behavior.gds.mdm.common.util.TimeUtils.LEGACY_DATE_FORMATTER;
import static com.ebay.behavior.gds.mdm.signal.common.model.migration.SignalMigrationStatus.startStep;
import static com.ebay.behavior.gds.mdm.signal.model.SpecialPlanType.MIGRATION;
import static com.ebay.behavior.gds.mdm.signal.util.ImportUtils.createImportPlanIfAbsent;
import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.groupingBy;

/**
 * This is a summary of the migration process (for production environment only):
 * 1. writeToUnstaged
 * 2. remotePromote - inject to UDC staging
 * 2.1  write to remote unstaged - StagingUdcInjectionResource/importUnstaged
 * 2.2  write to remote staged - StagingUdcInjectionResource/importStaged
 * 3. localPromote - inject to UDC production
 * 4  write to local staged - signalImportService/importStagedSignalIfAbsent
 */
@Slf4j
@Service
@Validated
public class SignalMigrationService {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private LegacyMapper legacyMapper;

    @Autowired
    private DomainLookupService domainService;

    @Autowired
    private LegacySignalReadService legacySignalReadService;

    @Autowired
    private SignalImportService signalImportService;

    @Autowired
    private PlanService planService;

    @Autowired
    private UnstagedSignalService unstagedSignalService;

    @Autowired
    private UnstageUsecase unstageUsecase;

    @Autowired
    private PromoteToStagingUsecase remotePromoteUsecase;

    @Autowired
    private PromoteToProductionUsecase localPromoteUsecase;

    @Autowired
    private MetricsService metricsService;

    @Autowired
    private PlatformLookupService platformService;

    private class MigrationTask implements Callable<List<SignalMigrationStatus>> {
        private final List<LegacySignalRecord> partition;
        private final long planId;
        private final boolean skipRemote;
        private final Environment currEnv;

        public MigrationTask(List<LegacySignalRecord> partition, long planId, boolean skipRemote, Environment currEnv) {
            this.partition = partition;
            this.planId = planId;
            this.skipRemote = skipRemote;
            this.currEnv = currEnv;
        }

        @Override
        public List<SignalMigrationStatus> call() {
            List<SignalMigrationStatus> statuses = new ArrayList<>();
            int num = 0;
            for (val legacySignal : partition) {
                num++;
                statuses.addAll(migrate(legacySignal, planId, skipRemote, currEnv));

                var signalName = legacySignal.getSignalId();
                val signalVersions = legacySignal.getVersions();
                if (CollectionUtils.isNotEmpty(signalVersions)) {
                    signalName = signalVersions.get(0).getName();
                }

                log.info("Thread: {} - migrated legacy signal {} ({} out of {})", Thread.currentThread().getId(), signalName, num, partition.size());
            }
            return statuses;
        }
    }

    public void migrateAll(@NotNull String platformName, @NotNull Environment currEnv,
                           @NotNull LocalDateTime from, @NotNull LocalDateTime to) {
        log.info("migrating all signals (platformName: {}, currEnv: {}) from {} to {}", platformName, currEnv, from, to);
        val legacySignals = getAll(platformName, from, to);
        migrateAll(platformName, legacySignals, false, currEnv);
        log.info("Migration done.");
    }

    @Async
    public CompletableFuture<List<SignalMigrationStatus>> migrateAll(@NotNull String platformName, boolean skipRemote, @NotNull Environment currEnv) {
        try {
            log.info("Asynchronous migrating all signals (platformName: {}, currEnv: {})", platformName, currEnv);
            val legacySignals = getAll(platformName);
            log.info("Legacy signals to migrate: {}", legacySignals.size());
            val statuses = migrateAll(platformName, legacySignals, skipRemote, currEnv);
            log.info("Asynchronous migration done.");
            return CompletableFuture.completedFuture(statuses);
        } catch (Exception ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    private List<SignalMigrationStatus> migrateAll(String platformName, List<LegacySignalRecord> legacySignals, boolean skipRemote, Environment currEnv) {
        if (legacySignals.isEmpty()) {
            return List.of();
        }
        val platform = platformService.getByName(platformName);
        val planId = createImportPlanIfAbsent(MIGRATION, platform, planService, domainService);
        val migrationStatuses = new ArrayList<SignalMigrationStatus>();
        int numThreads = 4;
        val executorService = Executors.newFixedThreadPool(numThreads);
        try {
            // Partition the list into few sub-lists
            val partitions = Lists.partition(legacySignals, (int) Math.ceil((double) legacySignals.size() / numThreads));
            List<Callable<List<SignalMigrationStatus>>> tasks = new ArrayList<>();

            for (val partition : partitions) {
                tasks.add(new MigrationTask(partition, planId, skipRemote, currEnv));
            }
            log.info("Number of partitions/threads: {}", partitions.size());

            // Execute the tasks and collect results
            val futures = executorService.invokeAll(tasks);
            for (val future : futures) {
                migrationStatuses.addAll(future.get());
            }
        } catch (Exception ex) {
            log.info("migration failed");
            log.error(ex.getMessage(), ex);
        } finally {
            log.info("executorService shutdown");
            executorService.shutdown();
        }
        return migrationStatuses;
    }

    public List<SignalMigrationStatus> migrate(@NotBlank String name, @NotNull String platformName, boolean skipRemote, @NotNull Environment currEnv) {
        log.info("migrating signal {} (platformName: {}, currEnv: {})", name, platformName, currEnv);
        val legacySignal = getLegacySignal(name, platformName);
        val platform = platformService.getByName(platformName);
        val planId = createImportPlanIfAbsent(MIGRATION, platform, planService, domainService);

        return migrate(legacySignal, planId, skipRemote, currEnv);
    }

    @VisibleForTesting
    protected List<SignalMigrationStatus> migrate(LegacySignalRecord legacySignal, Long planId, boolean skipRemote, Environment currEnv) {
        val signalVersions = legacyMapper.mapLegacySignalRecord(legacySignal, planId);
        if (CollectionUtils.isEmpty(signalVersions)) {
            return List.of();
        }

        val statuses = signalVersions.stream()
                .map(signalVersion -> startStep(signalVersion.getName(), signalVersion.getVersion()))
                .toList();

        for (int i = 0; i < signalVersions.size(); i++) {
            val signal = writeSignalToUnstaged(signalVersions.get(i), i, statuses);
            if (Objects.isNull(signal)) {
                continue;
            }

            entityManager.detach(signal);
            boolean isStaged = writeSignalToStaged(signal, i, statuses);
            if (!isStaged) {
                continue;
            }

            if (!skipRemote) {
                remotePromote(signal, i, statuses, currEnv); // write to UDC staging
            }

            localPromote(signal, i, statuses, currEnv); // write to UDC production
        }
        metricsService.getSignalMigrationCounter().increment();
        return statuses;
    }

    private boolean writeSignalToStaged(UnstagedSignal signal, int index, List<SignalMigrationStatus> statuses) {
        val currStatus = statuses.get(index);
        try {
            signalImportService.importStagedSignalIfAbsent(signal);
            currStatus.setId(signal.getId());
            currStatus.setOkStatus(MigrationStepStatus.STAGED);
            return true;
        } catch (Exception ex) {
            log.error("Failed to write signal to staged: " + signal.getName(), ex);
            currStatus.setFailedStatus(MigrationStepStatus.STAGED, ex.getMessage());
            return false;
        }
    }

    private UnstagedSignal writeSignalToUnstaged(UnstagedSignal signal, int index, List<SignalMigrationStatus> statuses) {
        val currStatus = statuses.get(index);
        try {
            var created = unstageUsecase.createWithAssociations(signal);
            created = unstagedSignalService.getByIdWithAssociationsRecursive(created.getSignalId());
            currStatus.setId(created.getId());
            currStatus.setOkStatus(MigrationStepStatus.UNSTAGED);
            return created;
        } catch (Exception ex) {
            log.error("Failed to write signal to unstaged: " + signal.getName(), ex);
            currStatus.setFailedStatus(MigrationStepStatus.UNSTAGED, ex.getMessage());
            return null;
        }
    }

    private void localPromote(UnstagedSignal signal, int index, List<SignalMigrationStatus> statuses, Environment currEnv) {
        if (statuses.get(index).isFailed()) {
            return;
        }
        val stagedEitherStatus = localPromoteUsecase.localInjectMetadata(signal);
        val currStatus = Environment.STAGING.equals(currEnv) ? MigrationStepStatus.STAGING : MigrationStepStatus.PRODUCTION;

        if (stagedEitherStatus.isLeft()) {
            val message = stagedEitherStatus.getLeft().getMessage();
            log.error("localInjectMetadata failed for signal {}: {} ", signal.getName(), message);
        }

        updateEnvironmentAndMigrationStep(signal.getSignalId(), currEnv, stagedEitherStatus, index, statuses, currStatus);
    }

    protected void remotePromote(UnstagedSignal signal, int index, List<SignalMigrationStatus> statuses, Environment env) {
        if (statuses.get(index).isFailed() || Environment.STAGING.equals(env)) {
            return;
        }

        val stagedStatus = remotePromoteUsecase.remoteInjectMetadata(signal);
        if (stagedStatus.isLeft()) {
            val message = stagedStatus.getLeft().getMessage();
            log.error("remotePromote failed for signal {}: {} ", signal.getName(), message);
        }

        updateEnvironmentAndMigrationStep(signal.getSignalId(), env, stagedStatus, index, statuses, MigrationStepStatus.STAGING);
    }

    private void updateEnvironmentAndMigrationStep(VersionedId signalId, Environment env, Either<VersionedIdWithStatus, VersionedIdWithStatus> stagedStatus,
                                                   int index, List<SignalMigrationStatus> statuses, MigrationStepStatus currStatus) {
        if (stagedStatus.isLeft()) {
            val message = stagedStatus.getLeft().getMessage();
            statuses.get(index).setFailedStatus(currStatus, message);
            return;
        }

        statuses.get(index).setOkStatus(currStatus);
        unstagedSignalService.updateEnvironment(signalId, env);
    }

    private List<LegacySignalRecord> getAll(@NotNull String platformName) {
        val signals = legacySignalReadService.readAll(platformName);
        return toLegacySignalRecord(signals);
    }

    private List<LegacySignalRecord> getAll(@NotNull String platformName, @NotNull LocalDateTime from, @NotNull LocalDateTime to) {
        val signals = legacySignalReadService.readAll(platformName).stream()
                .filter(signal -> {
                    try {
                        val updatedTime = TimeUtils.toLocalDateTime(signal.getUpdatedTime(), LEGACY_DATE_FORMATTER);
                        return updatedTime.isAfter(from) && updatedTime.isBefore(to);
                    } catch (Exception ex) {
                        log.warn("Failed to parse updatedTime for signal: " + signal.getId(), ex);
                        return false;
                    }
                })
                .toList();

        if (signals.isEmpty()) {
            log.info("No signals to migrate between " + from + " and " + to);
            return List.of();
        }

        return toLegacySignalRecord(signals);
    }

    private List<LegacySignalRecord> toLegacySignalRecord(List<SignalDefinition> signals) {
        val idToDefinitions = signals.stream().collect(groupingBy(SignalDefinition::getId));

        return idToDefinitions.entrySet().stream()
                .map(entry -> {
                    val signalRecord = new LegacySignalRecord();
                    signalRecord.setSignalId(entry.getKey());
                    val versions = entry.getValue().stream()
                            .sorted(comparingLong(SignalDefinition::getVersion))
                            .toList();
                    signalRecord.setVersions(versions);
                    return signalRecord;
                }).toList();
    }

    public LegacySignalRecord getLegacySignal(@NotBlank String name, @NotNull String platformName) {
        val all = legacySignalReadService.readAll(platformName);
        val matched = all.stream()
                .filter(signal -> signal.getName().equals(name))
                .sorted(comparingLong(SignalDefinition::getVersion))
                .toList();

        if (matched.isEmpty()) {
            throw new DataNotFoundException(String.format("Legacy signal %s not found: ", name));
        }

        val legacySignal = new LegacySignalRecord();
        legacySignal.setSignalId(matched.get(0).getId());
        legacySignal.setVersions(matched);
        return legacySignal;
    }
}
