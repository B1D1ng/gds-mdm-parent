package com.ebay.behavior.gds.mdm.dec.repository;

import com.ebay.behavior.gds.mdm.dec.model.LdmFieldPhysicalStorageMapping;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface LdmFieldPhysicalStorageMappingRepository
        extends JpaRepository<LdmFieldPhysicalStorageMapping, Long> {

    @Modifying
    @Query("DELETE FROM LdmFieldPhysicalStorageMapping WHERE ldmFieldId IN :fieldIds")
    void deleteByFieldId(Set<Long> fieldIds);

    List<LdmFieldPhysicalStorageMapping> findByLdmFieldId(Long ldmFieldId);

    List<LdmFieldPhysicalStorageMapping> findByPhysicalStorageId(Long physicalStorageId);

    @Query("SELECT mp FROM LdmFieldPhysicalStorageMapping mp JOIN LdmField f ON mp.ldmFieldId = f.id WHERE f.ldmEntityId = :ldmEntityId")
    List<LdmFieldPhysicalStorageMapping> findByLdmEntityId(Long ldmEntityId);

    @Query("SELECT mp FROM LdmFieldPhysicalStorageMapping mp JOIN LdmField f ON mp.ldmFieldId = f.id "
            + "WHERE mp.physicalStorageId IN :storageIds AND f.ldmEntityId <> :ldmEntityId")
    Set<LdmFieldPhysicalStorageMapping> findSharedStorage(Set<Long> storageIds, Long ldmEntityId);
}
