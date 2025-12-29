package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractLookupService;
import com.ebay.behavior.gds.mdm.signal.common.model.SurfaceTypeLookup;
import com.ebay.behavior.gds.mdm.signal.repository.SurfaceTypeRepository;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class SurfaceTypeLookupService extends AbstractLookupService<SurfaceTypeLookup> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<SurfaceTypeLookup> modelType = SurfaceTypeLookup.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private SurfaceTypeRepository repository;
}
