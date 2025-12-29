package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.commonSvc.repository.SpecificationRepository;
import com.ebay.behavior.gds.mdm.signal.model.view.StagedSignalStagingView;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;

public interface StagedSignalStagingViewRepository extends SpecificationRepository<StagedSignalStagingView, VersionedId> {

    @Query("SELECT s FROM StagedSignalStagingView s WHERE s.id = :id")
    Optional<StagedSignalStagingView> findById(@Param(ID) Long id);

    List<StagedSignalStagingView> findAllByDataSourceAndPlatformId(UdcDataSourceType dataSource, Long platformId);

    @Override
    Page<StagedSignalStagingView> findAll(Specification<StagedSignalStagingView> spec, Pageable pageable);
}
