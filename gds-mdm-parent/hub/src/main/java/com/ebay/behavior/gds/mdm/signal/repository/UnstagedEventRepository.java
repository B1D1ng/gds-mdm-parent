package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UnstagedEventRepository extends JpaRepository<UnstagedEvent, Long> {

    Page<UnstagedEvent> findByName(String term, Pageable pageable);

    Page<UnstagedEvent> findByNameStartingWith(String term, Pageable pageable);

    Page<UnstagedEvent> findByNameContaining(String term, Pageable pageable);

    Page<UnstagedEvent> findByType(String term, Pageable pageable);

    Page<UnstagedEvent> findByTypeStartingWith(String term, Pageable pageable);

    Page<UnstagedEvent> findByTypeContaining(String term, Pageable pageable);

    Page<UnstagedEvent> findByDescription(String term, Pageable pageable);

    Page<UnstagedEvent> findByDescriptionStartingWith(String term, Pageable pageable);

    Page<UnstagedEvent> findByDescriptionContaining(String term, Pageable pageable);

    @Query("SELECT e FROM UnstagedEvent e JOIN FETCH e.pageIds WHERE :pageId MEMBER OF e.pageIds")
    Page<UnstagedEvent> findByPageId(@Param("pageId") Long pageId, Pageable pageable);

    @Query("SELECT e FROM UnstagedEvent e JOIN FETCH e.moduleIds WHERE :moduleId MEMBER OF e.moduleIds")
    Page<UnstagedEvent> findByModuleId(@Param("moduleId") Long moduleId, Pageable pageable);

    @Query("SELECT e FROM UnstagedEvent e JOIN FETCH e.clickIds WHERE :clickId MEMBER OF e.clickIds")
    Page<UnstagedEvent> findByClickId(@Param("clickId") Long clickId, Pageable pageable);
}