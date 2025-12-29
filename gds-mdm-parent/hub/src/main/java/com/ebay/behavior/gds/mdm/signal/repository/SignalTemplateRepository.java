package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.signal.common.model.SignalTemplate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

// All finder methods will ignore case in MySQL
public interface SignalTemplateRepository extends JpaRepository<SignalTemplate, Long> {

    Page<SignalTemplate> findByName(String term, Pageable pageable);

    Page<SignalTemplate> findByNameStartingWith(String term, Pageable pageable);

    Page<SignalTemplate> findByNameContaining(String term, Pageable pageable);

    Page<SignalTemplate> findByType(String term, Pageable pageable);

    Optional<SignalTemplate> findByType(String type);

    Page<SignalTemplate> findByTypeStartingWith(String term, Pageable pageable);

    Page<SignalTemplate> findByTypeContaining(String term, Pageable pageable);

    Page<SignalTemplate> findByDescription(String term, Pageable pageable);

    Page<SignalTemplate> findByDescriptionStartingWith(String term, Pageable pageable);

    Page<SignalTemplate> findByDescriptionContaining(String term, Pageable pageable);

    Page<SignalTemplate> findByDomain(String term, Pageable pageable);

    Page<SignalTemplate> findByDomainStartingWith(String term, Pageable pageable);

    Page<SignalTemplate> findByDomainContaining(String term, Pageable pageable);

    @Query("SELECT a.eventTemplateId FROM AttributeTemplate a "
            + "JOIN FieldAttributeTemplateMapping mp ON a.id = mp.attribute.id "
            + "JOIN FieldTemplate f ON f.id = mp.field.id " + "WHERE f.signalTemplateId = :signalId")
    Set<Long> getConnectedEventIds(@Param("signalId") long signalId);

    @Query("SELECT distinct type FROM SignalTemplate WHERE platformId = :platform")
    Set<String> getTypesByPlatform(@Param("platform") Long platform);
}
