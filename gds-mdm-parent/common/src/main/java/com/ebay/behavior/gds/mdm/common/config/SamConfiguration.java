package com.ebay.behavior.gds.mdm.common.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "sam")
public class SamConfiguration {

    @NotEmpty
    private List<@Valid SamConfig> fidelius;

    private <T extends ListElement> T getListElement(ListElement searchElement, List<T> list) {
        return CollectionUtils.emptyIfNull(list).stream()
                .filter(cfg -> cfg.getDomain().equals(searchElement.getDomain()) && cfg.getName().equals(searchElement.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("SamConfig not found for: domain=%s, name=%s", searchElement.getDomain(), searchElement.getName())));
    }

    public SamConfig getFideliusConfig(ListElement searchElement) {
        return getListElement(searchElement, fidelius);
    }

    public interface ListElement {
        String getDomain();

        String getName();
    }

    @Data
    @AllArgsConstructor
    public static class ConfigListElement implements ListElement {
        private String domain;
        private String name;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SamConfig implements ListElement {
        @NotBlank
        private String domain;

        @NotBlank
        private String name;

        @NotBlank
        private String path;
    }
}