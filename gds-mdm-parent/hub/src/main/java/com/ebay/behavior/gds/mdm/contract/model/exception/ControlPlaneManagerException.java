package com.ebay.behavior.gds.mdm.contract.model.exception;

public class ControlPlaneManagerException extends RuntimeException {
    public ControlPlaneManagerException(String message) {
        super(message);
    }

    public ControlPlaneManagerException(Throwable throwable) {
        super(throwable);
    }
}
