package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.signal.common.model.SignalDimTypeLookup;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalDimValueLookup;
import com.ebay.behavior.gds.mdm.signal.repository.SignalDimValueLookupRepository;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractSignalDimLookupService extends AbstractDimLookupService<SignalDimValueLookup, SignalDimTypeLookup> {

    @Autowired
    @Getter
    private SignalDimTypeLookupService dimensionLookupService;

    @Getter(AccessLevel.PROTECTED)
    private final Class<SignalDimValueLookup> modelType = SignalDimValueLookup.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private SignalDimValueLookupRepository repository;
}