package com.ebay.behavior.gds.mdm.signal.repository.manyToMany;

import com.ebay.behavior.gds.mdm.signal.model.manyToMany.TemplateQuestionEventMapping;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface TemplateQuestionEventMappingRepository extends JpaRepository<TemplateQuestionEventMapping, Long> {

    Optional<TemplateQuestionEventMapping> findByQuestionIdAndEventTemplateId(long questionId, long eventTemplateId);

    Set<TemplateQuestionEventMapping> findByEventTemplateId(long eventTemplateId);

    @Query("SELECT DISTINCT map FROM TemplateQuestionEventMapping map WHERE map.eventTemplate.id IN :eventTemplateIds")
    Set<TemplateQuestionEventMapping> findByEventTemplateIds(@Param("eventTemplateIds") Set<Long> eventTemplateIds);
}