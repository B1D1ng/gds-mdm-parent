package com.ebay.behavior.gds.mdm.dec.repository;

import com.ebay.behavior.gds.mdm.dec.model.LdmField;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LdmFieldRepository extends JpaRepository<LdmField, Long> {
    @Query(
            "SELECT s FROM LdmField s INNER JOIN LdmEntityIndex v ON s.ldmEntityId = v.id and s.ldmVersion = v.currentVersion "
                    + "WHERE s.ldmEntityId = :entityId and (s.dataType is null OR s.dataType <> 'FIELD_GROUP')")
    List<LdmField> findByEntityId(@Param("entityId") Long entityId);

    @Query("SELECT s FROM LdmField s WHERE s.ldmEntityId = :entityId")
    List<LdmField> findByEntityIdAllVersions(@Param("entityId") Long entityId);

    @Query(
            "SELECT s FROM LdmField s INNER JOIN LdmEntityIndex v ON s.ldmEntityId = v.id and s.ldmVersion = v.currentVersion "
                    + "WHERE s.ldmEntityId = :entityId and s.dataType = 'FIELD_GROUP'")
    List<LdmField> findFieldGroupByEntityId(@Param("entityId") Long entityId);
}
