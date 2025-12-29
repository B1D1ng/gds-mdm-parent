package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedAttribute;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// All finder methods will ignore case in MySQL
public interface UnstagedAttributeRepository extends JpaRepository<UnstagedAttribute, Long> {

    List<UnstagedAttribute> findByEventId(long eventId); // get associated attributes

    Page<UnstagedAttribute> findByTag(String term, Pageable pageable);

    Page<UnstagedAttribute> findByTagStartingWith(String term, Pageable pageable);

    Page<UnstagedAttribute> findByTagContaining(String term, Pageable pageable);

    Page<UnstagedAttribute> findBySchemaPathStartingWith(String term, Pageable pageable);

    Page<UnstagedAttribute> findBySchemaPathContaining(String term, Pageable pageable);

    Page<UnstagedAttribute> findByDescription(String term, Pageable pageable);

    Page<UnstagedAttribute> findByDescriptionStartingWith(String term, Pageable pageable);

    Page<UnstagedAttribute> findByDescriptionContaining(String term, Pageable pageable);
}
