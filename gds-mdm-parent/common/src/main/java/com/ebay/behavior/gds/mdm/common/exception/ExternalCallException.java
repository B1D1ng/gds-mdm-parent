package com.ebay.behavior.gds.mdm.common.exception;

import lombok.Getter;

public class ExternalCallException extends MdmException {

    @Getter
    private Integer statusCode;

    public ExternalCallException() {
        super();
    }

    public ExternalCallException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public ExternalCallException(int statusCode, String message, Exception cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public ExternalCallException(int statusCode, Exception cause) {
        super(cause);
        this.statusCode = statusCode;
    }

    public ExternalCallException(String message) {
        super(message);
    }

    public ExternalCallException(String message, Exception cause) {
        super(message, cause);
    }

    public ExternalCallException(Exception cause) {
        super(cause);
    }
}
