package com.ebay.behavior.gds.mdm.common.exception;

import lombok.Getter;
import lombok.Setter;

/**
 * A base Schema service exception type.
 */
public class MdmException extends RuntimeException {

    @Getter
    @Setter
    private String uiMessage;

    public MdmException() {
        super();
    }

    public MdmException(String message) {
        super(message);
    }

    public MdmException(String message, Exception cause) {
        super(message, cause);
    }

    public MdmException(Exception cause) {
        super(cause);
    }
}
