package com.ebay.behavior.gds.mdm.udf.common.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractAuditable;
import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;
import com.ebay.behavior.gds.mdm.udf.common.model.enums.FunctionSourceType;
import com.ebay.behavior.gds.mdm.udf.common.model.enums.Language;
import com.ebay.behavior.gds.mdm.udf.common.model.enums.UdfType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public abstract class MetadataUdf extends AbstractAuditable implements Metadata {
    @NotNull
    @PositiveOrZero
    private Long udfId;

    @NotBlank
    private String udfName;

    private String description;

    private Language language;

    private UdfType type;

    private String code;

    private String parameters;

    private String domain;

    private String owners;

    private FunctionSourceType functionSourceType;

    private Set<UdcDataSourceType> sources;

    @Override
    public Long getId() {
        return udfId;
    }

    @Override
    public UdcEntityType getEntityType() {
        return UdcEntityType.UDF;
    }
}
