package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.commonSvc.repository.SpecificationRepository;
import com.ebay.behavior.gds.mdm.signal.common.model.Plan;
import com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods", "PMD.UseObjectForClearerAPI"})
public interface PlanRepository extends SpecificationRepository<Plan, Long> {

    Page<Plan> findAllByStatus(PlanStatus status, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status = :status AND p.domain = :domain")
    Page<Plan> findAllByStatusAndDomain(PlanStatus status, String domain, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status = :status AND p.platformId = :platformId")
    Page<Plan> findAllByStatusAndPlatform(PlanStatus status, Long platformId, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status = :status "
            + "AND p.domain = :domain "
            + "AND p.platformId = :platformId")
    Page<Plan> findAllByStatusDomainAndPlatform(PlanStatus status, String domain, Long platformId, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status != 'HIDDEN'")
    Page<Plan> findAllExcludeHidden(Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status != 'HIDDEN' AND p.domain = :domain")
    Page<Plan> findAllExcludeHiddenAndDomain(String domain, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status != 'HIDDEN' AND p.platformId = :platformId")
    Page<Plan> findAllExcludeHiddenAndPlatform(Long platformId, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status != 'HIDDEN' "
            + "AND p.domain = :domain "
            + "AND p.platformId = :platformId")
    Page<Plan> findAllExcludeHiddenDomainAndPlatform(String domain, Long platformId, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE LOWER(p.jiraProject) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND p.status != 'HIDDEN'")
    Page<Plan> findAllByJiraProjectContainingIgnoreCase(String searchTerm, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE LOWER(p.jiraProject) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND p.status != 'HIDDEN' AND p.domain = :domain")
    Page<Plan> findAllByJiraProjectContainingIgnoreCaseAndDomain(String searchTerm, String domain, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE LOWER(p.jiraProject) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND p.status != 'HIDDEN' AND p.platformId = :platformId")
    Page<Plan> findAllByJiraProjectContainingIgnoreCaseAndPlatform(String searchTerm, Long platformId, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE LOWER(p.jiraProject) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
            + "AND p.status != 'HIDDEN' "
            + "And p.domain = :domain "
            + "AND p.platformId = :platformId")
    Page<Plan> findAllByJiraProjectContainingIgnoreCaseDomainAndPlatform(String searchTerm, String domain, Long platformId, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE LOWER(p.teamDls) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND p.status != 'HIDDEN'")
    Page<Plan> findAllByTeamDlsContainingIgnoreCase(String searchTerm, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE LOWER(p.teamDls) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND p.status != 'HIDDEN' AND p.domain = :domain")
    Page<Plan> findAllByTeamDlsContainingIgnoreCaseAndDomain(String searchTerm, String domain, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE LOWER(p.teamDls) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND p.status != 'HIDDEN' AND p.platformId = :platformId")
    Page<Plan> findAllByTeamDlsContainingIgnoreCaseAndPlatform(String searchTerm, Long platformId, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE LOWER(p.teamDls) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
            + "AND p.status != 'HIDDEN' "
            + "AND p.domain = :domain "
            + "AND p.platformId = :platformId")
    Page<Plan> findAllByTeamDlsContainingIgnoreCaseDomainAndPlatform(String searchTerm, String domain, Long platformId, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status != 'HIDDEN' "
            + "AND LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(p.description) "
            + "LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Plan> findAllByNameOrDescriptionIgnoreCase(String searchTerm, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status != 'HIDDEN' "
            + "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) "
            + "AND p.domain = :domain")
    Page<Plan> findAllByNameOrDescriptionIgnoreCaseAndDomain(String searchTerm, String domain, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status != 'HIDDEN' "
            + "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) "
            + "AND p.platformId = :platformId")
    Page<Plan> findAllByNameOrDescriptionIgnoreCaseAndPlatform(String searchTerm, Long platformId, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status != 'HIDDEN' "
            + "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) "
            + "AND p.domain = :domain "
            + "AND p.platformId = :platformId")
    Page<Plan> findAllByNameOrDescriptionIgnoreCaseDomainAndPlatform(String searchTerm, String domain, Long platformId, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status != 'HIDDEN' AND (p.createBy = :user OR p.owners LIKE %:user%)")
    Page<Plan> findAllByCreateByOrOwners(String user, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status != 'HIDDEN' AND (p.createBy = :user OR p.owners LIKE %:user%) AND p.domain = :domain")
    Page<Plan> findAllByCreateByOrOwnersAndDomain(String user, String domain, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status != 'HIDDEN' AND (p.createBy = :user OR p.owners LIKE %:user%) AND p.platformId = :platformId")
    Page<Plan> findAllByCreateByOrOwnersAndPlatform(String user, Long platformId, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status != 'HIDDEN' "
            + "AND (p.createBy = :user OR p.owners LIKE %:user%)"
            + "AND p.domain = :domain "
            + "AND p.platformId = :platformId")
    Page<Plan> findAllByCreateByOrOwnersDomainAndPlatform(String user, String domain, Long platformId, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.id = :searchTerm AND (p.createBy = :user OR p.owners LIKE %:user%)")
    Page<Plan> findAllByIdAndUser(Long searchTerm, String user, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status != 'HIDDEN' "
            + "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) "
            + "AND (p.createBy = :user OR p.owners LIKE %:user%)")
    Page<Plan> findAllByNameOrDescriptionAndUser(String searchTerm, String user, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status != 'HIDDEN' "
            + "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) "
            + "AND (p.createBy = :user OR p.owners LIKE %:user%) "
            + "AND p.domain = :domain")
    Page<Plan> findAllByNameOrDescriptionAndUserAndDomain(String searchTerm, String user, String domain, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status != 'HIDDEN' "
            + "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) "
            + "AND (p.createBy = :user OR p.owners LIKE %:user%) "
            + "AND p.platformId = :platformId")
    Page<Plan> findAllByNameOrDescriptionAndUserAndPlatform(String searchTerm, String user, Long platformId, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status != 'HIDDEN' "
            + "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) "
            + "AND (p.createBy = :user OR p.owners LIKE %:user%) "
            + "AND p.domain = :domain "
            + "AND p.platformId = :platformId")
    Page<Plan> findAllByNameOrDescriptionAndUserDomainAndPlatform(String searchTerm, String user, String domain, Long platformId, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status = :searchTerm AND (p.createBy = :user OR p.owners LIKE %:user%)")
    Page<Plan> findAllByStatusAndUser(PlanStatus searchTerm, String user, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status = :searchTerm AND (p.createBy = :user OR p.owners LIKE %:user%) "
            + "AND p.domain = :domain")
    Page<Plan> findAllByStatusAndUserAndDomain(PlanStatus searchTerm, String user, String domain, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status = :searchTerm AND (p.createBy = :user OR p.owners LIKE %:user%) "
            + "AND p.platformId = :platformId")
    Page<Plan> findAllByStatusAndUserAndPlatform(PlanStatus searchTerm, String user, Long platformId, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status = :searchTerm AND (p.createBy = :user OR p.owners LIKE %:user%) "
            + "AND p.domain = :domain "
            + "AND p.platformId = :platformId")
    Page<Plan> findAllByStatusAndUserDomainAndPlatform(PlanStatus searchTerm, String user, String domain, Long platformId, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status != 'HIDDEN' "
            + "AND LOWER(p.jiraProject) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
            + "AND (p.createBy = :user OR p.owners LIKE %:user%)")
    Page<Plan> findAllByJiraProjectContainingIgnoreCaseAndUser(String searchTerm, String user, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status != 'HIDDEN' "
            + "AND LOWER(p.jiraProject) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
            + "AND (p.createBy = :user OR p.owners LIKE %:user%) "
            + "AND p.domain = :domain")
    Page<Plan> findAllByJiraProjectContainingIgnoreCaseAndUserAndDomain(String searchTerm, String user, String domain, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status != 'HIDDEN' "
            + "AND LOWER(p.jiraProject) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
            + "AND (p.createBy = :user OR p.owners LIKE %:user%) "
            + "AND p.platformId = :platformId")
    Page<Plan> findAllByJiraProjectContainingIgnoreCaseAndUserAndPlatform(String searchTerm, String user, Long platformId, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status != 'HIDDEN' "
            + "AND LOWER(p.jiraProject) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
            + "AND (p.createBy = :user OR p.owners LIKE %:user%) "
            + "AND p.domain = :domain "
            + "AND p.platformId = :platformId")
    Page<Plan> findAllByJiraProjectContainingIgnoreCaseAndUserDomainAndPlatform(
            String searchTerm, String user, String domain, Long platformId, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status != 'HIDDEN' "
            + "AND LOWER(p.teamDls) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
            + "AND (p.createBy = :user OR p.owners LIKE %:user%)")
    Page<Plan> findAllByTeamDlsContainingIgnoreCaseAndUser(String searchTerm, String user, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status != 'HIDDEN' "
            + "AND LOWER(p.teamDls) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
            + "AND (p.createBy = :user OR p.owners LIKE %:user%) "
            + "AND p.domain = :domain")
    Page<Plan> findAllByTeamDlsContainingIgnoreCaseAndUserAndDomain(String searchTerm, String user, String domain, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status != 'HIDDEN' "
            + "AND LOWER(p.teamDls) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
            + "AND (p.createBy = :user OR p.owners LIKE %:user%) "
            + "AND p.platformId = :platformId")
    Page<Plan> findAllByTeamDlsContainingIgnoreCaseAndUserAndPlatform(String searchTerm, String user, Long platformId, Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.status != 'HIDDEN' "
            + "AND LOWER(p.teamDls) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
            + "AND (p.createBy = :user OR p.owners LIKE %:user%) "
            + "AND p.domain = :domain "
            + "AND p.platformId = :platformId")
    Page<Plan> findAllByTeamDlsContainingIgnoreCaseAndUserDomainAndPlatform(
            String searchTerm, String user, String domain, Long platformId, Pageable pageable);

    Page<Plan> findAllByName(String name, Pageable pageable);
}
