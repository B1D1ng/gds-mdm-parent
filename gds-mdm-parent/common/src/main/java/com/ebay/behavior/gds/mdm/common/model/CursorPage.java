package com.ebay.behavior.gds.mdm.common.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.util.List;

@Value
public class CursorPage<M> {

    @Valid
    @NotNull
    CursorPageable pageable;

    boolean hasMore;

    @NotNull
    List<@Valid M> entities;
}
