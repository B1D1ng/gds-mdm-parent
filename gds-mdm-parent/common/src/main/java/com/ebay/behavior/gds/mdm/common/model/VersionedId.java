package com.ebay.behavior.gds.mdm.common.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.val;
import org.apache.commons.lang3.Validate;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@ToString
public final class VersionedId implements Serializable {

    public static final int MIN_VERSION = 1;

    @NotNull
    @PositiveOrZero
    private Long id;

    @NotNull
    @Positive
    private Integer version;

    private VersionedId() { // used by hibernate framework
    }

    private VersionedId(Long id, Integer version) {
        Validate.isTrue(Objects.nonNull(id), "id cannot be null");
        Validate.isTrue(Objects.nonNull(version), "version cannot be null");
        this.id = id;
        this.version = version;
    }

    public static VersionedId of(long id, Integer version) {
        return new VersionedId(id, version);
    }

    public static VersionedId of(VersionedId versionedId) {
        return new VersionedId(versionedId.getId(), versionedId.getVersion());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        val pk = (VersionedId) obj;
        return Objects.equals(id, pk.id) && Objects.equals(version, pk.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version);
    }
}