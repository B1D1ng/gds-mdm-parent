package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.signal.common.model.SojEvent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;

public interface SojEventRepository extends JpaRepository<SojEvent, Long> {

    Optional<SojEvent> findByActionAndPageIdAndModuleIdAndClickId(String action, Long pageId, Long moduleId, Long clickId);

    Set<SojEvent> findByPageIdIn(Set<Long> pageIds);

    Set<SojEvent> findByModuleIdIn(Set<Long> moduleIds);

    Set<SojEvent> findByClickIdIn(Set<Long> clickIds);

    Set<SojEvent> findByModuleIdInAndClickIdIn(Set<Long> moduleIds, Set<Long> clickIds);
}