package com.ebay.behavior.gds.mdm.commonSvc.repository;

import com.ebay.behavior.gds.mdm.common.model.audit.HistoryAuditable;

import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

@NoRepositoryBean
public interface VersionedHistoryRepository<T extends HistoryAuditable> extends HistoryRepository<T> {

    List<T> findByOriginalIdAndOriginalVersion(Long originalId, Integer originalVersion);
}
