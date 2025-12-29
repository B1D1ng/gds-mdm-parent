package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractLookupService;
import com.ebay.behavior.gds.mdm.signal.common.model.ChannelIdLookup;
import com.ebay.behavior.gds.mdm.signal.repository.ChannelIdLookupRepository;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class ChannelIdLookupService extends AbstractLookupService<ChannelIdLookup> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<ChannelIdLookup> modelType = ChannelIdLookup.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private ChannelIdLookupRepository repository;
}
