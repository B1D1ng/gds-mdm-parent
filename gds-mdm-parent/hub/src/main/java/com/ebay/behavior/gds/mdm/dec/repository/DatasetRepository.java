package com.ebay.behavior.gds.mdm.dec.repository;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.dec.model.Dataset;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DatasetRepository extends JpaRepository<Dataset, VersionedId> {

    @Query("SELECT s FROM Dataset s INNER JOIN DatasetIndex v ON s.id = v.id and s.version = v.currentVersion WHERE v.id = :id")
    Optional<Dataset> findByIdCurrentVersion(@Param("id") Long id);

    @Query("SELECT s FROM Dataset s INNER JOIN DatasetIndex v ON s.id = v.id and s.version = v.currentVersion")
    List<Dataset> findAllCurrentVersion();

    // find all by name and latest version
    @Query("SELECT s FROM Dataset s INNER JOIN DatasetIndex v ON s.id = v.id and s.version = v.currentVersion WHERE s.name = :name")
    List<Dataset> findAllByNameCurrentVersion(@Param("name") String name);

    // find all by ldm id and ldm version
    @Query("SELECT s FROM Dataset s INNER JOIN DatasetIndex v ON s.id = v.id and s.version = v.currentVersion "
            + "WHERE s.ldmEntityId = :ldmEntityId and s.ldmVersion = :ldmVersion")
    List<Dataset> findAllByLdmEntityIdCurrentVersion(@Param("ldmEntityId") Long ldmEntityId, @Param("ldmVersion") Integer ldmVersion);

    // find all historical versions by id
    List<Dataset> findAllById(@Param("id") Long id);

    List<Dataset> findAllByLdmEntityId(@Param("ldmEntityId") Long ldmEntityId);

    // find all by name and namespace
    @Query("SELECT s FROM Dataset s INNER JOIN DatasetIndex v ON s.id = v.id and s.version = v.currentVersion "
            + "INNER JOIN Namespace n ON s.namespaceId = n.id "
            + "WHERE s.name = :name and n.name = :namespaceName")
    List<Dataset> findAllByNameAndNamespaceCurrentVersion(@Param("name") String name, @Param("namespaceName") String namespaceName);

    // find all by name and namespace id
    @Query("SELECT s FROM Dataset s INNER JOIN DatasetIndex v ON s.id = v.id and s.version = v.currentVersion "
            + "WHERE s.name = :name and s.namespaceId = :namespaceId")
    List<Dataset> findAllByNameAndNamespaceIdCurrentVersion(@Param("name") String name, @Param("namespaceId") Long namespaceId);

    // find all by namespace name and latest version
    @Query("SELECT s FROM Dataset s INNER JOIN DatasetIndex v ON s.id = v.id and s.version = v.currentVersion "
            + "INNER JOIN Namespace n ON s.namespaceId = n.id "
            + "WHERE n.name = :namespaceName")
    List<Dataset> findAllByNamespaceNameCurrentVersion(@Param("namespaceName") String namespaceName);
}
