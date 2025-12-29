package com.ebay.behavior.gds.mdm.common.config;

import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.datagov.pushingestion.PushIngestionClientEnvironment;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "udc")
@ConditionalOnProperty(prefix = "udc", name = "enable", havingValue = "true")
public class UdcConfiguration {

    @NotNull
    private PushIngestionClientEnvironment env;

    @NotNull
    private UdcDataSourceType dataSource;
}
