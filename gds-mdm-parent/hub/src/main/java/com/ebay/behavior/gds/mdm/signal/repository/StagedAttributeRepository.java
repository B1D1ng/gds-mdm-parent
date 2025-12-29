package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.signal.common.model.StagedAttribute;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

// All finder methods will ignore case in MySQL
public interface StagedAttributeRepository extends JpaRepository<StagedAttribute, Long> {

    List<StagedAttribute> findAllByEventIdIn(Set<Long> eventIds); // get associated attributes

    List<StagedAttribute> findByEventId(long eventId); // get associated attributes

    Page<StagedAttribute> findByTag(String term, Pageable pageable);

    Page<StagedAttribute> findByTagStartingWith(String term, Pageable pageable);

    Page<StagedAttribute> findByTagContaining(String term, Pageable pageable);

    Page<StagedAttribute> findBySchemaPathStartingWith(String term, Pageable pageable);

    Page<StagedAttribute> findBySchemaPathContaining(String term, Pageable pageable);

    Page<StagedAttribute> findByDescription(String term, Pageable pageable);

    Page<StagedAttribute> findByDescriptionStartingWith(String term, Pageable pageable);

    Page<StagedAttribute> findByDescriptionContaining(String term, Pageable pageable);
}
