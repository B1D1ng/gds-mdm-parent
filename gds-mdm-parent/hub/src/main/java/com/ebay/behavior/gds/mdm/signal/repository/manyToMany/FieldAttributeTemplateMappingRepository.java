package com.ebay.behavior.gds.mdm.signal.repository.manyToMany;

import com.ebay.behavior.gds.mdm.signal.model.manyToMany.FieldAttributeTemplateMapping;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FieldAttributeTemplateMappingRepository extends JpaRepository<FieldAttributeTemplateMapping, Long> {

    Optional<FieldAttributeTemplateMapping> findByFieldIdAndAttributeId(long fieldId, long attributeId);

    List<FieldAttributeTemplateMapping> findByFieldId(long fieldId);

    List<FieldAttributeTemplateMapping> findByAttributeId(long attributeId);

    @Query("SELECT mp FROM FieldAttributeTemplateMapping mp WHERE mp.attribute.id IN :attributeIds")
    Set<FieldAttributeTemplateMapping> getFields(Set<Long> attributeIds);
}