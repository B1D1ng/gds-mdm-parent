package com.ebay.behavior.gds.mdm.dec.model;

import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;
import com.ebay.behavior.gds.mdm.dec.util.StringListConverter;
import com.ebay.behavior.gds.mdm.dec.util.StringObjectConverter;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.javers.core.metamodel.annotation.DiffInclude;

import java.util.List;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.FIELD_GROUP_DATA_TYPE;

@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@ToString(exclude = {"signalMapping", "physicalStorageMapping"})
@EqualsAndHashCode(
        callSuper = true,
        exclude = {"signalMapping", "physicalStorageMapping"})
@Table(name = "dec_ldm_field")
public class LdmField extends DecAuditable {

    @Column(name = "ldm_entity_id")
    private Long ldmEntityId;

    @Column(name = "ldm_version")
    private Integer ldmVersion;

    @NotNull
    @DiffInclude
    @Column(name = "name")
    private String name;

    @DiffInclude
    @Column(name = "hierarchical_name")
    private String hierarchicalName;

    @DiffInclude
    @Column(name = "description")
    private String description;

    @DiffInclude
    @Column(name = "data_type")
    private String dataType;

    @DiffInclude
    @Column(name = "data_schema")
    @Convert(converter = StringObjectConverter.class)
    private Object dataSchema;

    @DiffInclude
    @Column(name = "value_function")
    private String valueFunction;

    @DiffInclude
    @Column(name = "signal_filter")
    @Convert(converter = StringListConverter.class)
    private List<String> signalFilter;

    @DiffInclude
    @Column(name = "is_derived_field")
    private Boolean isDerivedField;

    @DiffInclude
    @Column(name = "derived_field_expression")
    private String derivedFieldExpression;

    @DiffInclude
    @Column(name = "ordinal")
    private Integer ordinal;

    @DiffInclude
    @Column(name = "value_function_online")
    private String valueFunctionOnline;

    @DiffInclude
    @Column(name = "value_function_offline")
    private String valueFunctionOffline;

    @JsonBackReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(
                    name = "ldm_entity_id",
                    referencedColumnName = "id",
                    insertable = false,
                    updatable = false),
            @JoinColumn(
                    name = "ldm_version",
                    referencedColumnName = "version",
                    insertable = false,
                    updatable = false)
    })
    private LdmEntity entity;

    @DiffInclude
    @JsonManagedReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "field")
    private Set<LdmFieldSignalMapping> signalMapping;

    @DiffInclude
    @JsonManagedReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "field")
    private Set<LdmFieldPhysicalStorageMapping> physicalStorageMapping;

    @JsonIgnore
    public String getFieldGroup() {
        if (!FIELD_GROUP_DATA_TYPE.equalsIgnoreCase(this.dataType)) {
            String[] parts = this.name.split("\\.");
            if (parts.length > 1) {
                return String.join(".", List.of(parts).subList(0, parts.length - 1));
            } else {
                return null;
            }
        }
        return this.name;
    }

    @JsonIgnore
    public String getFieldName() {
        if (!FIELD_GROUP_DATA_TYPE.equalsIgnoreCase(this.dataType)) {
            // return the last part of the field name (<field_group>.<field_name>)
            String[] parts = this.name.split("\\.");
            if (parts.length > 0) {
                return parts[parts.length - 1];
            }
        }
        return this.name;
    }
}
