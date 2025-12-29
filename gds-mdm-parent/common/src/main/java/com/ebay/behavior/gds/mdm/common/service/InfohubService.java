package com.ebay.behavior.gds.mdm.common.service;

import com.ebay.behavior.gds.mdm.common.model.external.infohub.Project;
import com.ebay.behavior.gds.mdm.common.model.external.infohub.ProjectById;
import com.ebay.behavior.gds.mdm.common.model.external.infohub.ProjectView;
import com.ebay.behavior.gds.mdm.common.model.external.infohub.User;
import com.ebay.behavior.gds.mdm.common.model.external.infohub.Users;
import com.ebay.behavior.gds.mdm.common.service.token.EsAuthTokenGenerator;
import com.ebay.behavior.gds.mdm.common.util.DevZoneUtils;

import jakarta.inject.Named;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.client.WebTarget;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static com.ebay.behavior.gds.mdm.common.config.CacheConfiguration.ALL_JIRA_PROJECTS_CACHE;
import static com.ebay.behavior.gds.mdm.common.config.CacheConfiguration.ALL_USERS_CACHE;
import static com.ebay.behavior.gds.mdm.common.config.CacheConfiguration.JIRA_PROJECT_CACHE;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.AUTOWIRING_INSPECTION;
import static java.util.Locale.US;

@Slf4j
@Service
@Validated
public class InfohubService extends AbstractRestGetClient {

    public static final String INFOHUB_CLIENT_NAME = "infohubService.infohubClient";
    protected static final String PROJECTS_PATH = "/project/view.service";
    protected static final String USERS_PATH = "/user/allActiveUsers.service";

    @Getter
    private final String path = null; // needed for AbstractRestGetClient logic

    @Getter
    @Autowired
    private EsAuthTokenGenerator tokenGenerator;

    @Getter
    @Autowired
    @Named(INFOHUB_CLIENT_NAME)
    @SuppressWarnings(AUTOWIRING_INSPECTION)
    private WebTarget target;

    @Cacheable(value = ALL_JIRA_PROJECTS_CACHE, sync = true)
    public List<Project> readAllProjects() {
        if (DevZoneUtils.isDevZone()) {
            log.info("DevZone detected, skipping Jira project list cache load");
            return List.of();
        }

        log.info("Jira project cache miss");
        val projects = get(PROJECTS_PATH, ProjectView.class).object();

        if (Objects.isNull(projects)) {
            return List.of();
        }

        return projects;
    }

    @Cacheable(value = JIRA_PROJECT_CACHE, sync = true)
    public Project getProjectByKey(@NotBlank String key) {
        if (DevZoneUtils.isDevZone()) {
            log.info("DevZone detected, skipping Jira project cache load");
            return null;
        }

        val path = "/project/" + key + "/view.service";
        return get(path, ProjectById.class).object();
    }

    @Cacheable(value = ALL_USERS_CACHE, sync = true)
    public List<User> readAllUsers() {
        if (DevZoneUtils.isDevZone()) {
            log.info("DevZone detected, skipping Users cache load");
            return List.of();
        }

        log.info("Users cache miss");
        val users = get(USERS_PATH, Users.class).object();

        if (Objects.isNull(users)) {
            return List.of();
        }

        return users;
    }

    public List<Project> filterProjects(@NotBlank String keyPrefix, @NotNull List<Project> projects) {
        return filterListByTerm(keyPrefix, projects, Project::projectKey, String::startsWith);
    }

    public List<User> filterUsers(@NotBlank String corpUserPrefix, @NotNull List<User> projects) {
        return filterListByTerm(corpUserPrefix, projects, User::loginName, String::startsWith);
    }

    public Optional<User> findUser(@NotBlank String user, @NotNull List<User> users) {
        val foundUsers = filterListByTerm(user, users, User::loginName, String::equals);
        if (foundUsers.isEmpty()) {
            return Optional.empty();
        }
        if (foundUsers.size() > 1) {
            throw new IllegalArgumentException("Incorrect user: " + user);
        }
        return Optional.of(foundUsers.get(0));
    }

    private <M> List<M> filterListByTerm(String keyPrefix, List<M> items, Function<M, String> getter, BiPredicate<String, String> filterFunction) {
        return items.stream()
                .filter(obj -> Objects.nonNull(getter.apply(obj)))
                .filter(obj -> filterFunction.test(getter.apply(obj).toLowerCase(US), keyPrefix.toLowerCase(US)))
                .toList();
    }
}
