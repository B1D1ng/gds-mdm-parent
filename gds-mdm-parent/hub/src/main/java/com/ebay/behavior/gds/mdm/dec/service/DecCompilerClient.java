package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.service.AbstractRestPostClient;
import com.ebay.behavior.gds.mdm.common.service.token.TokenGenerator;

import jakarta.inject.Named;
import jakarta.ws.rs.client.WebTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.AUTOWIRING_INSPECTION;

@Service
@Validated
public class DecCompilerClient extends AbstractRestPostClient {

    @Autowired
    @Named("decCompiler")
    @SuppressWarnings(AUTOWIRING_INSPECTION)
    private WebTarget target;

    @Override
    protected String getPath() {
        return null;
    }

    @Override
    public WebTarget getTarget() {
        return target;
    }

    @Override
    protected TokenGenerator getTokenGenerator() {
        return null;
    }
}
