package com.ebay.behavior.gds.mdm.common.service.token;

public interface TokenGenerator {

    String getToken();

    String getTokenHeaderName();

    default TokenHeader getTokenHeader() {
        return new TokenHeader(getTokenHeaderName(), getToken());
    }

    record TokenHeader(String name, Object value) {
    }
}
