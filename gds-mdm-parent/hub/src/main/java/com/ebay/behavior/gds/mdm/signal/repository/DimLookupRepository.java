package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.signal.common.model.AbstractDimLookup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@NoRepositoryBean
public interface DimLookupRepository<T extends AbstractDimLookup> extends JpaRepository<T, Long> {

    Optional<T> findByDimensionTypeIdAndName(long dimensionTypeId, String name);

    List<T> findByDimensionTypeIdAndNameIn(long dimensionTypeId, Set<String> names);

    List<T> findAllByDimensionTypeId(long dimensionTypeId);
}
