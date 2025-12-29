package com.ebay.behavior.gds.mdm.udf.common.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractAuditable;
import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;
import com.ebay.behavior.gds.mdm.udf.common.model.enums.UdfStubLanguage;
import com.ebay.behavior.gds.mdm.udf.common.model.enums.UdfStubType;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.val;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@ToString(callSuper = true)
public abstract class MetadataUdfStub extends AbstractAuditable implements Metadata {
    @NotNull
    @PositiveOrZero
    private Long stubId;

    @NotBlank
    private String stubName;

    private String description;

    private UdfStubLanguage language;

    private String stubCode;

    private String stubParameters;

    private String stubRuntimeContext;

    private UdfStubType stubType;

    private Set<UdcDataSourceType> sources;

    @Override
    public Map<String, Object> toMetadataMap(ObjectMapper objectMapper) {
        val dst = toMap(objectMapper, this);
        dst.values().removeIf(Objects::isNull);
        return dst;
    }

    @Override
    public UdcEntityType getEntityType() {
        return UdcEntityType.UDF_STUB;
    }
}
