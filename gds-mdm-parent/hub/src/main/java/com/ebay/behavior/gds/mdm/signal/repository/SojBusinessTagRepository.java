package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.signal.common.model.SojBusinessTag;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SojBusinessTagRepository extends JpaRepository<SojBusinessTag, Long> {

    Optional<SojBusinessTag> findBySojName(String sojName);
}