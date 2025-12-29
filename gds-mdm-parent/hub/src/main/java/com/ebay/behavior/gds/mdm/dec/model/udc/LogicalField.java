package com.ebay.behavior.gds.mdm.dec.model.udc;

import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;
import com.ebay.behavior.gds.mdm.dec.model.enums.ViewType;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

import java.util.List;
import java.util.Locale;
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
public class LogicalField implements Metadata {

    private Long id;

    @NotNull
    private String logicalFieldName;

    @NotNull
    private Long ldmId;

    private String description;

    private String dataType;

    private List<ViewType> views;

    private Set<UdcDataSourceType> sources;

    private String graphPK;

    @Override
    public Map<String, Object> toMetadataMap(ObjectMapper objectMapper) {
        Map<String, Object> dst = objectMapper.convertValue(this, MAP_TYPE_REF);
        dst.values().removeIf(Objects::isNull);
        dst.remove("id");
        return dst;
    }

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public UdcEntityType getEntityType() {
        return UdcEntityType.LDM_FIELD;
    }

    @JsonIgnore
    public String getCompositeId() {
        return String.join("_", String.valueOf(ldmId), logicalFieldName.trim().toUpperCase(Locale.US));
    }
}
