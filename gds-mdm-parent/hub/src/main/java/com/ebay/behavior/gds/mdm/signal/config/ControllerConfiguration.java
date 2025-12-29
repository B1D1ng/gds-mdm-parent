package com.ebay.behavior.gds.mdm.signal.config;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "controller")
public class ControllerConfiguration {

    @NotNull
    @Getter
    @Setter
    private Boolean stagingEnabled;

    @NotNull
    @Getter
    @Setter
    private Boolean productionEnabled;
}
