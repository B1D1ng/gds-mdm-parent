package com.ebay.behavior.gds.mdm.signal.repository.migration;

import com.ebay.behavior.gds.mdm.signal.model.migration.SignalMigrationJob;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SignalMigrationJobRepository extends JpaRepository<SignalMigrationJob, Long> {

    Optional<SignalMigrationJob> findByJobId(long jobId);
}
