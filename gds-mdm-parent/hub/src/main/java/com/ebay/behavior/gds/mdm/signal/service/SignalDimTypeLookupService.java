package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractLookupService;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalDimTypeLookup;
import com.ebay.behavior.gds.mdm.signal.repository.SignalDimTypeLookupRepository;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class SignalDimTypeLookupService extends AbstractLookupService<SignalDimTypeLookup> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<SignalDimTypeLookup> modelType = SignalDimTypeLookup.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private SignalDimTypeLookupRepository repository;
}
