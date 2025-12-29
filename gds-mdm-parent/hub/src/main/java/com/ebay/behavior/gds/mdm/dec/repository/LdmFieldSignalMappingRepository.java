package com.ebay.behavior.gds.mdm.dec.repository;

import com.ebay.behavior.gds.mdm.dec.model.LdmFieldSignalMapping;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface LdmFieldSignalMappingRepository
        extends JpaRepository<LdmFieldSignalMapping, Long> {

    @Modifying
    @Query("DELETE FROM LdmFieldSignalMapping WHERE ldmFieldId IN :fieldIds")
    void deleteByFieldId(Set<Long> fieldIds);

    List<LdmFieldSignalMapping> findByLdmFieldId(Long ldmFieldId);
}
