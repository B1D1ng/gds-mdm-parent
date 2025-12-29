package com.ebay.behavior.gds.mdm.common.service.token;

import com.ebay.platform.security.trustfabric.client.TfTokenClient;
import com.ebay.platform.security.trustfabric.exception.TokenException;

import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractTfTokenGenerator implements TokenGenerator {

    @Autowired
    private TfTokenClient tfTokenClient;

    @Override
    public String getToken() {
        if (!tfTokenClient.isTokenAvailable()) {
            throw new TokenException("tfTokenClient returns false when calling isTokenAvailable(). Trust Fabric functionality is unavailable.");
        }
        return tfTokenClient.getTokenWithBearerPrefix();
    }

    @Override
    public abstract String getTokenHeaderName();
}
