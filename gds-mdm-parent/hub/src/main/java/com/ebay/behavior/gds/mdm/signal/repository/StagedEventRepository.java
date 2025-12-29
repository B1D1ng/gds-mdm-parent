package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.signal.common.model.StagedEvent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface StagedEventRepository extends JpaRepository<StagedEvent, Long> {

    List<StagedEvent> findAllByIdIn(@Param("ids") Set<Long> ids);

    Page<StagedEvent> findByName(String term, Pageable pageable);

    Page<StagedEvent> findByNameStartingWith(String term, Pageable pageable);

    Page<StagedEvent> findByNameContaining(String term, Pageable pageable);

    Page<StagedEvent> findByType(String term, Pageable pageable);

    Page<StagedEvent> findByTypeStartingWith(String term, Pageable pageable);

    Page<StagedEvent> findByTypeContaining(String term, Pageable pageable);

    Page<StagedEvent> findByDescription(String term, Pageable pageable);

    Page<StagedEvent> findByDescriptionStartingWith(String term, Pageable pageable);

    Page<StagedEvent> findByDescriptionContaining(String term, Pageable pageable);

    @Query("SELECT e FROM StagedEvent e JOIN FETCH e.pageIds WHERE :pageId MEMBER OF e.pageIds")
    Page<StagedEvent> findByPageId(@Param("pageId") Long pageId, Pageable pageable);

    @Query("SELECT e FROM StagedEvent e JOIN FETCH e.moduleIds WHERE :moduleId MEMBER OF e.moduleIds")
    Page<StagedEvent> findByModuleId(@Param("moduleId") Long moduleId, Pageable pageable);

    @Query("SELECT e FROM StagedEvent e JOIN FETCH e.clickIds WHERE :clickId MEMBER OF e.clickIds")
    Page<StagedEvent> findByClickId(@Param("clickId") Long clickId, Pageable pageable);
}