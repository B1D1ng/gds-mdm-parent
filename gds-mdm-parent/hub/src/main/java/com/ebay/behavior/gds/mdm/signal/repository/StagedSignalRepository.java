package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.commonSvc.repository.SpecificationRepository;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedSignal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;

public interface StagedSignalRepository extends SpecificationRepository<StagedSignal, VersionedId> {

    Optional<StagedSignal> findByIdAndVersion(long id, int version);

    @Override
    Page<StagedSignal> findAll(Specification<StagedSignal> spec, Pageable pageable);

    @Query("SELECT s FROM StagedSignal s WHERE s.id = :id")
    List<StagedSignal> findAllById(@Param(ID) Long id);

    List<StagedSignal> findAllByEnvironmentAndDataSourceAndPlatformId(Environment env, UdcDataSourceType dataSource, Long platformId);

    List<StagedSignal> findAllByDataSourceAndPlatformId(UdcDataSourceType dataSource, Long platformId);
}
