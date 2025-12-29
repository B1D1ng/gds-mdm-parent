package com.ebay.behavior.gds.mdm.signal.repository.manyToMany;

import com.ebay.behavior.gds.mdm.signal.model.manyToMany.StagedFieldAttributeMapping;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface StagedFieldAttributeMappingRepository extends JpaRepository<StagedFieldAttributeMapping, Long> {

    Optional<StagedFieldAttributeMapping> findByFieldIdAndAttributeId(long fieldId, long attributeId);

    List<StagedFieldAttributeMapping> findByFieldId(long fieldId);

    @Query("SELECT mp FROM StagedFieldAttributeMapping mp WHERE mp.attribute.id IN :attributeIds")
    Set<StagedFieldAttributeMapping> getFields(Set<Long> attributeIds);
}