package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractModel;
import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.datatype.PropertyV1;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@MappedSuperclass
public class SojTag extends AbstractModel {

    @NotBlank
    @Column(name = "soj_name")
    private String sojName;

    @NotBlank
    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @NotBlank
    @Column(name = "data_type")
    private String dataType;

    @NotBlank
    @Column(name = "schema_path")
    private String schemaPath;

    protected SojTag(PropertyV1 tag) {
        this.sojName = tag.getSojName();
        this.name = tag.getName();
        this.description = tag.getDescription();
        this.dataType = tag.getDataType();
        this.schemaPath = String.format("event.eventPayload.eventProperties[\"%s\"]", tag.getSojName());
    }
}
