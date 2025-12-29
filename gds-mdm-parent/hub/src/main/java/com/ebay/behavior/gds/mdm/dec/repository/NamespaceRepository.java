package com.ebay.behavior.gds.mdm.dec.repository;

import com.ebay.behavior.gds.mdm.dec.model.Namespace;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NamespaceRepository extends JpaRepository<Namespace, Long> {

    List<Namespace> findAllByName(@Param("name") String name);
}
