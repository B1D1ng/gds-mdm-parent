package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.signal.common.model.FieldTemplate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

// All finder methods will ignore case in MySQL
public interface FieldTemplateRepository extends JpaRepository<FieldTemplate, Long> {

    Set<FieldTemplate> findBySignalTemplateId(long signalId); // get associated fields

    Page<FieldTemplate> findByName(String name, Pageable pageable);

    Page<FieldTemplate> findByNameStartingWith(String term, Pageable pageable);

    Page<FieldTemplate> findByNameContaining(String term, Pageable pageable);

    Page<FieldTemplate> findByTag(String term, Pageable pageable);

    Page<FieldTemplate> findByTagStartingWith(String term, Pageable pageable);

    Page<FieldTemplate> findByTagContaining(String term, Pageable pageable);

    Page<FieldTemplate> findByDescription(String term, Pageable pageable);

    Page<FieldTemplate> findByDescriptionStartingWith(String term, Pageable pageable);

    Page<FieldTemplate> findByDescriptionContaining(String term, Pageable pageable);
}