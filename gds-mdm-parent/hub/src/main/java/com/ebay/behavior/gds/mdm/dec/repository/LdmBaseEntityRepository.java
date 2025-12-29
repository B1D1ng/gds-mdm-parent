package com.ebay.behavior.gds.mdm.dec.repository;

import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LdmBaseEntityRepository extends JpaRepository<LdmBaseEntity, Long> {

    List<LdmBaseEntity> findByNameAndNamespaceId(String name, Long namespaceId);

    @Query("SELECT s FROM LdmBaseEntity s INNER JOIN Namespace n ON s.namespaceId = n.id WHERE s.name = :name and n.name = :namespaceName")
    List<LdmBaseEntity> findByNameAndNamespaceName(String name, String namespaceName);

    List<LdmBaseEntity> findByName(String name);

    @Query("SELECT s FROM LdmBaseEntity s INNER JOIN Namespace n ON s.namespaceId = n.id WHERE n.name = :namespaceName")
    List<LdmBaseEntity> findByNamespaceName(String namespaceName);
}
