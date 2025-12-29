package com.ebay.behavior.gds.mdm.signal.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.UNKNOWN;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.csvStringToSet;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "governance")
public class GovernanceConfiguration {

    @NotBlank
    private String admins;

    @NotBlank
    private String moderators;

    private Set<String> moderatorSet;

    private Set<String> adminSet;

    @NotEmpty
    private Map<String, @Valid Action> actions;

    @PostConstruct
    private void init() {
        this.moderatorSet = csvStringToSet(moderators);
        this.adminSet = csvStringToSet(admins);
    }

    public boolean isModerator(String user) {
        if (user == null || UNKNOWN.equals(user)) {
            return false;
        }
        return moderatorSet.contains(user);
    }

    public boolean isAdmin(String user) {
        if (user == null || UNKNOWN.equals(user)) {
            return false;
        }
        return adminSet.contains(user);
    }

    @Getter
    @Setter
    public static class Action {

        @NotBlank
        private String subject;

        @NotBlank
        private String template;
    }
}