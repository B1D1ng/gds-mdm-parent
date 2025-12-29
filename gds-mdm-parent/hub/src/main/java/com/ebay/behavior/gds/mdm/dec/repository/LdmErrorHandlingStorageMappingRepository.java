package com.ebay.behavior.gds.mdm.dec.repository;

import com.ebay.behavior.gds.mdm.dec.model.LdmErrorHandlingStorageMapping;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Repository for managing LdmErrorHandlingStorageMapping entities.
 */
public interface LdmErrorHandlingStorageMappingRepository
        extends JpaRepository<LdmErrorHandlingStorageMapping, Long> {

    /**
     * Find mappings by LDM entity ID and current version.
     *
     * @param ldmEntityId the LDM entity ID
     * @return list of mappings
     */
    @Query(
            "SELECT s FROM LdmErrorHandlingStorageMapping s INNER JOIN LdmEntityIndex v ON s.ldmEntityId = v.id and s.ldmVersion = v.currentVersion "
                    + "WHERE s.ldmEntityId = :ldmEntityId")
    List<LdmErrorHandlingStorageMapping> findByLdmEntityIdCurrentVersion(Long ldmEntityId);

    /**
     * Find mappings by LDM entity ID.
     *
     * @param ldmEntityId the LDM entity ID
     * @return list of mappings
     */
    List<LdmErrorHandlingStorageMapping> findByLdmEntityId(Long ldmEntityId);

    /**
     * Find mappings by LDM entity ID and version.
     *
     * @param ldmEntityId the LDM entity ID
     * @param ldmVersion the LDM version
     * @return list of mappings
     */
    List<LdmErrorHandlingStorageMapping> findByLdmEntityIdAndLdmVersion(Long ldmEntityId, Integer ldmVersion);
}
