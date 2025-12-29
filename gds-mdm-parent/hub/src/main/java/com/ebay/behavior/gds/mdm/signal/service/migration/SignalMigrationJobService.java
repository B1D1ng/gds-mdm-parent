package com.ebay.behavior.gds.mdm.signal.service.migration;

import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.service.PlatformAware;
import com.ebay.behavior.gds.mdm.common.util.RandomUtils;
import com.ebay.behavior.gds.mdm.common.util.TimeUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.migration.SignalMigrationJobType;
import com.ebay.behavior.gds.mdm.signal.config.SignalMigrationJobConfiguration;
import com.ebay.behavior.gds.mdm.signal.model.migration.SignalMigrationJob;
import com.ebay.behavior.gds.mdm.signal.repository.migration.SignalMigrationJobRepository;
import com.ebay.behavior.gds.mdm.signal.service.MetricsService;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import io.micrometer.core.instrument.Counter;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import static com.ebay.behavior.gds.mdm.common.util.TimeUtils.nowPst;
import static com.ebay.behavior.gds.mdm.signal.common.model.migration.SignalMigrationJobStatus.FINISHED;
import static com.ebay.behavior.gds.mdm.signal.common.model.migration.SignalMigrationJobStatus.STARTED;
import static com.ebay.behavior.gds.mdm.signal.common.model.migration.SignalMigrationJobType.CUSTOM;
import static com.ebay.behavior.gds.mdm.signal.common.model.migration.SignalMigrationJobType.HOURLY;
import static com.ebay.behavior.gds.mdm.signal.util.ImportUtils.CJS;
import static com.ebay.behavior.gds.mdm.signal.util.ImportUtils.EJS;

@Slf4j
@Service
@Validated
public class SignalMigrationJobService extends PlatformAware {

    @Autowired
    private MetricsService metricsService;

    @Autowired
    private SignalMigrationJobRepository jobRepository;

    @Autowired
    private SignalMigrationJobConfiguration config;

    @Autowired
    private SignalMigrationService migrationService;

    private final ReentrantLock lock = new ReentrantLock(); // a single thread only can run a sync

    @PostConstruct
    private void init() {
        log.info("Signal migration is enabled");
    }

    @Scheduled(cron = "#{signalMigrationJobConfiguration.hourly.cron}")
    public void syncHourly() {
        RandomUtils.sleepRandomSeconds(20);
        syncScheduled(config.getHourly(), metricsService.getSignalMigrationHourlyCounter(), HOURLY);
    }

    @Scheduled(cron = "#{signalMigrationJobConfiguration.custom.cron}")
    public void syncCustom() {
        RandomUtils.sleepRandomSeconds(20);
        syncScheduled(config.getCustom(), metricsService.getSignalMigrationCustomCounter(), CUSTOM);
    }

    private boolean syncScheduled(SignalMigrationJobConfiguration.SchedulerConfig scheduler, Counter counter, SignalMigrationJobType type) {
        log.info("------------------------------------------------------------");
        val enabled = scheduler.getEnabled();
        if (!enabled) {
            log.info(String.format("Scheduled %s sync is disabled. Exiting.", type.getName()));
            return false;
        }

        val to = nowPst();
        val from = to.minusMinutes(scheduler.getRangeMinutes());
        return syncAll(from, to, counter, type, getStagedEnvironment());
    }

    @VisibleForTesting
    protected Optional<SignalMigrationJob> acquireJob(SignalMigrationJobType type, LocalDateTime ldt) {
        val key = type.getKey(ldt);
        var job = SignalMigrationJob.builder()
                .jobId(key)
                .status(STARTED)
                .build();
        try {
            job = jobRepository.saveAndFlush(job); // Flush the change to DB immediately to prevent other processes from starting the same job
            return Optional.of(job);
        } catch (DataIntegrityViolationException ex) {
            log.debug(String.format("%s sync [%s] already in progress. The error is: %s", type.getName(), type.getKey(ldt), ex.getMessage()));
            return Optional.empty();
        }
    }

    @Async
    public Future<Boolean> syncOnDemand(@NotNull LocalDateTime from, @NotNull LocalDateTime to) {
        val range = getStrRange(from, to);
        log.info(String.format("OnDemand migration job started for %s...", range));

        return new AsyncResult<>(syncAll(from, to, metricsService.getSignalMigrationCustomCounter(), CUSTOM, getStagedEnvironment()));
    }

    @VisibleForTesting
    protected boolean syncAll(LocalDateTime from, LocalDateTime to, Counter counter, SignalMigrationJobType type, Environment env) {
        Optional<SignalMigrationJob> maybeJob = Optional.empty();
        try {
            val timer = Stopwatch.createUnstarted();
            val range = getStrRange(from, to);
            var message = String.format("%s sync started for %s...", type.getName(), range);
            log.info(message);
            timer.start();

            val lockAcquired = lock.tryLock();
            if (!lockAcquired) {
                log.info(String.format("%s sync declined for %s - already in progress.", type.getName(), range));
                return false;
            }

            maybeJob = acquireJob(type, to); // job is used as a database locking mechanism
            if (maybeJob.isEmpty()) {
                message = String.format("%s sync already in progress. Exiting.", type.getName());
                log.info(message);
                return false;
            }

            migrationService.migrateAll(CJS, env, from, to);
            migrationService.migrateAll(EJS, env, from, to);

            message = String.format("%s sync DONE for %s [elapsed: %s]", type.getName(), range, timer);
            log.info(message);
            counter.increment();
            return true;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            if (maybeJob.isPresent()) {
                val job = maybeJob.get();
                job.setStatus(FINISHED);
                jobRepository.save(job);
            }
        }
    }

    private String getStrRange(LocalDateTime from, LocalDateTime to) {
        return String.format("range [%s - %s]", TimeUtils.toDateString(from), TimeUtils.toDateString(to));
    }
}
