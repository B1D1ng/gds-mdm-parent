package com.ebay.behavior.gds.mdm.contract.repository;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.contract.model.UnstagedContract;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;

public interface UnstagedContractRepository extends JpaRepository<UnstagedContract, VersionedId> {
    @Query("SELECT c FROM UnstagedContract c WHERE c.createBy = :user OR c.owners LIKE %:user%")
    Page<UnstagedContract> findAllByCreateByOrOwners(String user, Pageable pageable);

    @Query("SELECT MAX(s.version) FROM UnstagedContract s WHERE s.id = :id")
    Optional<Integer> findLatestVersion(@Param(ID) Long id);

    @Query("SELECT s FROM UnstagedContract s WHERE s.id = :id AND s.version = "
            + "COALESCE((SELECT MAX(s1.version) FROM UnstagedContract s1 WHERE s1.id = :id), 0)")
    Optional<UnstagedContract> findByIdAndLatestVersion(@Param(ID) Long id);

    @Query("SELECT c FROM UnstagedContract c WHERE"
            + "(LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) "
            + "AND (c.createBy = :user OR c.owners LIKE %:user%)")
    Page<UnstagedContract> findAllByNameOrDescriptionAndUser(String searchTerm, String user, Pageable pageable);

    @Query("SELECT c FROM UnstagedContract c WHERE "
            + "LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(c.description) "
            + "LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<UnstagedContract> findAllByNameOrDescriptionIgnoreCase(String searchTerm, Pageable pageable);
}