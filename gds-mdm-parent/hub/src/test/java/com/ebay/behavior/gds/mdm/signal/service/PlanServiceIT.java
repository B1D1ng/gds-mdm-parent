package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.audit.AuditLogParams;
import com.ebay.behavior.gds.mdm.common.model.audit.ChangeType;
import com.ebay.behavior.gds.mdm.common.model.search.RelationalSearchRequest;
import com.ebay.behavior.gds.mdm.signal.common.model.Plan;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalDimValueLookup;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.model.search.PlanSearchBy;
import com.ebay.behavior.gds.mdm.signal.repository.audit.PlanHistoryRepository;
import com.ebay.behavior.gds.mdm.commonTestUtil.TestRequestContextUtils;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.Auditable.UPDATE_DATE;
import static com.ebay.behavior.gds.mdm.common.model.audit.AuditMode.BASIC;
import static com.ebay.behavior.gds.mdm.common.model.audit.AuditMode.FULL;
import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.DELETED;
import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.UPDATED;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.NOT_EQUAL_IGNORE_CASE;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus.CANCELED;
import static com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus.HIDDEN;
import static com.ebay.behavior.gds.mdm.signal.common.model.search.PlanSearchBy.JIRA_PROJECT;
import static com.ebay.behavior.gds.mdm.signal.common.model.search.PlanSearchBy.OWNER;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.CJS_PLATFORM_ID;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.EJS_PLATFORM_ID;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.plan;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.searchRequest;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedSignal;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.data.domain.Sort.Direction.ASC;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PlanServiceIT {

    private final Pageable pageable = PageRequest.of(0, 1000);

    @Autowired
    private PlanService service;

    @Autowired
    private UnstagedSignalService signalService;

    @Autowired
    private PlanHistoryRepository historyRepository;

    @Autowired
    private DomainLookupService domainService;

    private Plan plan;

    @BeforeEach
    void setUp() {
        plan = plan();
    }

    @Test
    void create() {
        var persisted = service.create(plan);

        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getName()).isEqualTo(plan.getName());

        var histories = historyRepository.findByOriginalId(persisted.getId());

        assertThat(histories.size()).isEqualTo(1);
        var history = histories.get(0);
        assertThat(history.getOriginalId()).isEqualTo(persisted.getId());
        assertThat(history.getOriginalRevision()).isEqualTo(persisted.getRevision());
        assertThat(history.getOriginalCreateDate()).isEqualTo(persisted.getCreateDate());
        assertThat(history.getOriginalUpdateDate()).isEqualTo(persisted.getUpdateDate());
        assertThat(history.getName()).isEqualTo(plan.getName());
        assertThat(history.getChangeType()).isEqualTo(ChangeType.CREATED);
    }

    @Test
    void create_badStatus_error() {
        plan = plan.setStatus(CANCELED);

        assertThatThrownBy(() -> service.create(plan))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid status");
    }

    @Test
    void create_invalid_error() {
        plan = plan.withId(123L);

        assertThatThrownBy(() -> service.create(plan))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be null");
    }

    @Test
    void create_domainNotExist_error() {
        plan.setDomain(getRandomSmallString());
        assertThatThrownBy(() -> service.create(plan))
                .isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("SignalDimValueLookup");
    }

    @Test
    void update_invalid_error() {
        assertThatThrownBy(() -> service.update(plan))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void update_domainNotExist_error() {
        var persisted = service.create(plan);
        persisted.setDomain(getRandomSmallString()).setDomain(getRandomSmallString());
        assertThatThrownBy(() -> service.update(plan))
                .isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("doesn't found");
    }

    @Test
    void update() {
        var persisted = service.create(plan);
        var updatedName = getRandomSmallString();
        persisted.setName(updatedName);

        var updatedPlan = service.update(persisted);

        assertThat(updatedPlan.getName()).isEqualTo(updatedName);

        var histories = historyRepository.findByOriginalId(persisted.getId());

        assertThat(histories.size()).isEqualTo(2);
        var history = histories.get(1);
        assertThat(history.getOriginalId()).isEqualTo(updatedPlan.getId());
        assertThat(history.getOriginalRevision()).isEqualTo(updatedPlan.getRevision());
        assertThat(history.getOriginalCreateDate()).isEqualTo(updatedPlan.getCreateDate());
        assertThat(history.getOriginalUpdateDate()).isEqualTo(updatedPlan.getUpdateDate());
        assertThat(history.getName()).isEqualTo(updatedName);
        assertThat(history.getChangeType()).isEqualTo(UPDATED);
    }

    @Test
    void delete() {
        var persistedPlan = service.create(plan);

        service.delete(persistedPlan.getId());

        var deleted = service.findById(persistedPlan.getId());
        assertThat(deleted).isEmpty();

        var historyPlans = historyRepository.findByOriginalId(persistedPlan.getId());
        assertThat(historyPlans.size()).isEqualTo(2);
        var historyPlan = historyPlans.get(1);
        assertThat(historyPlan.getOriginalId()).isEqualTo(persistedPlan.getId());
        assertThat(historyPlan.getOriginalRevision()).isEqualTo(persistedPlan.getRevision());
        assertThat(historyPlan.getOriginalCreateDate()).isEqualTo(persistedPlan.getCreateDate());
        assertThat(historyPlan.getOriginalUpdateDate()).isEqualTo(persistedPlan.getUpdateDate());
        assertThat(historyPlan.getName()).isEqualTo(persistedPlan.getName());
        assertThat(historyPlan.getChangeType()).isEqualTo(DELETED);
    }

    @Test
    void delete_withAssociatedPlans_error() {
        var planId = service.create(plan).getId();
        var signal = unstagedSignal(planId);
        signalService.create(signal);

        assertThatThrownBy(() -> service.delete(planId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot delete");
    }

    @Test
    void deleteAll() {
        var plan1 = plan();
        var plan2 = plan();
        var persisted1 = service.create(plan1);
        var persisted2 = service.create(plan2);

        service.deleteAll(Set.of(persisted1.getId(), persisted2.getId()));

        var deleted1 = service.findById(persisted1.getId());
        var deleted2 = service.findById(persisted2.getId());
        assertThat(deleted1).isEmpty();
        assertThat(deleted2).isEmpty();

        var historyPlans1 = historyRepository.findByOriginalId(persisted1.getId());
        var historyPlans2 = historyRepository.findByOriginalId(persisted2.getId());
        assertThat(historyPlans1.size()).isEqualTo(2);
        assertThat(historyPlans2.size()).isEqualTo(2);
        var historyPlan1 = historyPlans1.get(1);
        var historyPlan2 = historyPlans2.get(1);
        assertThat(historyPlan1.getOriginalId()).isEqualTo(persisted1.getId());
        assertThat(historyPlan1.getOriginalRevision()).isEqualTo(persisted1.getRevision());
        assertThat(historyPlan1.getOriginalCreateDate()).isEqualTo(persisted1.getCreateDate());
        assertThat(historyPlan1.getOriginalUpdateDate()).isEqualTo(persisted1.getUpdateDate());
        assertThat(historyPlan1.getName()).isEqualTo(persisted1.getName());
        assertThat(historyPlan1.getChangeType()).isEqualTo(DELETED);
        assertThat(historyPlan2.getOriginalId()).isEqualTo(persisted2.getId());
        assertThat(historyPlan2.getOriginalRevision()).isEqualTo(persisted2.getRevision());
        assertThat(historyPlan2.getOriginalCreateDate()).isEqualTo(persisted2.getCreateDate());
        assertThat(historyPlan2.getOriginalUpdateDate()).isEqualTo(persisted2.getUpdateDate());
        assertThat(historyPlan2.getName()).isEqualTo(persisted2.getName());
        assertThat(historyPlan2.getChangeType()).isEqualTo(DELETED);
    }

    @Test
    void getSignals() {
        var planId = service.create(plan).getId();
        var signal1 = unstagedSignal(planId);
        var signal2 = unstagedSignal(planId);
        signalService.createAll(Set.of(signal1, signal2));

        var signals = service.getSignals(planId);

        assertThat(signals).hasSize(2);
        assertThat(signals).extracting(UnstagedSignal::getPlanId).containsOnly(planId);
    }

    @Test
    @Transactional
    void getById() {
        var created = service.create(plan);

        var persisted = service.getById(created.getId());

        assertThat(persisted).isEqualTo(created);
    }

    @Test
    void getById_nonExistentId_error() {
        assertThatThrownBy(() -> service.getById(9999L))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    @Transactional
    void getAllByName() {
        var name = getRandomSmallString();
        var plan1 = plan().setName(name);
        var plan2 = plan().setName(name);
        plan1 = service.create(plan1);
        plan2 = service.create(plan2);

        var page = service.getAllByName(name, pageable);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).contains(plan1, plan2);
    }

    @Test
    @Transactional
    void getAll_nullSearchByNoUser() {
        var plan1 = plan();
        var plan2 = plan();
        plan1 = service.create(plan1);
        plan2 = service.create(plan2);

        plan2.setStatus(HIDDEN);
        service.update(plan2);

        var page = service.getAll(Optional.empty(), null, null, Optional.empty(), Optional.empty(), pageable);

        assertThat(page.getContent()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(page.getContent()).contains(plan1);
    }

    @Test
    @Transactional
    void getAll_nullSearchByNoUserAndDomain() {
        var domainName1 = getRandomSmallString();
        domainService.createIfAbsent(SignalDimValueLookup.builder().name(domainName1).readableName(domainName1).dimensionTypeId(0L).build());

        var plan1 = plan().setDomain(domainName1);
        var plan2 = plan();
        plan1 = service.create(plan1);
        plan2 = service.create(plan2);

        plan2.setStatus(HIDDEN);
        service.update(plan2);

        var page = service.getAll(Optional.empty(), null, null, Optional.of(plan1.getDomain()), Optional.empty(), pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent()).contains(plan1);
    }

    @Test
    @Transactional
    void getAll_nullSearchByNoUserAndPlatform() {
        var plan1 = plan().setPlatformId(EJS_PLATFORM_ID);
        var plan2 = plan().setPlatformId(EJS_PLATFORM_ID);
        plan1 = service.create(plan1);
        plan2 = service.create(plan2);

        plan2.setStatus(HIDDEN);
        service.update(plan2);

        var page = service.getAll(Optional.empty(), null, null, Optional.empty(), Optional.of(plan1.getPlatformId()), pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent()).contains(plan1);
    }

    @Test
    @Transactional
    void getAll_nullSearchByNoUserDomainAndPlatform() {
        var domainName1 = getRandomSmallString();
        domainService.createIfAbsent(SignalDimValueLookup.builder().name(domainName1).readableName(domainName1).dimensionTypeId(0L).build());
        var plan1 = plan().setPlatformId(CJS_PLATFORM_ID).setDomain(domainName1);
        var plan2 = plan().setPlatformId(CJS_PLATFORM_ID);
        plan1 = service.create(plan1);
        plan2 = service.create(plan2);

        plan2.setStatus(HIDDEN);
        service.update(plan2);

        var page = service.getAll(Optional.empty(), null, null, Optional.of(plan1.getDomain()), Optional.of(plan1.getPlatformId()), pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent()).contains(plan1);
    }

    @Test
    @Transactional
    void getAll_nullSearchByWithUser() {
        var user = getRandomSmallString();
        TestRequestContextUtils.setUser(user);
        var plan1 = plan();
        var plan2 = plan();
        plan1 = service.create(plan1);
        plan2 = service.create(plan2);

        plan2.setStatus(HIDDEN);
        service.update(plan2);

        var page = service.getAll(Optional.of(user), null, null, Optional.empty(), Optional.empty(), pageable);

        assertThat(page.getContent()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(page.getContent()).contains(plan1);
    }

    @Test
    @Transactional
    void getAll_nullSearchByWithUserAndDomain() {
        var user = getRandomSmallString();
        TestRequestContextUtils.setUser(user);
        var plan1 = plan();
        var plan2 = plan();
        plan1 = service.create(plan1);
        plan2 = service.create(plan2);

        plan2.setStatus(HIDDEN);
        service.update(plan2);

        var page = service.getAll(Optional.of(user), null, null, Optional.of(plan1.getDomain()), Optional.empty(), pageable);

        assertThat(page.getContent()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(page.getContent()).contains(plan1);
    }

    @Test
    @Transactional
    void getAll_nullSearchByWithUserAndPlatform() {
        var user = getRandomSmallString();
        TestRequestContextUtils.setUser(user);
        var plan1 = plan();
        var plan2 = plan();
        plan1 = service.create(plan1);
        plan2 = service.create(plan2);

        plan2.setStatus(HIDDEN);
        service.update(plan2);

        var page = service.getAll(Optional.of(user), null, null, Optional.empty(), Optional.of(plan1.getPlatformId()), pageable);

        assertThat(page.getContent()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(page.getContent()).contains(plan1);
    }

    @Test
    @Transactional
    void getAll_nullSearchByWithUserDomainAndPlatform() {
        var user = getRandomSmallString();
        TestRequestContextUtils.setUser(user);
        var plan1 = plan();
        var plan2 = plan();
        plan1 = service.create(plan1);
        plan2 = service.create(plan2);

        plan2.setStatus(HIDDEN);
        service.update(plan2);

        var page = service.getAll(Optional.of(user), null, null, Optional.of(plan1.getDomain()), Optional.of(plan1.getPlatformId()), pageable);

        assertThat(page.getContent()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(page.getContent()).contains(plan1);
    }

    @Test
    void getAll_jiraProjectNoUser() {
        var jiraProject = getRandomSmallString();
        var plan1 = plan().setJiraProject(jiraProject);
        var plan2 = plan().setJiraProject(jiraProject);
        service.create(plan1);
        service.create(plan2);

        var page = service.getAll(Optional.empty(), JIRA_PROJECT, jiraProject, Optional.empty(), Optional.empty(), pageable);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getJiraProject()).isEqualTo(jiraProject);
    }

    @Test
    void getAll_jiraProjectNoUserAndDomain() {
        var domainName2 = getRandomSmallString();
        domainService.createIfAbsent(SignalDimValueLookup.builder().name(domainName2).readableName(domainName2).dimensionTypeId(0L).build());
        var jiraProject = getRandomSmallString();
        var plan1 = plan().setJiraProject(jiraProject);
        var plan2 = plan().setJiraProject(jiraProject).setDomain(domainName2);
        service.create(plan1);
        service.create(plan2);

        var page = service.getAll(Optional.empty(), JIRA_PROJECT, jiraProject, Optional.of(plan2.getDomain()), Optional.empty(), pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getJiraProject()).isEqualTo(jiraProject);
        assertThat(page.getContent().get(0).getDomain()).isEqualTo(plan2.getDomain());
    }

    @Test
    @Transactional
    void getAll_jiraProjectNoUserAndPlatform() {
        var jiraProject = getRandomSmallString();
        var plan1 = plan().setJiraProject(jiraProject);
        var plan2 = plan().setJiraProject(jiraProject);
        plan1.setPlatformId(EJS_PLATFORM_ID);
        plan2.setPlatformId(CJS_PLATFORM_ID);
        service.create(plan1);
        service.create(plan2);

        var page = service.getAll(Optional.empty(), JIRA_PROJECT, jiraProject, Optional.empty(), Optional.of(plan2.getPlatformId()), pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getJiraProject()).isEqualTo(jiraProject);
        assertThat(page.getContent().get(0).getPlatformId()).isEqualTo(plan2.getPlatformId());
    }

    @Test
    void getAll_jiraProjectNoUserDomainAndPlatform() {
        var domainName2 = getRandomSmallString();
        domainService.createIfAbsent(SignalDimValueLookup.builder().name(domainName2).readableName(domainName2).dimensionTypeId(0L).build());
        var jiraProject = getRandomSmallString();
        var plan1 = plan().setJiraProject(jiraProject);
        var plan2 = plan().setJiraProject(jiraProject);
        plan2.setDomain(domainName2);
        plan2.setPlatformId(CJS_PLATFORM_ID);
        service.create(plan1);
        service.create(plan2);

        var page = service.getAll(Optional.empty(), JIRA_PROJECT, jiraProject, Optional.of(plan2.getDomain()), Optional.of(plan2.getPlatformId()), pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getJiraProject()).isEqualTo(jiraProject);
        assertThat(page.getContent().get(0).getDomain()).isEqualTo(plan2.getDomain());
    }

    @Test
    void getAll_jiraProjectWithUser() {
        var jiraProject = getRandomSmallString();
        var user = getRandomSmallString();
        TestRequestContextUtils.setUser(user);
        var plan1 = plan().setJiraProject(jiraProject);
        var plan2 = plan().setJiraProject(jiraProject);
        service.create(plan1);
        service.create(plan2);

        var page = service.getAll(Optional.of(user), JIRA_PROJECT, jiraProject, Optional.empty(), Optional.empty(), pageable);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).extracting(Plan::getJiraProject).containsOnly(jiraProject);
        assertThat(page.getContent()).extracting(Plan::getCreateBy).containsOnly(user);
    }

    @Test
    void getAll_jiraProjectWithUserAndDomain() {
        var jiraProject = getRandomSmallString();
        var user = getRandomSmallString();
        TestRequestContextUtils.setUser(user);
        var plan = plan().setJiraProject(jiraProject);
        service.create(plan);
        val domain = plan.getDomain();

        var page = service.getAll(Optional.of(user), JIRA_PROJECT, jiraProject, Optional.of(domain), Optional.empty(), pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getJiraProject()).isEqualTo(jiraProject);
        assertThat(page.getContent().get(0).getCreateBy()).isEqualTo(user);
        assertThat(page.getContent().get(0).getDomain()).isEqualTo(domain);
    }

    @Test
    @Transactional
    void getAll_jiraProjectWithUserAndPlatform() {
        var jiraProject = getRandomSmallString();
        var user = getRandomSmallString();
        TestRequestContextUtils.setUser(user);
        var plan = plan().setJiraProject(jiraProject);
        service.create(plan);
        val platformId = plan.getPlatformId();

        var page = service.getAll(Optional.of(user), JIRA_PROJECT, jiraProject, Optional.empty(), Optional.of(platformId), pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getJiraProject()).isEqualTo(jiraProject);
        assertThat(page.getContent().get(0).getCreateBy()).isEqualTo(user);
        assertThat(page.getContent().get(0).getPlatformId()).isEqualTo(platformId);
    }

    @Test
    @Transactional
    void getAll_jiraProjectWithUserDomainAndPlatform() {
        var jiraProject = getRandomSmallString();
        var user = getRandomSmallString();
        TestRequestContextUtils.setUser(user);
        var plan = plan().setJiraProject(jiraProject);
        service.create(plan);
        val platformId = plan.getPlatformId();
        val domain = plan.getDomain();

        var page = service.getAll(Optional.of(user), JIRA_PROJECT, jiraProject, Optional.of(domain), Optional.of(platformId), pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getJiraProject()).isEqualTo(jiraProject);
        assertThat(page.getContent().get(0).getCreateBy()).isEqualTo(user);
        assertThat(page.getContent().get(0).getPlatformId()).isEqualTo(platformId);
        assertThat(page.getContent().get(0).getDomain()).isEqualTo(domain);
    }

    @Test
    void getAll_teamsDlsNoUser() {
        var teamDls = getRandomSmallString();
        var plan1 = plan().setTeamDls(teamDls);
        var plan2 = plan().setTeamDls(teamDls);
        service.create(plan1);
        service.create(plan2);

        var page = service.getAll(Optional.empty(), PlanSearchBy.TEAM_DL, teamDls, Optional.empty(), Optional.empty(), pageable);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getTeamDls()).isEqualTo(teamDls);
    }

    @Test
    void getAll_teamsDlsNoUserAndDomain() {
        var teamDls = getRandomSmallString();
        var plan1 = plan().setTeamDls(teamDls);
        var plan2 = plan().setTeamDls(teamDls);
        service.create(plan1);
        service.create(plan2);
        plan2.setDomain(getRandomSmallString());
        val domain = plan1.getDomain();
        var page = service.getAll(Optional.empty(), PlanSearchBy.TEAM_DL, teamDls, Optional.of(domain), Optional.empty(), pageable);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getTeamDls()).isEqualTo(teamDls);
        assertThat(page.getContent().get(0).getDomain()).isEqualTo(domain);
    }

    @Test
    @Transactional
    void getAll_teamsDlsNoUserAndPlatform() {
        var teamDls = getRandomSmallString();
        var plan1 = plan().setTeamDls(teamDls);
        var plan2 = plan().setTeamDls(teamDls);
        service.create(plan1);
        service.create(plan2);
        plan2.setPlatformId(CJS_PLATFORM_ID);
        val platformId = plan1.getPlatformId();
        var page = service.getAll(Optional.empty(), PlanSearchBy.TEAM_DL, teamDls, Optional.empty(), Optional.of(platformId), pageable);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getTeamDls()).isEqualTo(teamDls);
        assertThat(page.getContent().get(0).getPlatformId()).isEqualTo(platformId);
    }

    @Test
    @Transactional
    void getAll_teamsDlsNoUserDomainAndPlatform() {
        var teamDls = getRandomSmallString();
        var plan1 = plan().setTeamDls(teamDls);
        var plan2 = plan().setTeamDls(teamDls);
        service.create(plan1);
        service.create(plan2);

        plan2.setPlatformId(CJS_PLATFORM_ID);
        val platformId = plan1.getPlatformId();
        val domain = plan1.getDomain();
        var page = service.getAll(Optional.empty(), PlanSearchBy.TEAM_DL, teamDls, Optional.of(domain), Optional.of(platformId), pageable);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getTeamDls()).isEqualTo(teamDls);
        assertThat(page.getContent().get(0).getPlatformId()).isEqualTo(platformId);
        assertThat(page.getContent().get(0).getDomain()).isEqualTo(domain);
    }

    @Test
    void getAll_teamsDlsWithUser() {
        var teamDls = getRandomSmallString();
        var user = getRandomSmallString();
        TestRequestContextUtils.setUser(user);
        var plan = plan().setTeamDls(teamDls);
        service.create(plan);

        var page = service.getAll(Optional.of(user), PlanSearchBy.TEAM_DL, teamDls, Optional.empty(), Optional.empty(), pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTeamDls()).isEqualTo(teamDls);
        assertThat(page.getContent().get(0).getCreateBy()).isEqualTo(user);
    }

    @Test
    void getAll_teamsDlsWithUserAndDomain() {
        var teamDls = getRandomSmallString();
        var user = getRandomSmallString();
        TestRequestContextUtils.setUser(user);
        var plan = plan().setTeamDls(teamDls);
        service.create(plan);

        val domain = plan.getDomain();
        var page = service.getAll(Optional.of(user), PlanSearchBy.TEAM_DL, teamDls, Optional.of(domain), Optional.empty(), pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTeamDls()).isEqualTo(teamDls);
        assertThat(page.getContent().get(0).getCreateBy()).isEqualTo(user);
        assertThat(page.getContent().get(0).getDomain()).isEqualTo(domain);
    }

    @Test
    @Transactional
    void getAll_teamsDlsWithUserAndPlatform() {
        var teamDls = getRandomSmallString();
        var user = getRandomSmallString();
        TestRequestContextUtils.setUser(user);
        var plan = plan().setTeamDls(teamDls);
        service.create(plan);

        val platformId = plan.getPlatformId();
        var page = service.getAll(Optional.of(user), PlanSearchBy.TEAM_DL, teamDls, Optional.empty(), Optional.of(platformId), pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTeamDls()).isEqualTo(teamDls);
        assertThat(page.getContent().get(0).getCreateBy()).isEqualTo(user);
        assertThat(page.getContent().get(0).getPlatformId()).isEqualTo(platformId);
    }

    @Test
    @Transactional
    void getAll_teamsDlsWithUserDomainAndPlatform() {
        var teamDls = getRandomSmallString();
        var user = getRandomSmallString();
        TestRequestContextUtils.setUser(user);
        var plan = plan().setTeamDls(teamDls);
        service.create(plan);

        val platformId = plan.getPlatformId();
        val domain = plan.getDomain();
        var page = service.getAll(Optional.of(user), PlanSearchBy.TEAM_DL, teamDls, Optional.of(domain), Optional.of(platformId), pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTeamDls()).isEqualTo(teamDls);
        assertThat(page.getContent().get(0).getCreateBy()).isEqualTo(user);
        assertThat(page.getContent().get(0).getPlatformId()).isEqualTo(platformId);
        assertThat(page.getContent().get(0).getDomain()).isEqualTo(domain);
    }

    @Test
    void getAll_owner() {
        var owner = getRandomSmallString();
        var plan = plan().setOwners(owner);
        service.create(plan);

        var page = service.getAll(Optional.empty(), OWNER, owner, Optional.empty(), Optional.empty(), pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getOwners()).isEqualTo(owner);
    }

    @Test
    void getAll_ownerAndDomain() {
        var domainName2 = getRandomSmallString();
        domainService.createIfAbsent(SignalDimValueLookup.builder().name(domainName2).readableName(domainName2).dimensionTypeId(0L).build());
        var owner = getRandomSmallString();
        var plan1 = plan().setOwners(getRandomSmallString());
        var plan2 = plan().setOwners(owner).setDomain(domainName2);
        service.create(plan1);
        service.create(plan2);

        var page = service.getAll(Optional.empty(), OWNER, owner, Optional.of(domainName2), Optional.empty(), pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getOwners()).isEqualTo(owner);
        assertThat(page.getContent().get(0).getDomain()).isEqualTo(domainName2);
    }

    @Test
    @Transactional
    void getAll_ownerAndPlatform() {
        var owner = getRandomSmallString();
        var plan1 = plan().setOwners(getRandomSmallString()).setPlatformId(CJS_PLATFORM_ID);
        var plan2 = plan().setOwners(owner).setPlatformId(CJS_PLATFORM_ID);
        service.create(plan1);
        service.create(plan2);

        var page = service.getAll(Optional.empty(), OWNER, owner, Optional.empty(), Optional.of(CJS_PLATFORM_ID), pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getOwners()).isEqualTo(owner);
        assertThat(page.getContent().get(0).getPlatformId()).isEqualTo(CJS_PLATFORM_ID);
    }

    @Test
    @Transactional
    void getAll_ownerDomainAndPlatform() {
        var domainName2 = getRandomSmallString();
        domainService.createIfAbsent(SignalDimValueLookup.builder().name(domainName2).readableName(domainName2).dimensionTypeId(0L).build());
        var owner = getRandomSmallString();
        var plan1 = plan().setOwners(getRandomSmallString()).setPlatformId(CJS_PLATFORM_ID);
        var plan2 = plan().setOwners(owner).setPlatformId(CJS_PLATFORM_ID).setDomain(domainName2);
        service.create(plan1);
        service.create(plan2);

        var page = service.getAll(Optional.empty(), OWNER, owner, Optional.of(domainName2), Optional.of(CJS_PLATFORM_ID), pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getOwners()).isEqualTo(owner);
        assertThat(page.getContent().get(0).getPlatformId()).isEqualTo(CJS_PLATFORM_ID);
        assertThat(page.getContent().get(0).getDomain()).isEqualTo(domainName2);
    }

    @Test
    void getAll_statusNoUser() {
        var plan1 = plan();
        var plan2 = plan();
        plan1 = service.create(plan1);
        plan2 = service.create(plan2);

        plan1.setStatus(CANCELED);
        plan2.setStatus(CANCELED);
        service.update(plan1);
        service.update(plan2);

        var page = service.getAll(Optional.empty(), PlanSearchBy.STATUS, CANCELED.name(), Optional.empty(), Optional.empty(), pageable);

        assertThat(page.getContent()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(page.getContent().get(0).getStatus()).isEqualTo(CANCELED);
    }

    @Test
    void getAll_statusNoUserAndDomain() {
        var domainName1 = getRandomSmallString();
        domainService.createIfAbsent(SignalDimValueLookup.builder().name(domainName1).readableName(domainName1).dimensionTypeId(0L).build());
        var plan1 = plan().setDomain(domainName1);
        var plan2 = plan();
        plan1 = service.create(plan1);
        plan2 = service.create(plan2);

        plan1.setStatus(CANCELED);
        plan2.setStatus(CANCELED);
        service.update(plan1);
        service.update(plan2);

        var page = service.getAll(Optional.empty(), PlanSearchBy.STATUS, CANCELED.name(), Optional.of(domainName1), Optional.empty(), pageable);

        assertThat(page.getContent()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(page.getContent().get(0).getStatus()).isEqualTo(CANCELED);
        assertThat(page.getContent().get(0).getDomain()).isEqualTo(domainName1);
    }

    @Test
    @Transactional
    void getAll_statusNoUserAndPlatform() {
        var plan1 = plan().setPlatformId(CJS_PLATFORM_ID);
        var plan2 = plan();
        plan1 = service.create(plan1);
        plan2 = service.create(plan2);

        plan1.setStatus(CANCELED);
        plan2.setStatus(CANCELED);
        service.update(plan1);
        service.update(plan2);

        var page = service.getAll(Optional.empty(), PlanSearchBy.STATUS, CANCELED.name(), Optional.empty(), Optional.of(CJS_PLATFORM_ID), pageable);

        assertThat(page.getContent()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(page.getContent().get(0).getStatus()).isEqualTo(CANCELED);
        assertThat(page.getContent().get(0).getPlatformId()).isEqualTo(CJS_PLATFORM_ID);
    }

    @Test
    @Transactional
    void getAll_statusNoUserDomainAndPlatform() {
        var domainName1 = getRandomSmallString();
        domainService.createIfAbsent(SignalDimValueLookup.builder().name(domainName1).readableName(domainName1).dimensionTypeId(0L).build());
        var plan1 = plan().setPlatformId(CJS_PLATFORM_ID).setDomain(domainName1);
        var plan2 = plan();
        plan1 = service.create(plan1);
        plan2 = service.create(plan2);

        plan1.setStatus(CANCELED);
        plan2.setStatus(CANCELED);
        service.update(plan1);
        service.update(plan2);

        var page = service.getAll(Optional.empty(), PlanSearchBy.STATUS, CANCELED.name(), Optional.of(domainName1), Optional.of(CJS_PLATFORM_ID), pageable);

        assertThat(page.getContent()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(page.getContent().get(0).getStatus()).isEqualTo(CANCELED);
        assertThat(page.getContent().get(0).getPlatformId()).isEqualTo(CJS_PLATFORM_ID);
        assertThat(page.getContent().get(0).getDomain()).isEqualTo(domainName1);
    }

    @Test
    void getAll_statusWithUser() {
        var user = getRandomSmallString();
        TestRequestContextUtils.setUser(user);
        var plan1 = plan();
        var plan2 = plan();
        plan1 = service.create(plan1);
        plan2 = service.create(plan2);

        plan1.setStatus(CANCELED);
        plan2.setStatus(CANCELED);
        service.update(plan1);
        service.update(plan2);

        var page = service.getAll(Optional.of(user), PlanSearchBy.STATUS, CANCELED.name(), Optional.empty(), Optional.empty(), pageable);

        assertThat(page.getContent()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(page.getContent()).extracting(Plan::getStatus).containsOnly(CANCELED);
        assertThat(page.getContent()).extracting(Plan::getCreateBy).containsOnly(user);
        assertThat(page.getContent()).extracting(Plan::getUpdateBy).containsOnly(user);
    }

    @Test
    void getAll_statusWithUserAndDomain() {
        var domainName1 = getRandomSmallString();
        domainService.createIfAbsent(SignalDimValueLookup.builder().name(domainName1).readableName(domainName1).dimensionTypeId(0L).build());
        var user = getRandomSmallString();
        TestRequestContextUtils.setUser(user);

        var plan1 = plan().setDomain(domainName1);
        var plan2 = plan();
        plan1 = service.create(plan1);
        plan2 = service.create(plan2);

        plan1.setStatus(CANCELED);
        plan2.setStatus(CANCELED);
        service.update(plan1);
        service.update(plan2);

        var page = service.getAll(Optional.of(user), PlanSearchBy.STATUS, CANCELED.name(), Optional.of(domainName1), Optional.empty(), pageable);

        assertThat(page.getContent()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(page.getContent().get(0).getStatus()).isEqualTo(CANCELED);
        assertThat(page.getContent().get(0).getCreateBy()).isEqualTo(user);
        assertThat(page.getContent().get(0).getDomain()).isEqualTo(domainName1);
    }

    @Test
    @Transactional
    void getAll_statusWithUserAndPlatform() {
        var user = getRandomSmallString();
        TestRequestContextUtils.setUser(user);

        var plan1 = plan().setPlatformId(CJS_PLATFORM_ID);
        var plan2 = plan();
        plan1 = service.create(plan1);
        plan2 = service.create(plan2);

        plan1.setStatus(CANCELED);
        plan2.setStatus(CANCELED);
        service.update(plan1);
        service.update(plan2);

        var page = service.getAll(Optional.of(user), PlanSearchBy.STATUS, CANCELED.name(), Optional.empty(), Optional.of(CJS_PLATFORM_ID), pageable);

        assertThat(page.getContent()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(page.getContent().get(0).getStatus()).isEqualTo(CANCELED);
        assertThat(page.getContent().get(0).getCreateBy()).isEqualTo(user);
        assertThat(page.getContent().get(0).getPlatformId()).isEqualTo(CJS_PLATFORM_ID);
    }

    @Test
    @Transactional
    void getAll_statusWithUserDomainAndPlatform() {
        var domainName1 = getRandomSmallString();
        domainService.createIfAbsent(SignalDimValueLookup.builder().name(domainName1).readableName(domainName1).dimensionTypeId(0L).build());
        var user = getRandomSmallString();
        TestRequestContextUtils.setUser(user);

        var plan1 = plan().setPlatformId(CJS_PLATFORM_ID).setDomain(domainName1);
        var plan2 = plan();
        plan1 = service.create(plan1);
        plan2 = service.create(plan2);

        plan1.setStatus(CANCELED);
        plan2.setStatus(CANCELED);
        service.update(plan1);
        service.update(plan2);

        var page = service.getAll(Optional.of(user), PlanSearchBy.STATUS, CANCELED.name(), Optional.of(domainName1), Optional.of(CJS_PLATFORM_ID), pageable);

        assertThat(page.getContent()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(page.getContent().get(0).getStatus()).isEqualTo(CANCELED);
        assertThat(page.getContent().get(0).getCreateBy()).isEqualTo(user);
        assertThat(page.getContent().get(0).getPlatformId()).isEqualTo(CJS_PLATFORM_ID);
        assertThat(page.getContent().get(0).getDomain()).isEqualTo(domainName1);
    }

    @Test
    void getAll_withNumericSearchTermNoUser() {
        plan = service.create(plan);
        var planId = plan.getId();

        var page = service.getAll(Optional.empty(), PlanSearchBy.PLAN, String.valueOf(planId), Optional.empty(), Optional.empty(), pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(planId);
    }

    @Test
    void getAll_withNumericSearchTermNoUserAndDomain() {
        var domainName = getRandomSmallString();
        domainService.createIfAbsent(SignalDimValueLookup.builder().name(domainName).readableName(domainName).dimensionTypeId(0L).build());
        plan = service.create(plan.setDomain(domainName));

        var planId = plan.getId();

        var page = service.getAll(Optional.empty(), PlanSearchBy.PLAN, String.valueOf(planId), Optional.of(domainName), Optional.empty(), pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(planId);
    }

    @Test
    void getAll_withNumericSearchTermNoUserAndDifferentDomain() {
        val domain = getRandomSmallString();
        plan = service.create(plan);

        var planId = plan.getId();

        var page = service.getAll(Optional.empty(), PlanSearchBy.PLAN, String.valueOf(planId), Optional.of(domain), Optional.empty(), pageable);

        assertThat(page.getContent()).hasSize(0);
    }

    @Test
    void getAll_withNumericSearchTermWithUser() {
        plan = service.create(plan);
        var planId = plan.getId();

        var page = service.getAll(Optional.of("user"), PlanSearchBy.PLAN, String.valueOf(planId), Optional.empty(), Optional.empty(), pageable);

        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void getAll_withStringSearchTermNoUser() {
        plan = service.create(plan);
        var planName = plan.getName();

        var page = service.getAll(Optional.empty(), PlanSearchBy.PLAN, planName, Optional.empty(), Optional.empty(), pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getName()).isEqualTo(planName);
    }

    @Test
    void getAll_withStringSearchTermNoUserAndDomain() {
        plan = service.create(plan);
        var planName = plan.getName();

        var page = service.getAll(Optional.empty(), PlanSearchBy.PLAN, planName, Optional.of(plan.getDomain()), Optional.empty(), pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getName()).isEqualTo(planName);
        assertThat(page.getContent().get(0).getDomain()).isEqualTo(plan.getDomain());
    }

    @Test
    @Transactional
    void getAll_withStringSearchTermNoUserAndPlatform() {
        plan = service.create(plan);
        var planName = plan.getName();

        var page = service.getAll(Optional.empty(), PlanSearchBy.PLAN, planName, Optional.empty(), Optional.of(plan.getPlatformId()), pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getName()).isEqualTo(planName);
        assertThat(page.getContent().get(0).getPlatform()).isEqualTo(plan.getPlatform());
    }

    @Test
    @Transactional
    void getAll_withStringSearchTermNoUserDomainAndPlatform() {
        plan = service.create(plan);
        var planName = plan.getName();

        var page = service.getAll(Optional.empty(), PlanSearchBy.PLAN, planName, Optional.of(plan.getDomain()), Optional.of(plan.getPlatformId()), pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getName()).isEqualTo(planName);
        assertThat(page.getContent().get(0).getPlatform()).isEqualTo(plan.getPlatform());
        assertThat(page.getContent().get(0).getDomain()).isEqualTo(plan.getDomain());
    }

    @Test
    void getAll_withDescriptionAndUser() {
        var user = getRandomSmallString();
        TestRequestContextUtils.setUser(user);
        var prefix = getRandomSmallString();
        var desc = prefix + getRandomSmallString();
        var plan1 = plan().setDescription(desc);
        var plan2 = plan().setDescription(desc);
        service.create(plan1);
        service.create(plan2);

        var page = service.getAll(Optional.of(user), PlanSearchBy.PLAN, prefix, Optional.empty(), Optional.empty(), pageable);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).extracting(Plan::getDescription).containsOnly(desc);
    }

    @Test
    void getAll_withDescriptionAndUserAndDomain() {
        var user = getRandomSmallString();
        TestRequestContextUtils.setUser(user);
        var prefix = getRandomSmallString();
        var desc = prefix + getRandomSmallString();
        var plan = plan().setDescription(desc);
        val domain = plan.getDomain();
        service.create(plan);

        var page = service.getAll(Optional.of(user), PlanSearchBy.PLAN, prefix, Optional.of(domain), Optional.empty(), pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getDescription()).isEqualTo(desc);
        assertThat(page.getContent().get(0).getDomain()).isEqualTo(domain);
    }

    @Test
    @Transactional
    void getAll_withDescriptionAndUserAndPlatform() {
        var user = getRandomSmallString();
        TestRequestContextUtils.setUser(user);
        var prefix = getRandomSmallString();
        var desc = prefix + getRandomSmallString();
        var plan = plan().setDescription(desc);
        val platformId = plan.getPlatformId();
        service.create(plan);

        var page = service.getAll(Optional.of(user), PlanSearchBy.PLAN, prefix, Optional.empty(), Optional.of(platformId), pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getDescription()).isEqualTo(desc);
        assertThat(page.getContent().get(0).getPlatformId()).isEqualTo(platformId);
    }

    @Test
    @Transactional
    void getAll_withDescriptionAndUserDomainAndPlatform() {
        var user = getRandomSmallString();
        TestRequestContextUtils.setUser(user);
        var prefix = getRandomSmallString();
        var desc = prefix + getRandomSmallString();
        var plan = plan().setDescription(desc);
        val platformId = plan.getPlatformId();
        val domain = plan.getDomain();
        service.create(plan);

        var page = service.getAll(Optional.of(user), PlanSearchBy.PLAN, prefix, Optional.of(domain), Optional.of(platformId), pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getDescription()).isEqualTo(desc);
        assertThat(page.getContent().get(0).getPlatformId()).isEqualTo(platformId);
        assertThat(page.getContent().get(0).getDomain()).isEqualTo(domain);
    }

    @Test
    void getAll_excludeHidden() {
        var notHidden = service.create(plan);
        var hidden = plan();
        hidden = service.create(hidden);

        hidden.setStatus(HIDDEN);
        service.update(hidden);

        var page = service.getAll(Optional.empty(), null, null, Optional.empty(), Optional.empty(), pageable);

        assertThat(page.getContent().size()).isGreaterThanOrEqualTo(1);
        assertThat(page.getContent()).extracting(Plan::getStatus).contains(notHidden.getStatus()).doesNotContain(HIDDEN);
    }

    @Test
    void search_excludeHidden() {
        var notHidden = service.create(plan);
        var hidden = plan();
        hidden = service.create(hidden);

        hidden.setStatus(HIDDEN);
        service.update(hidden);

        var filter = new RelationalSearchRequest.Filter("status", NOT_EQUAL_IGNORE_CASE, HIDDEN.getValue());
        var request = searchRequest(UPDATE_DATE, ASC, filter);

        var page = service.search(request);

        assertThat(page.getContent().size()).isGreaterThanOrEqualTo(1);
        assertThat(page.getContent()).extracting(Plan::getStatus).contains(notHidden.getStatus()).doesNotContain(HIDDEN);
    }

    @Test
    void getAuditLog_basic() {
        var persisted = service.create(plan);
        var updatedName = getRandomSmallString();
        persisted.setName(updatedName);
        var updatedPlan = service.update(persisted);

        var auditParams = AuditLogParams.ofNonVersioned(persisted.getId(), BASIC);
        var auditRecords = service.getAuditLog(auditParams);

        assertThat(auditRecords).hasSize(2);
        var record1 = auditRecords.get(0);
        assertThat(record1.getUpdateBy()).isEqualTo(persisted.getCreateBy());
        assertThat(record1.getRight().getOriginalUpdateDate()).isEqualTo(persisted.getUpdateDate());
        assertThat(record1.getRevision()).isEqualTo(persisted.getRevision());
        assertThat(record1.getChangeType()).isEqualTo(ChangeType.CREATED);
        var record2 = auditRecords.get(1);
        assertThat(record2.getUpdateBy()).isEqualTo(updatedPlan.getUpdateBy());
        assertThat(record2.getRight().getOriginalUpdateDate()).isEqualTo(updatedPlan.getUpdateDate());
        assertThat(record2.getRevision()).isEqualTo(updatedPlan.getRevision());
        assertThat(record2.getChangeType()).isEqualTo(UPDATED);
    }

    @Test
    void getAuditLog_full() {
        var persisted = service.create(plan);
        var originalName = persisted.getName();
        var originalDesc = persisted.getDescription();
        persisted.setName(getRandomSmallString()).setDescription(getRandomSmallString());
        var updatedPlan = service.update(persisted);

        var auditParams = AuditLogParams.ofNonVersioned(persisted.getId(), FULL);
        var auditRecords = service.getAuditLog(auditParams);

        assertThat(auditRecords).hasSize(2);
        var record = auditRecords.get(1);
        assertThat(record.getUpdateBy()).isEqualTo(updatedPlan.getUpdateBy());
        assertThat(record.getRight().getOriginalUpdateDate()).isEqualTo(updatedPlan.getUpdateDate());
        assertThat(record.getRevision()).isEqualTo(updatedPlan.getRevision());
        assertThat(record.getChangeType()).isEqualTo(UPDATED);
        var diff = record.getDiff();
        assertThat(diff.getChanges()).hasSize(2);
        assertThat(diff.getPropertyChanges(NAME).get(0).getLeft()).isEqualTo(originalName);
        assertThat(diff.getPropertyChanges(NAME).get(0).getRight()).isEqualTo(updatedPlan.getName());
        assertThat(diff.getPropertyChanges("description").get(0).getLeft()).isEqualTo(originalDesc);
        assertThat(diff.getPropertyChanges("description").get(0).getRight()).isEqualTo(updatedPlan.getDescription());
    }

    @Test
    void getAuditLog_full_empty() {
        var persisted = service.create(plan);

        var auditParams = AuditLogParams.ofNonVersioned(persisted.getId(), FULL);
        var auditLog = service.getAuditLog(auditParams);
        assertThat(auditLog).hasSize(1);
        assertThat(auditLog.get(0).getRight().getChangeType()).isEqualTo(ChangeType.CREATED);
    }

    @Test
    void getAuditLog_nonExistentId_error() {
        var auditParams = AuditLogParams.ofNonVersioned(getRandomLong(), BASIC);
        assertThatThrownBy(() -> service.getAuditLog(auditParams))
                .isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("doesn't found");
    }
}
