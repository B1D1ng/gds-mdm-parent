package com.ebay.behavior.gds.mdm.common.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

@Getter
public class VersionedIdWithStatus extends IdWithStatus {

    @NotNull
    @Positive
    private final Integer version;

    public VersionedIdWithStatus(long id, int version, int httpStatusCode, String message) {
        super(id, httpStatusCode, message);
        this.version = version;
    }

    public static VersionedIdWithStatus okStatus(VersionedId versionedId) {
        return new VersionedIdWithStatus(versionedId.getId(), versionedId.getVersion(), OK_VALUE, null);
    }

    public static VersionedIdWithStatus okStatus(long id, int version, String message) {
        return new VersionedIdWithStatus(id, version, OK_VALUE, message);
    }

    public static VersionedIdWithStatus failedStatus(long id, int version, String message) {
        return new VersionedIdWithStatus(id, version, INTERNAL_SERVER_ERROR_VALUE, message);
    }

    public static VersionedIdWithStatus failedStatus(VersionedId versionedId, String message) {
        return failedStatus(versionedId.getId(), versionedId.getVersion(), message);
    }

    public VersionedId toVersionedId() {
        return VersionedId.of(id, version);
    }
}