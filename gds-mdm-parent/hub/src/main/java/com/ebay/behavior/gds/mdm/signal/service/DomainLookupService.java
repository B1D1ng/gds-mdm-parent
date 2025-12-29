package com.ebay.behavior.gds.mdm.signal.service;

import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class DomainLookupService extends AbstractSignalDimLookupService {

    @Getter
    public String dimensionName = SIGNAL_DOMAIN_DIM_NAME;
}