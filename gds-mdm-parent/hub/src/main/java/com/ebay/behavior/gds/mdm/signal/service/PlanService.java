package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.search.RelationalSearchRequest;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.common.model.search.SearchSpecification;
import com.ebay.behavior.gds.mdm.commonSvc.service.AuditService;
import com.ebay.behavior.gds.mdm.commonSvc.service.CrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.SearchService;
import com.ebay.behavior.gds.mdm.commonSvc.util.DbUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.Plan;
import com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus;
import com.ebay.behavior.gds.mdm.signal.common.model.PlanUserAction;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.model.audit.PlanHistory;
import com.ebay.behavior.gds.mdm.signal.common.model.search.PlanSearchBy;
import com.ebay.behavior.gds.mdm.signal.repository.PlanRepository;
import com.ebay.behavior.gds.mdm.signal.repository.UnstagedSignalRepository;
import com.ebay.behavior.gds.mdm.signal.repository.audit.PlanHistoryRepository;
import com.ebay.kernel.util.StringUtils;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.CREATED;
import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.DELETED;
import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.UPDATED;
import static com.ebay.behavior.gds.mdm.common.util.CommonValidationUtils.validateForCreate;
import static com.ebay.behavior.gds.mdm.common.util.CommonValidationUtils.validateForUpdate;
import static com.ebay.behavior.gds.mdm.signal.util.AuditUtils.deleteAndAudit;
import static com.ebay.behavior.gds.mdm.signal.util.AuditUtils.saveAndAudit;
import static com.ebay.behavior.gds.mdm.signal.util.AuditUtils.toHistoryRecord;

