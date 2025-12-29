package com.ebay.behavior.gds.mdm.dec.repository;

import com.ebay.behavior.gds.mdm.dec.model.LdmEntityIndex;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LdmEntityIndexRepository extends JpaRepository<LdmEntityIndex, Long> {

    List<LdmEntityIndex> findByBaseEntityId(Long baseEntityId);
}
