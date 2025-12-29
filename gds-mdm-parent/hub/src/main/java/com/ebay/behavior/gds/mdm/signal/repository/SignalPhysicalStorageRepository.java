package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalPhysicalStorage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SignalPhysicalStorageRepository extends JpaRepository<SignalPhysicalStorage, Long> {

    @Query("SELECT p FROM SignalPhysicalStorage p WHERE p.kafkaTopic = :kafkaTopic and p.environment = :environment")
    Optional<SignalPhysicalStorage> findByKafkaTopicAndEnvironment(String kafkaTopic, Environment environment);
}
