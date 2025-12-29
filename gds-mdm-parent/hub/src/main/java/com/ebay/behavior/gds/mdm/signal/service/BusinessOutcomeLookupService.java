package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractLookupService;
import com.ebay.behavior.gds.mdm.signal.common.model.BusinessOutcomeLookup;
import com.ebay.behavior.gds.mdm.signal.repository.BusinessOutcomeLookupRepository;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class BusinessOutcomeLookupService extends AbstractLookupService<BusinessOutcomeLookup> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<BusinessOutcomeLookup> modelType = BusinessOutcomeLookup.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private BusinessOutcomeLookupRepository repository;
}
