package com.ebay.behavior.gds.mdm.common.service;

import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.model.PlatformEnvironment;
import com.ebay.raptorio.env.PlatformEnvProperties;

import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class PlatformAware {

    @Autowired
    private PlatformEnvProperties platformEnvProperties;

    public boolean isProduction() {
        val env = PlatformEnvironment.fromValue(platformEnvProperties.getPlatformEnvironment());
        return env == PlatformEnvironment.PRODUCTION || env == PlatformEnvironment.PRE_PRODUCTION;
    }

    public Environment getStagedEnvironment() {
        return isProduction() ? Environment.PRODUCTION : Environment.STAGING;
    }
}
