package com.ebay.behavior.gds.mdm.signal.model;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SignalChildId {

    @NotNull
    @PositiveOrZero
    private Long signalId;

    @NotNull
    @Positive
    private Integer signalVersion;

    @NotNull
    @PositiveOrZero
    private Long childId;

    public VersionedId getSignalVersionedId() {
        return VersionedId.of(signalId, signalVersion);
    }
}
