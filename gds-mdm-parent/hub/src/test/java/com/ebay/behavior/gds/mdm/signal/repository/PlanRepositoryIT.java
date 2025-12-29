package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.common.model.Model;
import com.ebay.behavior.gds.mdm.commonSvc.util.DbUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.Plan;
import com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus;
import com.ebay.behavior.gds.mdm.commonTestUtil.TestRequestContextUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.CJS_PLATFORM_ID;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.plan;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PlanRepositoryIT {

    private final Pageable pageable = DbUtils.getAuditablePageable(0, 100);

    @Autowired
    private PlanRepository repository;

    private Plan model;

    @BeforeEach
    void setUp() {
        model = plan();
    }

    @Test
    void save() {
        var saved = repository.save(model);

        var id = repository.getReferenceById(saved.getId()).getId();
        assertThat(id).isEqualTo(saved.getId());
        assertThat(saved.getRevision()).isEqualTo(0);
        assertThat(saved.getCreateDate()).isNotNull();
        assertThat(saved.getUpdateDate()).isNotNull();
    }

    @Test
    void deleteById() {
        repository.save(model);

        repository.deleteById(model.getId());

        var maybePersisted = repository.findById(model.getId());

        assertThat(maybePersisted).isEmpty();
    }

    @Test
    void findAllByNameOrDescriptionIgnoreCase() {
        var searchTerm = getRandomSmallString();
        var model1 = plan().setName(searchTerm);
        var model2 = plan().setDescription(searchTerm);
        var model3 = plan().setName("otherName").setDescription("otherDescription");
        var model4 = plan().setName(searchTerm);

        repository.saveAllAndFlush(List.of(model1, model2, model3, model4));
        model4.setStatus(PlanStatus.HIDDEN);
        repository.save(model4);

        var ids = repository.findAllByNameOrDescriptionIgnoreCase(searchTerm, pageable)
                .stream().map(Model::getId).toList();

        assertThat(ids).containsExactlyInAnyOrder(model1.getId(), model2.getId());
    }

    @Test
    void findAllByStatus() {
        var status = PlanStatus.CANCELED;
        var model1 = plan().setStatus(status);
        var model2 = plan().setStatus(status);
        var model3 = plan().setStatus(PlanStatus.CREATED);

        repository.saveAllAndFlush(List.of(model1, model2, model3));

        var ids = repository.findAllByStatus(status, pageable)
                .stream().map(Model::getId).toList();

        assertThat(ids).contains(model1.getId(), model2.getId());
        assertThat(ids).doesNotContain(model3.getId());
    }

    @Test
    void findAllExcludeHidden() {
        var model1 = plan();
        var model2 = plan();
        var model3 = plan();

        repository.saveAllAndFlush(List.of(model1, model2, model3));

        model2.setStatus(PlanStatus.CANCELED);
        model3.setStatus(PlanStatus.HIDDEN);
        repository.save(model2);
        repository.save(model3);

        var ids = repository.findAllExcludeHidden(pageable)
                .stream().map(Model::getId).toList();

        assertThat(ids).contains(model1.getId(), model2.getId());
    }

    @Test
    void findAllByJiraProjectContainingIgnoreCase() {
        var searchTerm = getRandomSmallString();
        var model1 = plan().setJiraProject(searchTerm.toUpperCase());
        var model2 = plan().setJiraProject(searchTerm.toLowerCase());
        var model3 = plan().setJiraProject("no match");
        var model4 = plan().setJiraProject(searchTerm);

        repository.saveAllAndFlush(List.of(model1, model2, model3, model4));

        model4.setStatus(PlanStatus.HIDDEN);
        repository.save(model4);

        var ids = repository.findAllByJiraProjectContainingIgnoreCase(searchTerm, pageable)
                .stream().map(Model::getId).toList();

        assertThat(ids).containsExactlyInAnyOrder(model1.getId(), model2.getId());
    }

    @Test
    void findAllByTeamDlsContainingIgnoreCase() {
        var searchTerm = getRandomSmallString();
        var model1 = plan().setTeamDls("Team1,Team2," + searchTerm.toUpperCase());
        var model2 = plan().setTeamDls(searchTerm.toLowerCase() + ",Team3, Team4");
        var model3 = plan().setTeamDls("Team5,Team6, Team7");
        var model4 = plan().setTeamDls(searchTerm);

        repository.saveAllAndFlush(List.of(model1, model2, model3, model4));
        model4.setStatus(PlanStatus.HIDDEN);
        repository.save(model4);

        var ids = repository.findAllByTeamDlsContainingIgnoreCase(searchTerm, pageable)
                .stream().map(Model::getId).toList();

        assertThat(ids).containsExactlyInAnyOrder(model1.getId(), model2.getId());
    }

    @Test
    void findAllByCreateByOrOwners_createBy() {
        var user = getRandomSmallString();
        TestRequestContextUtils.setUser(user);
        var model1 = plan();
        var model2 = plan();

        repository.saveAllAndFlush(List.of(model1, model2));

        model2.setStatus(PlanStatus.HIDDEN);
        repository.save(model2);

        var ids = repository.findAllByCreateByOrOwners(user, pageable)
                .stream().map(Model::getId).toList();

        assertThat(ids).containsExactly(model1.getId());
    }

    @Test
    void findAllByCreateByOrOwners_owners() {
        var user = getRandomSmallString();
        var model1 = plan().setOwners(user + ",otherOwner");
        var model2 = plan().setOwners(user);

        repository.saveAllAndFlush(List.of(model1, model2));

        model2.setStatus(PlanStatus.HIDDEN);
        repository.save(model2);

        var ids = repository.findAllByCreateByOrOwners(user, pageable)
                .stream().map(Model::getId).toList();

        assertThat(ids).containsExactly(model1.getId());
    }

    @Test
    void findAllByIdAndUser() {
        var user = getRandomSmallString();
        TestRequestContextUtils.setUser(user);
        var model1 = plan();

        model1 = repository.saveAndFlush(model1);

        var ids = repository.findAllByIdAndUser(model1.getId(), user, pageable)
                .stream().map(Model::getId).toList();
        assertThat(ids).containsExactly(model1.getId());
    }

    @Test
    void findAllByNameOrDescriptionAndUser() {
        var searchTerm = getRandomSmallString();
        var user = getRandomSmallString();
        var model1 = plan().setName(searchTerm).setOwners(user);
        var model2 = plan().setDescription(searchTerm).setOwners(user);
        var model3 = plan().setName(searchTerm).setOwners(user + ",otherOwner");
        var model4 = plan().setDescription("otherName").setOwners(user);
        var model5 = plan().setName(searchTerm).setOwners("otherUser");
        var model6 = plan().setDescription(searchTerm).setOwners(user);

        repository.saveAllAndFlush(List.of(model1, model2, model3, model4, model5, model6));

        model6.setStatus(PlanStatus.HIDDEN);
        repository.save(model6);

        var ids = repository.findAllByNameOrDescriptionAndUser(searchTerm, user, pageable)
                .stream().map(Model::getId).toList();

        assertThat(ids).containsExactlyInAnyOrder(model1.getId(), model2.getId(), model3.getId());
        assertThat(ids).doesNotContain(model4.getId(), model5.getId(), model6.getId());
    }

    @Test
    void findAllByStatusAndUser() {
        var status = PlanStatus.CANCELED;
        var user = getRandomSmallString();
        var model1 = plan().setStatus(status).setOwners(user);
        var model2 = plan().setStatus(status).setOwners(user + ",otherOwner");
        var model3 = plan().setStatus(PlanStatus.CREATED).setOwners(user);
        var model4 = plan().setStatus(status).setOwners("otherUser");

        repository.saveAllAndFlush(List.of(model1, model2, model3, model4));

        var ids = repository.findAllByStatusAndUser(status, user, pageable)
                .stream().map(Model::getId).toList();

        assertThat(ids).containsExactlyInAnyOrder(model1.getId(), model2.getId());
    }

    @Test
    void findAllByJiraProjectContainingIgnoreCaseAndUser() {
        var searchTerm = getRandomSmallString();
        var user = getRandomSmallString();
        var model1 = plan().setJiraProject(searchTerm).setOwners(user);
        var model2 = plan().setJiraProject(searchTerm).setOwners(user + ",otherOwner");
        var model3 = plan().setJiraProject("otherJira").setOwners(user);
        var model4 = plan().setJiraProject(searchTerm).setOwners("otherUser");
        var model5 = plan().setJiraProject(searchTerm).setOwners(user);

        repository.saveAllAndFlush(List.of(model1, model2, model3, model4, model5));
        model5.setStatus(PlanStatus.HIDDEN);
        repository.save(model5);

        var ids = repository.findAllByJiraProjectContainingIgnoreCaseAndUser(searchTerm, user, pageable)
                .stream().map(Model::getId).toList();

        assertThat(ids).containsExactlyInAnyOrder(model1.getId(), model2.getId());
    }

    @Test
    void findAllByTeamDlsContainingIgnoreCaseAndUser() {
        var searchTerm = getRandomSmallString();
        var user = getRandomSmallString();
        var model1 = plan().setTeamDls(searchTerm).setOwners(user);
        var model2 = plan().setTeamDls(searchTerm).setOwners(user + ",otherOwner");
        var model3 = plan().setTeamDls("otherTeamDls").setOwners(user);
        var model4 = plan().setTeamDls(searchTerm).setOwners("otherUser");
        var model5 = plan().setTeamDls(searchTerm).setOwners(user);

        repository.saveAllAndFlush(List.of(model1, model2, model3, model4, model5));

        model5.setStatus(PlanStatus.HIDDEN);
        repository.save(model5);

        var ids = repository.findAllByTeamDlsContainingIgnoreCaseAndUser(searchTerm, user, pageable)
                .stream().map(Model::getId).toList();

        assertThat(ids).containsExactlyInAnyOrder(model1.getId(), model2.getId());
    }

    @Test
    void findAllByCreateByOrOwnersDomainAndPlatform() {
        var user = getRandomSmallString();
        var domain = getRandomSmallString();

        var model1 = plan();
        model1.setDomain(domain).setPlatformId(CJS_PLATFORM_ID);
        model1.setOwners(user).setUpdateBy(user);

        var model2 = plan().setDomain(domain);
        model2.setPlatformId(CJS_PLATFORM_ID).setCreateBy(user);
        model2.setUpdateBy(user);

        // Save models to the repository
        repository.saveAllAndFlush(List.of(model1, model2));

        // Fetch IDs using the repository method
        var ids = repository.findAllByCreateByOrOwnersDomainAndPlatform(user, domain, CJS_PLATFORM_ID, pageable)
                .stream()
                .map(Model::getId)
                .toList();

        // Assert that the IDs match the expected models
        assertThat(ids).containsExactlyInAnyOrder(model1.getId(), model2.getId());
    }

    @Test
    void findAllByNameOrDescriptionAndUserAndDomain() {
        var searchTerm = getRandomSmallString();
        var user = getRandomSmallString();
        var domain = getRandomSmallString();
        var model1 = plan().setName(searchTerm).setOwners(user).setDomain(domain);
        var model2 = plan().setDescription(searchTerm).setOwners(user).setDomain(domain);

        repository.saveAllAndFlush(List.of(model1, model2));

        var ids = repository.findAllByNameOrDescriptionAndUserAndDomain(searchTerm, user, domain, pageable)
                .stream().map(Model::getId).toList();

        assertThat(ids).containsExactlyInAnyOrder(model1.getId(), model2.getId());
    }

    @Test
    void findAllByNameOrDescriptionAndUserAndPlatform() {
        var searchTerm = getRandomSmallString();
        var user = getRandomSmallString();
        var model1 = plan().setName(searchTerm).setOwners(user).setPlatformId(CJS_PLATFORM_ID);
        var model2 = plan().setDescription(searchTerm).setOwners(user).setPlatformId(CJS_PLATFORM_ID);

        repository.saveAllAndFlush(List.of(model1, model2));

        var ids = repository.findAllByNameOrDescriptionAndUserAndPlatform(searchTerm, user, CJS_PLATFORM_ID, pageable)
                .stream().map(Model::getId).toList();

        assertThat(ids).containsExactlyInAnyOrder(model1.getId(), model2.getId());
    }

    @Test
    void findAllByStatusAndUserAndDomain() {
        var status = PlanStatus.CREATED;
        var user = getRandomSmallString();
        var domain = getRandomSmallString();
        var model1 = plan().setStatus(status).setOwners(user).setDomain(domain);
        var model2 = plan().setStatus(status).setOwners(user).setDomain(domain);

        repository.saveAllAndFlush(List.of(model1, model2));

        var ids = repository.findAllByStatusAndUserAndDomain(status, user, domain, pageable)
                .stream().map(Model::getId).toList();

        assertThat(ids).containsExactlyInAnyOrder(model1.getId(), model2.getId());
    }

    @Test
    void findAllByJiraProjectContainingIgnoreCaseAndUserAndDomain() {
        var searchTerm = getRandomSmallString();
        var user = getRandomSmallString();
        var domain = getRandomSmallString();
        var model1 = plan().setJiraProject(searchTerm).setOwners(user).setDomain(domain);
        var model2 = plan().setJiraProject(searchTerm).setOwners(user).setDomain(domain);

        repository.saveAllAndFlush(List.of(model1, model2));

        var ids = repository.findAllByJiraProjectContainingIgnoreCaseAndUserAndDomain(searchTerm, user, domain, pageable)
                .stream().map(Model::getId).toList();

        assertThat(ids).containsExactlyInAnyOrder(model1.getId(), model2.getId());
    }

    @Test
    void findAllByTeamDlsContainingIgnoreCaseAndUserAndPlatform() {
        var searchTerm = getRandomSmallString();
        var user = getRandomSmallString();
        var model1 = plan().setTeamDls(searchTerm).setOwners(user).setPlatformId(CJS_PLATFORM_ID);
        var model2 = plan().setTeamDls(searchTerm).setOwners(user).setPlatformId(CJS_PLATFORM_ID);

        repository.saveAllAndFlush(List.of(model1, model2));

        var ids = repository.findAllByTeamDlsContainingIgnoreCaseAndUserAndPlatform(searchTerm, user, CJS_PLATFORM_ID, pageable)
                .stream().map(Model::getId).toList();

        assertThat(ids).containsExactlyInAnyOrder(model1.getId(), model2.getId());
    }

    @Test
    void findAllByTeamDlsContainingIgnoreCaseAndUserAndDomain() {
        var searchTerm = getRandomSmallString();
        var user = getRandomSmallString();
        var domain = getRandomSmallString();
        var model1 = plan().setTeamDls(searchTerm).setOwners(user).setDomain(domain);
        var model2 = plan().setTeamDls(searchTerm).setOwners(user).setDomain(domain);

        repository.saveAllAndFlush(List.of(model1, model2));

        var ids = repository.findAllByTeamDlsContainingIgnoreCaseAndUserAndDomain(searchTerm, user, domain, pageable)
                .stream().map(Model::getId).toList();

        assertThat(ids).containsExactlyInAnyOrder(model1.getId(), model2.getId());
    }

    @Test
    void findAllByTeamDlsContainingIgnoreCaseAndUserDomainAndPlatform() {
        var searchTerm = getRandomSmallString();
        var user = getRandomSmallString();
        var domain = getRandomSmallString();
        var model1 = plan().setTeamDls(searchTerm).setOwners(user).setDomain(domain).setPlatformId(CJS_PLATFORM_ID);
        var model2 = plan().setTeamDls(searchTerm).setOwners(user).setDomain(domain).setPlatformId(CJS_PLATFORM_ID);

        repository.saveAllAndFlush(List.of(model1, model2));

        var ids = repository.findAllByTeamDlsContainingIgnoreCaseAndUserDomainAndPlatform(searchTerm, user, domain, CJS_PLATFORM_ID, pageable)
                .stream().map(Model::getId).toList();

        assertThat(ids).containsExactlyInAnyOrder(model1.getId(), model2.getId());
    }

    @Test
    void findAllByJiraProjectContainingIgnoreCaseAndUserDomainAndPlatform() {
        var searchTerm = getRandomSmallString();
        var user = getRandomSmallString();
        var domain = getRandomSmallString();
        var plan1 = plan().setJiraProject(searchTerm).setOwners(user).setDomain(domain).setPlatformId(CJS_PLATFORM_ID);
        var plan2 = plan().setJiraProject(searchTerm).setOwners(user).setDomain(domain).setPlatformId(CJS_PLATFORM_ID);

        repository.saveAllAndFlush(List.of(plan1, plan2));

        var ids = repository.findAllByJiraProjectContainingIgnoreCaseAndUserDomainAndPlatform(searchTerm, user, domain, CJS_PLATFORM_ID, pageable)
                .stream().map(Model::getId).toList();

        assertThat(ids).containsExactlyInAnyOrder(plan1.getId(), plan2.getId());
    }
}
