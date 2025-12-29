package com.ebay.behavior.gds.mdm.signal.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Getter
@Setter
@Validated
@Profile({"!IT", "!Dev"})
@Configuration
@ConfigurationProperties(prefix = "hadoop")
@ConditionalOnProperty(prefix = "hadoop", name = "enable", havingValue = "true", matchIfMissing = false)
public class HadoopConfiguration {

    @Valid
    @NotNull
    private KiteConfig kite;

    @Valid
    @NotNull
    private HiveConfig hive;

    @Valid
    @NotNull
    private HdfsConfig hdfs;

    @Getter
    @Setter
    public static class KiteConfig {

        @NotBlank
        private String serverEndpoint;

        @NotBlank
        private String keystoneApiKeyPath;

        @NotBlank
        private String keystoneApiSecretPath;

        @NotBlank
        private String krb5ConfLocation;

        @NotBlank
        private String userPrincipal;
    }

    @Getter
    @Setter
    public static class HiveConfig {

        @NotBlank
        private String driverName;

        @NotBlank
        private String carmelUrl;

        @NotBlank
        private String krb5TicketCacheLocationPrefix;
    }

    @Getter
    @Setter
    public static class HdfsConfig {

        @NotBlank
        private String path;
    }
}