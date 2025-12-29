package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractLookupService;
import com.ebay.behavior.gds.mdm.signal.common.model.PlatformLookup;
import com.ebay.behavior.gds.mdm.signal.repository.PlatformLookupRepository;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class PlatformLookupService extends AbstractLookupService<PlatformLookup> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<PlatformLookup> modelType = PlatformLookup.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private PlatformLookupRepository repository;

    public Long getPlatformId(String platformName) {
        val platform = getByName(platformName);
        return platform.getId();
    }

    public String getPlatformName(Long platformId) {
        val platform = getById(platformId);
        return platform.getName();
    }
}
