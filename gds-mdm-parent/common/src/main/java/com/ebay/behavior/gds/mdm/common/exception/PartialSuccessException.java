package com.ebay.behavior.gds.mdm.common.exception;

import com.ebay.behavior.gds.mdm.common.model.VersionedIdWithStatus;

import lombok.Getter;

import java.util.List;

@Getter
public class PartialSuccessException extends MdmException {

    private final List<VersionedIdWithStatus> statuses;

    public PartialSuccessException(String message, List<VersionedIdWithStatus> statuses) {
        super(message);
        this.statuses = statuses;
    }

    public PartialSuccessException(String message, Exception cause, List<VersionedIdWithStatus> statuses) {
        super(message, cause);
        this.statuses = statuses;
    }

    public PartialSuccessException(Exception cause, List<VersionedIdWithStatus> statuses) {
        super(cause);
        this.statuses = statuses;
    }
}