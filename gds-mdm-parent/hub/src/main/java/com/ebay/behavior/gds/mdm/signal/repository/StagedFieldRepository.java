package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.signal.common.model.StagedField;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

// All finder methods will ignore case in MySQL
public interface StagedFieldRepository extends JpaRepository<StagedField, Long>, StagedFieldRepositoryCustom {

    List<StagedField> findAllByIdIn(@Param("ids") Set<Long> ids);

    List<StagedField> findBySignalIdAndSignalVersion(long signalId, int signalVersion); // get associated fields

    Page<StagedField> findByName(String name, Pageable pageable);

    Page<StagedField> findByNameStartingWith(String term, Pageable pageable);

    Page<StagedField> findByNameContaining(String term, Pageable pageable);

    Page<StagedField> findByTag(String term, Pageable pageable);

    Page<StagedField> findByTagStartingWith(String term, Pageable pageable);

    Page<StagedField> findByTagContaining(String term, Pageable pageable);

    Page<StagedField> findByDescription(String term, Pageable pageable);

    Page<StagedField> findByDescriptionStartingWith(String term, Pageable pageable);

    Page<StagedField> findByDescriptionContaining(String term, Pageable pageable);
}
