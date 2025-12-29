package com.ebay.behavior.gds.mdm.common.exception;

import lombok.Getter;

@Getter
public class UdcException extends MdmException {

    private final String requestId;

    public UdcException(String requestId, String message) {
        super(message);
        this.requestId = requestId;
    }

    public UdcException(String requestId, Exception ex) {
        super(ex);
        this.requestId = requestId;
    }

    @Override
    public String toString() {
        return String.format("UdcException [requestId=%s, message='%s]", requestId, getMessage());
    }
}
