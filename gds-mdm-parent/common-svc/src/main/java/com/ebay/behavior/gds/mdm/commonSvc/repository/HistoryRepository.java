package com.ebay.behavior.gds.mdm.commonSvc.repository;

import com.ebay.behavior.gds.mdm.common.model.audit.HistoryAuditable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

@NoRepositoryBean
public interface HistoryRepository<T extends HistoryAuditable> extends JpaRepository<T, Long> {

    List<T> findByOriginalId(Long originalId);
}
