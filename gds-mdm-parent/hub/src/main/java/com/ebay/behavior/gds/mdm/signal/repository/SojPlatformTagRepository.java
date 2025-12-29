package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.signal.common.model.SojPlatformTag;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SojPlatformTagRepository extends JpaRepository<SojPlatformTag, Long> {

    Optional<SojPlatformTag> findBySojName(String sojName);
}