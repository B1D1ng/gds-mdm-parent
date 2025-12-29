package com.ebay.behavior.gds.mdm.signal.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import lombok.Getter;
import org.springframework.stereotype.Service;

@Getter
@Service
public class MetricsService {

    private final Counter signalMigrationHourlyCounter = Counter.builder("signal_migration_hourly_call_count")
        .description("SIGNAL MIGRATION HOURLY CALL COUNT")
        .register(Metrics.globalRegistry);

    private final Counter signalMigrationCustomCounter = Counter.builder("signal_migration_custom_call_count")
        .description("SIGNAL MIGRATION CALL COUNT")
        .register(Metrics.globalRegistry);

    public final Counter promoteToProductionCounter = Counter.builder("promote_to_production_count")
            .description("PROMOTE TO PRODUCTION COUNT")
            .register(Metrics.globalRegistry);

    public final Counter promoteToStagingCounter = Counter.builder("promote_to_staging_count")
            .description("PROMOTE TO STAGING COUNT")
            .register(Metrics.globalRegistry);

    public final Counter signalMigrationCounter = Counter.builder("signal_migration_count")
            .description("SIGNAL MIGRATION COUNT")
            .register(Metrics.globalRegistry);

    private final Counter exportSuccessCounter = Counter.builder("signal_export_success_count")
        .description("SIGNAL EXPORT SUCCESS COUNT")
        .register(Metrics.globalRegistry);

    private final Counter exportErrorCounter = Counter.builder("signal_export_error_count")
        .description("SIGNAL EXPORT ERROR COUNT")
        .register(Metrics.globalRegistry);

    private final Counter deleteSignalErrorCounter = Counter.builder("delete_signal_error_count")
            .description("DELETE SIGNAL ERROR COUNT")
            .register(Metrics.globalRegistry);

    public final Counter signalSuccessCounter = Counter.builder("signal_success_count")
            .description("Successful GET signal calls")
            .register(Metrics.globalRegistry);

    public final Counter signalErrorCounter = Counter.builder("signal_error_count")
            .description("Failed GET signal calls")
            .register(Metrics.globalRegistry);
}
