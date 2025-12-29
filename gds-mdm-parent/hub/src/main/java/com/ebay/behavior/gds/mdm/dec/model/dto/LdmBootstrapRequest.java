package com.ebay.behavior.gds.mdm.dec.model.dto;

import jakarta.validation.constraints.NotNull;

public record LdmBootstrapRequest(@NotNull Long fromViewId, @NotNull Long toViewId) {
}
