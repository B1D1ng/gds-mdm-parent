package com.ebay.behavior.gds.mdm.common.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.Validate;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
public final class RevisionedId implements Serializable {

    @NotNull
    @PositiveOrZero
    private Long id;

    @NotNull
    @PositiveOrZero
    private Integer revision;

    private RevisionedId() {
    }

    private RevisionedId(Long id, Integer revision) {
        Validate.isTrue(Objects.nonNull(id), "id cannot be null");
        Validate.isTrue(Objects.nonNull(revision), "revision cannot be null");
        this.id = id;
        this.revision = revision;
    }

    public static RevisionedId of(long id, Integer revision) {
        return new RevisionedId(id, revision);
    }
}