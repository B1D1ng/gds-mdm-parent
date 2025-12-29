package com.ebay.behavior.gds.mdm.common.service.token;

import org.apache.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Service
public class UdcTokenGenerator extends AbstractTfTokenGenerator {

    @Override
    public String getTokenHeaderName() {
        return HttpHeaders.AUTHORIZATION;
    }
}