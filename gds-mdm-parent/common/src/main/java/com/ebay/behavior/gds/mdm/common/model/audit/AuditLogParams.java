package com.ebay.behavior.gds.mdm.common.model.audit;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;

@Getter
public final class AuditLogParams {

    public static final String MODE = "mode";

    @NotNull
    @PositiveOrZero
    private final Long id;

    @Positive
    private final Integer version;

    @NotNull
    private final AuditMode mode;

    private AuditLogParams(Long id, Integer version, AuditMode mode) {
        this.id = id;
        this.version = version;
        this.mode = mode;
    }

    public static AuditLogParams ofNonVersioned(Long id, AuditMode mode) {
        return new AuditLogParams(id, null, mode);
    }

    public static AuditLogParams ofVersioned(Long id, Integer version, AuditMode mode) {
        return new AuditLogParams(id, version, mode);
    }

    public boolean isVersioned() {
        return version != null;
    }

    public boolean isNonVersioned() {
        return !isVersioned();
    }
}
