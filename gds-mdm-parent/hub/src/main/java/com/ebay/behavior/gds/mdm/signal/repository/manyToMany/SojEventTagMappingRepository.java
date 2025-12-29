package com.ebay.behavior.gds.mdm.signal.repository.manyToMany;

import com.ebay.behavior.gds.mdm.signal.model.manyToMany.SojEventTagMapping;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface SojEventTagMappingRepository extends JpaRepository<SojEventTagMapping, Long> {

    List<SojEventTagMapping> findBySojEventId(long sojEventId);

    @Query("SELECT CONCAT(e.sojEvent, ';', e.sojTag) FROM SojEventTagMapping e")
    Set<String> findAllMappings();
}