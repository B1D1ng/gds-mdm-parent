package com.ebay.behavior.gds.mdm.dec.model.udc;

import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;
import com.ebay.behavior.gds.mdm.dec.model.enums.AccessType;
import com.ebay.behavior.gds.mdm.dec.model.enums.DatasetStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Getter
@Setter
public class ConsumableDataset implements Metadata {

    @NotNull
    private Long consumableDatasetId;

    @NotNull
    private String consumableDatasetName;

    @NotNull
    private Long ldmId;

    @NotNull
    private String ldmName;

    private List<String> owners;

    private DatasetStatus status;

    @NotNull
    private String namespace;

    private List<AccessDetail> accessDetails;

    private String createBy;

    private String updateBy;

    private Timestamp createDate;

    private Timestamp updateDate;

    private Set<UdcDataSourceType> sources;

    private String graphPK;

    private AccessType accessType;

    private DataStoreTableDetail storeTableDetail;

    @Override
    public Map<String, Object> toMetadataMap(ObjectMapper objectMapper) {
        Map<String, Object> dst = objectMapper.convertValue(this, MAP_TYPE_REF);
        dst.values().removeIf(Objects::isNull);
        dst.remove("id");
        dst.remove("accessType");
        dst.remove("storeTableDetail");
        dst.compute("createDate", (k, v) -> this.getCreateDate() != null ? this.getCreateDate().getTime() : null);
        dst.compute("updateDate", (k, v) -> this.getUpdateDate() != null ? this.getUpdateDate().getTime() : null);
        return dst;
    }

    @Override
    public Long getId() {
        return this.consumableDatasetId;
    }

    @Override
    public UdcEntityType getEntityType() {
        return UdcEntityType.DATASET;
    }
}
