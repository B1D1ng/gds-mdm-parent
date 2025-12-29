package com.ebay.behavior.gds.mdm.dec.repository;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.enums.ViewType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LdmEntityRepository extends JpaRepository<LdmEntity, VersionedId> {

    @Query(
            "SELECT s FROM LdmEntity s INNER JOIN LdmEntityIndex v ON s.id = v.id and s.version = v.currentVersion "
                    + "WHERE v.id = :id")
    Optional<LdmEntity> findByIdCurrentVersion(@Param("id") Long id);

    @Query(
            "SELECT s FROM LdmEntity s INNER JOIN LdmEntityIndex v ON s.id = v.id and s.version = v.currentVersion "
                    + "WHERE s.baseEntityId = :entityId")
    List<LdmEntity> findByEntityIdCurrentVersion(Long entityId);

    @Query(
            "SELECT s FROM LdmEntity s INNER JOIN LdmEntityIndex v ON s.id = v.id and s.version = v.currentVersion")
    List<LdmEntity> findAllCurrentVersion();

    // find all by name + view type and latest version
    @Query(
            "SELECT s FROM LdmEntity s INNER JOIN LdmEntityIndex v ON s.id = v.id and s.version = v.currentVersion "
                    + "WHERE s.name = :name and s.viewType = :viewType")
    List<LdmEntity> findAllByNameAndTypeCurrentVersion(@Param("name") String name, @Param("viewType") ViewType viewType);

    // find all by name + view type + namespace and latest version
    @Query(
            "SELECT s FROM LdmEntity s INNER JOIN LdmEntityIndex v ON s.id = v.id and s.version = v.currentVersion "
                    + "INNER JOIN Namespace n ON s.namespaceId = n.id "
                    + "WHERE s.name = :name and s.viewType = :viewType and n.name = :namespaceName")
    List<LdmEntity> findAllByNameAndTypeAndNamespaceCurrentVersion(@Param("name") String name, @Param("viewType") ViewType viewType,
                                                                   @Param("namespaceName") String namespaceName);

    // find all by name + namespace and latest version
    @Query(
            "SELECT s FROM LdmEntity s INNER JOIN LdmEntityIndex v ON s.id = v.id and s.version = v.currentVersion "
                    + "INNER JOIN Namespace n ON s.namespaceId = n.id "
                    + "WHERE s.name = :name and n.name = :namespaceName")
    List<LdmEntity> findAllByNameAndNamespaceCurrentVersion(@Param("name") String name, @Param("namespaceName") String namespaceName);

    // find all by name + view type + namespace id and latest version
    @Query(
            "SELECT s FROM LdmEntity s INNER JOIN LdmEntityIndex v ON s.id = v.id and s.version = v.currentVersion "
                    + "WHERE s.name = :name and s.viewType = :viewType and s.namespaceId = :namespaceId")
    List<LdmEntity> findAllByNameAndTypeAndNamespaceIdCurrentVersion(@Param("name") String name, @Param("viewType") ViewType viewType,
                                                                     @Param("namespaceId") Long namespaceId);

    // findAllCurrentVersion for base LDMs
    @Query(
            "SELECT s FROM LdmEntity s INNER JOIN LdmEntityIndex v ON s.id = v.id and s.version = v.currentVersion "
                    + "INNER JOIN Namespace n ON s.namespaceId = n.id and n.type = 'BASE'")
    List<LdmEntity> findAllBaseLdmCurrentVersion();

    // find all by namespace name and latest version
    @Query(
            "SELECT s FROM LdmEntity s INNER JOIN LdmEntityIndex v ON s.id = v.id and s.version = v.currentVersion "
                    + "INNER JOIN Namespace n ON s.namespaceId = n.id "
                    + "WHERE n.name = :namespaceName")
    List<LdmEntity> findAllByNamespaceNameCurrentVersion(@Param("namespaceName") String namespaceName);

    // find all by name and latest version
    @Query("SELECT s FROM LdmEntity s INNER JOIN LdmEntityIndex v ON s.id = v.id and s.version = v.currentVersion WHERE s.name = :name")
    List<LdmEntity> findAllByNameCurrentVersion(@Param("name") String name);

    List<LdmEntity> findAllById(@Param("id") Long id);

    // find all by upstream ldm
    @Query("SELECT s FROM LdmEntity s WHERE CONCAT(',', s.upstreamLdm, ',') LIKE CONCAT('%,', :upstreamLdmId, ',%') ")
    List<LdmEntity> findAllByUpstreamLdmId(@Param("upstreamLdmId") Long upstreamLdmId);

    // find all DCS entities by upstream ldm id
    @Query("SELECT s FROM LdmEntity s "
            + "WHERE s.isDcs = true AND CONCAT(',', s.upstreamLdm, ',') LIKE CONCAT('%,', :upstreamLdmId, ',%') ")
    List<LdmEntity> findDcsByUpstreamLdmId(@Param("upstreamLdmId") Long upstreamLdmId);
}
