package com.ebay.behavior.gds.mdm.common.config;

import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "sitesso")
@ConditionalOnProperty(prefix = "sitesso", name = "enable")
public class SiteSsoConfiguration {

    @NotBlank
    private String oidcAuthRedirectUrl;

    @NotBlank
    private String sessionPatronusKeyRef;

    @NotBlank
    private String filterLoginUrl;

    @NotBlank
    private String filterLogoutUrl;

    @NotBlank
    private String oidcScopes;

    @NotBlank
    private String filterAllowedCrossDomains;
}
