package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.commonSvc.repository.SpecificationRepository;
import com.ebay.behavior.gds.mdm.signal.model.view.StagedSignalProductionView;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;

public interface StagedSignalProductionViewRepository extends SpecificationRepository<StagedSignalProductionView, VersionedId> {

    @Query("SELECT s FROM StagedSignalProductionView s WHERE s.id = :id")
    Optional<StagedSignalProductionView> findById(@Param(ID) Long id);

    List<StagedSignalProductionView> findAllByDataSourceAndPlatformId(UdcDataSourceType dataSource, Long platformId);

    @Override
    Page<StagedSignalProductionView> findAll(Specification<StagedSignalProductionView> spec, Pageable pageable);
}