@Service
@Validated
@SuppressWarnings({"PMD.GodClass"})  //TODO: Refactor this class (using hibernate specification) to reduce its size and complexity
public class PlanService
        extends AbstractCrudAndAuditService<Plan, PlanHistory>
        implements CrudService<Plan>, SearchService<Plan>, AuditService<PlanHistory> {

    private static final String WRONG_PLAN_SEARCH_BY_MESSAGE = "Wrong PlanSearchBy enum value: %s";

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private PlanRepository repository;

    @Autowired
    private UnstagedSignalRepository signalRepository;

    @Autowired
    private DomainLookupService domainLookupService;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private PlanHistoryRepository historyRepository;

    @Getter(AccessLevel.PROTECTED)
    private final Class<Plan> modelType = Plan.class;

    @Getter(AccessLevel.PROTECTED)
    private final Class<PlanHistory> historyModelType = PlanHistory.class;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Plan create(@NotNull @Valid Plan plan) {
        validateForCreate(plan);
        if (!plan.getStatus().equals(PlanStatus.CREATED)) {
            throw new IllegalArgumentException(String.format("Invalid status %s while creating new plan.", plan.getStatus()));
        }

        domainLookupService.getByName(plan.getDomain()); // Ensure domain exists
        return saveAndAudit(plan, repository, historyRepository, CREATED, null, getHistoryModelType());
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Plan update(@NotNull @Valid Plan plan) {
        return update(plan, null);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Plan update(@NotNull @Valid Plan plan, PlanUserAction planAction) {
        validateForUpdate(plan);
        getById(plan.getId()); // Ensure signal exists before update
        domainLookupService.getByName(plan.getDomain()); // Ensure domain exists
        val actionName = Optional.ofNullable(planAction).map(PlanUserAction::name).orElse(null);
        return saveAndAudit(plan, getRepository(), getHistoryRepository(), UPDATED, actionName, getHistoryModelType());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteAll(@NotEmpty Set<@NotNull Long> ids) {
        val plans = repository.findAllById(ids);
        if (plans.isEmpty()) {
            throw new DataNotFoundException("No plans found with the provided ids.");
        }

        val historyRecords = plans.stream()
                .map(plan -> toHistoryRecord(plan, DELETED, null, PlanHistory.class))
                .toList();

        repository.deleteAllById(ids);
        historyRepository.saveAll(historyRecords);
    }

    @SuppressWarnings("PMD.PrematureDeclaration")
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(long id) {
        val plan = getById(id);
        val signals = getSignals(id);

        if (!signals.isEmpty()) {
            throw new IllegalArgumentException("Cannot delete plan with associated signals.");
        }

        deleteAndAudit(plan, repository, historyRepository, PlanHistory.class);
    }

    @Transactional(readOnly = true)
    public Set<UnstagedSignal> getSignals(long id) {
        getById(id); // Ensure the plan exists
        return Set.copyOf(signalRepository.findByPlanId(id));
    }

    @Override
    public Plan getByIdWithAssociations(long id) {
        throw new NotImplementedException("Plan doesn't require search with association functionality");
    }

    @Transactional(readOnly = true)
    public Page<Plan> search(@Valid @NotNull RelationalSearchRequest request) {
        return repository.findAll(
                SearchSpecification.getSpecification(request, false, Plan.class),
                SearchSpecification.getPageable(request)
        );
    }

    public Page<Plan> getAllByName(@NotBlank String name, @NotNull @Valid Pageable pageable) {
        return repository.findAllByName(name, pageable);
    }

    @Override
    public Page<Plan> getAll(@Valid @NotNull Search search) {
        throw new NotImplementedException("Use getAll(Optional<String> maybeUser, PlanSearchBy searchBy, String searchTerm, Pageable pageable) instead");
    }

    public Page<Plan> getAll(@NotNull Optional<String> maybeUser, PlanSearchBy searchBy, String searchTerm,
                             Optional<String> maybeDomain, Optional<Long> maybePlatformId, @NotNull @Valid Pageable pageable) {
        if (maybePlatformId.isPresent() && maybeDomain.isPresent()) {
            return getAllByDomainAndPlatform(maybeUser, searchBy, searchTerm, maybeDomain.get(), maybePlatformId.get(), pageable);
        }

        if (maybePlatformId.isPresent()) {
            return getAllByPlatform(maybeUser, searchBy, searchTerm, maybePlatformId.get(), pageable);
        }

        if (maybeDomain.isPresent()) {
            return getAllByDomain(maybeUser, searchBy, searchTerm, maybeDomain.get(), pageable);
        }

        if (Objects.isNull(searchBy) && Objects.isNull(searchTerm)) {
            return maybeUser.map(user -> repository.findAllByCreateByOrOwners(user, pageable))
                    .orElseGet(() -> repository.findAllExcludeHidden(pageable));
        }

        Validate.isTrue(Objects.nonNull(searchBy), String.format(WRONG_PLAN_SEARCH_BY_MESSAGE, searchBy));

        return switch (searchBy) {
            case PLAN -> getPlans(maybeUser, searchTerm, pageable);

            case JIRA_PROJECT -> maybeUser.map(user -> repository.findAllByJiraProjectContainingIgnoreCaseAndUser(searchTerm, user, pageable))
                    .orElseGet(() -> repository.findAllByJiraProjectContainingIgnoreCase(searchTerm, pageable));

            case TEAM_DL -> maybeUser.map(user -> repository.findAllByTeamDlsContainingIgnoreCaseAndUser(searchTerm, user, pageable))
                    .orElseGet(() -> repository.findAllByTeamDlsContainingIgnoreCase(searchTerm, pageable));

            case OWNER -> repository.findAllByCreateByOrOwners(searchTerm, pageable);

            case STATUS -> maybeUser.map(user -> repository.findAllByStatusAndUser(PlanStatus.valueOf(searchTerm), user, pageable))
                    .orElseGet(() -> repository.findAllByStatus(PlanStatus.valueOf(searchTerm), pageable));
        };
    }

    private Page<Plan> getAllByDomain(Optional<String> maybeUser, PlanSearchBy searchBy, String searchTerm, String domain, Pageable pageable) {
        if (Objects.isNull(searchBy) && Objects.isNull(searchTerm)) {
            return maybeUser.map(user -> repository.findAllByCreateByOrOwnersAndDomain(user, domain, pageable))
                    .orElseGet(() -> repository.findAllExcludeHiddenAndDomain(domain, pageable));
        }

        Validate.isTrue(Objects.nonNull(searchBy), String.format(WRONG_PLAN_SEARCH_BY_MESSAGE, searchBy));

        return switch (searchBy) {
            case PLAN -> getPlansByDomain(maybeUser, searchTerm, domain, pageable);

            case JIRA_PROJECT -> maybeUser.map(user -> repository.findAllByJiraProjectContainingIgnoreCaseAndUserAndDomain(searchTerm, user, domain, pageable))
                    .orElseGet(() -> repository.findAllByJiraProjectContainingIgnoreCaseAndDomain(searchTerm, domain, pageable));

            case TEAM_DL -> maybeUser.map(user -> repository.findAllByTeamDlsContainingIgnoreCaseAndUserAndDomain(searchTerm, user, domain, pageable))
                    .orElseGet(() -> repository.findAllByTeamDlsContainingIgnoreCaseAndDomain(searchTerm, domain, pageable));

            case OWNER -> repository.findAllByCreateByOrOwnersAndDomain(searchTerm, domain, pageable);

            case STATUS -> maybeUser.map(user -> repository.findAllByStatusAndUserAndDomain(PlanStatus.valueOf(searchTerm), user, domain, pageable))
                    .orElseGet(() -> repository.findAllByStatusAndDomain(PlanStatus.valueOf(searchTerm), domain, pageable));
        };
    }

    private Page<Plan> getAllByPlatform(Optional<String> maybeUser, PlanSearchBy searchBy, String searchTerm, Long platformId, Pageable pageable) {
        if (Objects.isNull(searchBy) && Objects.isNull(searchTerm)) {
            return maybeUser.map(user -> repository.findAllByCreateByOrOwnersAndPlatform(user, platformId, pageable))
                    .orElseGet(() -> repository.findAllExcludeHiddenAndPlatform(platformId, pageable));
        }

        Validate.isTrue(Objects.nonNull(searchBy), String.format(WRONG_PLAN_SEARCH_BY_MESSAGE, searchBy));

        return switch (searchBy) {
            case PLAN -> getPlansByPlatform(maybeUser, searchTerm, platformId, pageable);

            case JIRA_PROJECT ->
                    maybeUser.map(user -> repository.findAllByJiraProjectContainingIgnoreCaseAndUserAndPlatform(searchTerm, user, platformId, pageable))
                            .orElseGet(() -> repository.findAllByJiraProjectContainingIgnoreCaseAndPlatform(searchTerm, platformId, pageable));

            case TEAM_DL -> maybeUser.map(user -> repository.findAllByTeamDlsContainingIgnoreCaseAndUserAndPlatform(searchTerm, user, platformId, pageable))
                    .orElseGet(() -> repository.findAllByTeamDlsContainingIgnoreCaseAndPlatform(searchTerm, platformId, pageable));

            case OWNER -> repository.findAllByCreateByOrOwnersAndPlatform(searchTerm, platformId, pageable);

            case STATUS -> maybeUser.map(user -> repository.findAllByStatusAndUserAndPlatform(PlanStatus.valueOf(searchTerm), user, platformId, pageable))
                    .orElseGet(() -> repository.findAllByStatusAndPlatform(PlanStatus.valueOf(searchTerm), platformId, pageable));
        };
    }

    private Page<Plan> getAllByDomainAndPlatform(
            Optional<String> maybeUser, PlanSearchBy searchBy, String searchTerm, String domain, Long platformId, Pageable pageable) {
        if (Objects.isNull(searchBy) && Objects.isNull(searchTerm)) {
            return maybeUser.map(user -> repository.findAllByCreateByOrOwnersDomainAndPlatform(user, domain, platformId, pageable))
                    .orElseGet(() -> repository.findAllExcludeHiddenDomainAndPlatform(domain, platformId, pageable));
        }

        Validate.isTrue(Objects.nonNull(searchBy), String.format(WRONG_PLAN_SEARCH_BY_MESSAGE, searchBy));

        return switch (searchBy) {
            case PLAN -> getPlansByDomainAndPlatform(maybeUser, searchTerm, domain, platformId, pageable);

            case JIRA_PROJECT -> maybeUser.map(
                            user -> repository.findAllByJiraProjectContainingIgnoreCaseAndUserDomainAndPlatform(searchTerm, user, domain, platformId, pageable))
                    .orElseGet(() -> repository.findAllByJiraProjectContainingIgnoreCaseDomainAndPlatform(searchTerm, domain, platformId, pageable));

            case TEAM_DL ->
                    maybeUser.map(user -> repository.findAllByTeamDlsContainingIgnoreCaseAndUserDomainAndPlatform(searchTerm, user, domain,
                                    platformId, pageable))
                            .orElseGet(() -> repository.findAllByTeamDlsContainingIgnoreCaseDomainAndPlatform(searchTerm, domain,
                                    platformId, pageable));

            case OWNER -> repository.findAllByCreateByOrOwnersDomainAndPlatform(searchTerm, domain, platformId, pageable);

            case STATUS ->
                    maybeUser.map(user -> repository.findAllByStatusAndUserDomainAndPlatform(PlanStatus.valueOf(searchTerm),
                                    user, domain, platformId, pageable))
                            .orElseGet(() -> repository.findAllByStatusDomainAndPlatform(PlanStatus.valueOf(searchTerm),
                                    domain, platformId, pageable));
        };
    }

    private Page<Plan> getPlans(Optional<String> maybeUser, String searchTerm, Pageable pageable) {
        if (StringUtils.isNumeric(searchTerm)) {
            return getPlansByNumericSearchTerm(maybeUser, Long.parseLong(searchTerm), null, null);
        }
        return getPlansByStringSearchTerm(maybeUser, searchTerm, pageable);
    }

    private Page<Plan> getPlansByDomain(Optional<String> maybeUser, String searchTerm, String domain, Pageable pageable) {
        if (StringUtils.isNumeric(searchTerm)) {
            return getPlansByNumericSearchTerm(maybeUser, Long.parseLong(searchTerm), domain, null);
        }
        return getPlansByStringSearchTermAndDomain(maybeUser, searchTerm, domain, pageable);
    }

    private Page<Plan> getPlansByPlatform(Optional<String> maybeUser, String searchTerm, Long platformId, Pageable pageable) {
        if (StringUtils.isNumeric(searchTerm)) {
            return getPlansByNumericSearchTerm(maybeUser, Long.parseLong(searchTerm), null, platformId);
        }
        return getPlansByStringSearchTermAndPlatform(maybeUser, searchTerm, platformId, pageable);
    }

    private Page<Plan> getPlansByDomainAndPlatform(Optional<String> maybeUser, String searchTerm, String domain, Long platformId, Pageable pageable) {
        if (StringUtils.isNumeric(searchTerm)) {
            return getPlansByNumericSearchTerm(maybeUser, Long.parseLong(searchTerm), domain, platformId);
        }
        return getPlansByStringSearchTermDomainAndPlatform(maybeUser, searchTerm, domain, platformId, pageable);
    }

    private Page<Plan> getPlansByNumericSearchTerm(Optional<String> maybeUser, Long id, String domain, Long platformId) {
        val maybePlan = repository.findById(id);
        if (maybePlan.isEmpty()) {
            return Page.empty();
        }

        Plan plan = maybePlan.get();
        boolean isUserMatched = maybeUser.map(user -> plan.getCreateBy().equals(user)
                || plan.getUpdateBy().equals(user) || plan.getOwnersAsList().contains(user)).orElse(true);
        boolean isDomainMatched = Objects.isNull(domain) || domain.equals(plan.getDomain());
        boolean isPlatformMatched = Objects.isNull(platformId) || platformId.equals(plan.getPlatformId());
        if (isUserMatched && isDomainMatched && isPlatformMatched) {
            return DbUtils.getPage(plan);
        }

        return Page.empty();
    }

    private Page<Plan> getPlansByStringSearchTerm(Optional<String> maybeUser, String searchTerm, Pageable pageable) {
        return maybeUser.map(user -> repository.findAllByNameOrDescriptionAndUser(searchTerm, user, pageable))
                .orElseGet(() -> repository.findAllByNameOrDescriptionIgnoreCase(searchTerm, pageable));
    }

    private Page<Plan> getPlansByStringSearchTermAndDomain(Optional<String> maybeUser, String searchTerm, String domain, Pageable pageable) {
        return maybeUser.map(user -> repository.findAllByNameOrDescriptionAndUserAndDomain(searchTerm, user, domain, pageable))
                .orElseGet(() -> repository.findAllByNameOrDescriptionIgnoreCaseAndDomain(searchTerm, domain, pageable));
    }

    private Page<Plan> getPlansByStringSearchTermAndPlatform(Optional<String> maybeUser, String searchTerm, Long platformId, Pageable pageable) {
        return maybeUser.map(user -> repository.findAllByNameOrDescriptionAndUserAndPlatform(searchTerm, user, platformId, pageable))
                .orElseGet(() -> repository.findAllByNameOrDescriptionIgnoreCaseAndPlatform(searchTerm, platformId, pageable));
    }

    private Page<Plan> getPlansByStringSearchTermDomainAndPlatform(
            Optional<String> maybeUser, String searchTerm, String domain, Long platformId, Pageable pageable) {
        return maybeUser.map(user -> repository.findAllByNameOrDescriptionAndUserDomainAndPlatform(searchTerm, user, domain, platformId, pageable))
                .orElseGet(() -> repository.findAllByNameOrDescriptionIgnoreCaseDomainAndPlatform(searchTerm, domain, platformId, pageable));
    }
}
