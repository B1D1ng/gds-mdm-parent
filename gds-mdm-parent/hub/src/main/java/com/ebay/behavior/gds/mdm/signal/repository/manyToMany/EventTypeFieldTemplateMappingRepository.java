package com.ebay.behavior.gds.mdm.signal.repository.manyToMany;

import com.ebay.behavior.gds.mdm.signal.model.manyToMany.EventTypeFieldTemplateMapping;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;
import java.util.Set;

public interface EventTypeFieldTemplateMappingRepository extends JpaRepository<EventTypeFieldTemplateMapping, Long> {

    Set<EventTypeFieldTemplateMapping> findByFieldId(long fieldId);

    Optional<EventTypeFieldTemplateMapping> findByFieldIdAndEventTypeId(long fieldId, long eventTypeId);

    @Modifying
    void deleteByFieldId(long fieldId);
}