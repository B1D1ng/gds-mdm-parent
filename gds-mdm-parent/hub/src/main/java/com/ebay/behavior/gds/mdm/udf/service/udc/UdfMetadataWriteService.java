package com.ebay.behavior.gds.mdm.udf.service.udc;

import com.ebay.behavior.gds.mdm.udf.common.model.UdcUdf;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface UdfMetadataWriteService {
    String upsertUdf(@Valid @NotNull UdcUdf udf);

    String deleteUdf(@NotBlank String id);
}
