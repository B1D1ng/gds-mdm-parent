package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.signal.common.model.EventSource;
import com.ebay.behavior.gds.mdm.signal.common.model.EventTemplate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

// All finder methods will ignore case in MySQL
public interface EventTemplateRepository extends JpaRepository<EventTemplate, Long> {

    Page<EventTemplate> findByName(String term, Pageable pageable);

    Page<EventTemplate> findByNameStartingWith(String term, Pageable pageable);

    Page<EventTemplate> findByNameContaining(String term, Pageable pageable);

    Page<EventTemplate> findByType(String term, Pageable pageable);

    Page<EventTemplate> findByTypeStartingWith(String term, Pageable pageable);

    Page<EventTemplate> findByTypeContaining(String term, Pageable pageable);

    Page<EventTemplate> findByDescription(String term, Pageable pageable);

    Page<EventTemplate> findByDescriptionStartingWith(String term, Pageable pageable);

    Page<EventTemplate> findByDescriptionContaining(String term, Pageable pageable);

    Set<EventTemplate> findBySource(EventSource source);
}
