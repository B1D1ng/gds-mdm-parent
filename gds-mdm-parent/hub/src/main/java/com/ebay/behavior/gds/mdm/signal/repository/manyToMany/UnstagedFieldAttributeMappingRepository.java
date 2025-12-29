package com.ebay.behavior.gds.mdm.signal.repository.manyToMany;

import com.ebay.behavior.gds.mdm.signal.model.manyToMany.UnstagedFieldAttributeMapping;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UnstagedFieldAttributeMappingRepository extends JpaRepository<UnstagedFieldAttributeMapping, Long> {

    Optional<UnstagedFieldAttributeMapping> findByFieldIdAndAttributeId(long fieldId, long attributeId);

    List<UnstagedFieldAttributeMapping> findByFieldId(long fieldId);

    List<UnstagedFieldAttributeMapping> findByAttributeId(long attributeId);

    @Query("SELECT mp FROM UnstagedFieldAttributeMapping mp WHERE mp.attribute.id IN :attributeIds")
    Set<UnstagedFieldAttributeMapping> getFields(Set<Long> attributeIds);
}