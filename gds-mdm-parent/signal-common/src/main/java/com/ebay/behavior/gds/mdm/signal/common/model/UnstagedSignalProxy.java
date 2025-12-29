package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.Environment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Value;

@Value
public class UnstagedSignalProxy {

    @NotNull
    Boolean isUnstaged;

    @PositiveOrZero
    Long planId;

    String planName;

    @NotNull
    @Positive
    Integer version;

    @NotNull
    Environment environment;
}
