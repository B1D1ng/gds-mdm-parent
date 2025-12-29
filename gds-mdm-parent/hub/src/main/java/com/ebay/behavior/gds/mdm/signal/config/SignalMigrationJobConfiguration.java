package com.ebay.behavior.gds.mdm.signal.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "signal-migration-job")
public class SignalMigrationJobConfiguration {

    @Valid
    @NotNull
    private SchedulerConfig hourly;

    @Valid
    @NotNull
    private SchedulerConfig custom;

    @Getter
    @Setter
    public static class SchedulerConfig {

        @NotNull
        private Boolean enabled;

        @NotBlank
        private String cron;

        @NotBlank
        private String range;

        public long getRangeMinutes() {
            return DurationStyle.detectAndParse(range).toMinutes();
        }
    }
}
