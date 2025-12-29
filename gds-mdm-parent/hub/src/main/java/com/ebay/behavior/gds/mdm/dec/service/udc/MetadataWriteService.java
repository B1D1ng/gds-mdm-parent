package com.ebay.behavior.gds.mdm.dec.service.udc;

import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;
import com.ebay.behavior.gds.mdm.dec.model.Dataset;
import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalStorage;
import com.ebay.behavior.gds.mdm.dec.model.udc.ConsumableDataset;
import com.ebay.behavior.gds.mdm.dec.model.udc.LogicalDataModel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Map;
import java.util.Set;

public interface MetadataWriteService {

    String deleteLogicalDataModel(@PositiveOrZero long id);

    String upsertLogicalDataModel(@Valid @NotNull LdmBaseEntity ldm, @Valid @NotNull Namespace namespace,
                                  Set<Long> upstreamLdmIds, Set<Long> lastUpstreamLdmIds);

    Map<UdcEntityType, String> upsertLogicalDataModel(@Valid @NotNull LogicalDataModel model, Set<Long> upstreamLdmIds);

    String deleteConsumableDataset(@PositiveOrZero long id);

    Map<UdcEntityType, String> upsertConsumableDataset(@Valid @NotNull Dataset dataset, @Valid @NotNull Namespace namespace,
                                                       @NotNull Long ldmId, @NotEmpty String ldmName, PhysicalStorage storage, Long lastLdmId);

    Map<UdcEntityType, String> upsertConsumableDataset(@Valid @NotNull ConsumableDataset dataset);

    Map<UdcEntityType, String> upsertSignalToLdmLineage(@NotNull Long ldmId, Set<Long> upstreamSignalIds);

    String deleteLineage(@NotNull @Valid UdcEntityType entityType, @NotBlank String id);
}
