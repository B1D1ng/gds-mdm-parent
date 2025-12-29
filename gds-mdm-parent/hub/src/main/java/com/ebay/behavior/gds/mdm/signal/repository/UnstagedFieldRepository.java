package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// All finder methods will ignore case in MySQL
public interface UnstagedFieldRepository extends JpaRepository<UnstagedField, Long> {

    List<UnstagedField> findBySignalIdAndSignalVersion(long signalId, int signalVersion); // get associated fields

    Page<UnstagedField> findByName(String name, Pageable pageable);

    Page<UnstagedField> findByNameStartingWith(String term, Pageable pageable);

    Page<UnstagedField> findByNameContaining(String term, Pageable pageable);

    Page<UnstagedField> findByTag(String term, Pageable pageable);

    Page<UnstagedField> findByTagStartingWith(String term, Pageable pageable);

    Page<UnstagedField> findByTagContaining(String term, Pageable pageable);

    Page<UnstagedField> findByDescription(String term, Pageable pageable);

    Page<UnstagedField> findByDescriptionStartingWith(String term, Pageable pageable);

    Page<UnstagedField> findByDescriptionContaining(String term, Pageable pageable);
}
