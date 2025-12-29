package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.service.InfohubService;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Locale;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.SLOW_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Disabled // Disabled until InfohubService works in devZone
@Tag(INTEGRATION_TEST)
@Tag(SLOW_TEST)
@ActiveProfiles(IT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class InfohubServiceIT {

    private final String projectKey = "hellowrl";

    @MockitoSpyBean
    private InfohubService service;

    @Test
    @Order(1)
    void readAllProjects() {
        var projects = service.readAllProjects(); // cache miss
        service.readAllProjects(); // cache hit
        service.readAllProjects(); // cache hit

        assertThat(projects.size()).isGreaterThan(0);
        verify(service, times(1)).readAllProjects(); // verify cache hit
    }

    @Test
    @Order(2)
    @Tag(SLOW_TEST)
    void readAllUsers() {
        var users = service.readAllUsers(); // cache miss
        service.readAllUsers(); // cache hit
        service.readAllUsers(); // cache hit

        assertThat(users.size()).isGreaterThanOrEqualTo(1);
        assertThat(users).extracting("loginName").isNotNull();
        assertThat(users).extracting("firstName").isNotNull();
        verify(service, times(1)).readAllUsers(); // verify cache hit
    }

    @Test
    void filterProjects() {
        var projects = service.readAllProjects();
        var filtered = service.filterProjects("hello", projects);

        assertThat(filtered.size()).isGreaterThan(0);
        assertThat(filtered.get(0).projectKey()).contains(projectKey.toUpperCase(Locale.US));
    }

    @Test
    void filterProjects_notFound() {
        var projects = service.readAllProjects();
        var filtered = service.filterProjects("not_found", projects);

        assertThat(filtered).isEmpty();
    }

    @Test
    void getProjectByKey() {
        var projectName = "HelloWorld";

        var project = service.getProjectByKey(projectKey);

        assertThat(project.projectKey()).isEqualToIgnoringCase(projectKey);
        assertThat(project.name()).isEqualTo(projectName);
    }

    @Test
    void filterUsers() {
        var prefix = "test1234";
        var users = service.readAllUsers();
        var filtered = service.filterUsers(prefix, users);

        assertThat(filtered.size()).isEqualTo(1);
        assertThat(filtered.get(0).loginName()).isEqualToIgnoringCase(prefix);
        assertThat(filtered.get(0).firstName()).isEqualTo("Test");
    }

    @Test
    void filterUsers_notFound() {
        var users = service.readAllUsers();
        var filtered = service.filterUsers("notFound", users);

        assertThat(filtered).isEmpty();
    }
}
