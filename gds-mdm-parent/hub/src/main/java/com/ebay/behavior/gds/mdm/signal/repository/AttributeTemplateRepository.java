package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.signal.common.model.AttributeTemplate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

// All finder methods will ignore case in MySQL
public interface AttributeTemplateRepository extends JpaRepository<AttributeTemplate, Long> {

    Set<AttributeTemplate> findByEventTemplateId(long eventId); // get associated attributes

    Page<AttributeTemplate> findByTag(String term, Pageable pageable);

    Page<AttributeTemplate> findByTagStartingWith(String term, Pageable pageable);

    Page<AttributeTemplate> findByTagContaining(String term, Pageable pageable);

    Page<AttributeTemplate> findBySchemaPathStartingWith(String term, Pageable pageable);

    Page<AttributeTemplate> findBySchemaPathContaining(String term, Pageable pageable);

    Page<AttributeTemplate> findByDescription(String term, Pageable pageable);

    Page<AttributeTemplate> findByDescriptionStartingWith(String term, Pageable pageable);

    Page<AttributeTemplate> findByDescriptionContaining(String term, Pageable pageable);
}
