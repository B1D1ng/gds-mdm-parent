package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractAuditable;
import com.ebay.behavior.gds.mdm.common.model.ExpressionType;
import com.ebay.behavior.gds.mdm.common.model.JavaType;
import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;
import com.ebay.behavior.gds.mdm.common.serde.AvroSchemaDeserializer;
import com.ebay.behavior.gds.mdm.common.serde.AvroSchemaSerializer;
import com.ebay.behavior.gds.mdm.signal.common.model.converter.AvroSchemaConverter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.val;
import org.apache.avro.Schema;
import org.javers.core.metamodel.annotation.DiffInclude;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class MetadataField extends AbstractAuditable implements Metadata, Field {

    @NotNull
    @PositiveOrZero
    @Column(name = "signal_id")
    private Long signalId;

    @NotNull
    @Positive
    @Column(name = "signal_version")
    private Integer signalVersion;

    @NotBlank
    @DiffInclude
    @Column(name = NAME)
    private String name;

    @NotBlank
    @DiffInclude
    @Column(name = "description")
    private String description;

    @NotBlank
    @DiffInclude
    @Column(name = "tag")
    private String tag;

    @NotNull
    @DiffInclude
    @Enumerated(EnumType.STRING)
    @Column(name = "java_type")
    private JavaType javaType;

    @NotNull
    @DiffInclude
    @Column(name = "avro_schema")
    @Convert(converter = AvroSchemaConverter.class)
    @JsonSerialize(using = AvroSchemaSerializer.class)
    @JsonDeserialize(using = AvroSchemaDeserializer.class)
    private Schema avroSchema;

    @DiffInclude
    @Column(name = "expression")
    private String expression;

    @NotNull
    @DiffInclude
    @Enumerated(EnumType.STRING)
    @Column(name = "expression_type")
    private ExpressionType expressionType;

    @NotNull
    @DiffInclude
    @Column(name = "is_mandatory")
    private Boolean isMandatory;

    @DiffInclude
    @Column(name = "is_cached")
    private Boolean isCached;

    @NotBlank
    @DiffInclude
    @Column(name = "event_types")
    private String eventTypes;

    // UDC elasticsearch API returns a list of UDC data sources
    @Transient
    private Set<UdcDataSourceType> sources;

    @Override
    public Long getParentId() {
        return signalId;
    }

    /**
     * Converts the UnstagedField to a Map representation.
     * getAttributes() must not return null.
     * If it does, it means UnstagedField sourced not by getByIdWithAssociationsRecursive() method, that is wrong by design.
     * The code must fail fast in this case. And so there shouldn't be a null check for getAttributes().
     * The map keys must follow the UDC field schema.
     */
    @Override
    public Map<String, Object> toMetadataMap(ObjectMapper objectMapper) {
        val attributes = getAttributes();
        validateAssociationsNotNull(attributes);

        val attributeEntities = attributes.stream()
                .map(attribute -> ((Metadata) attribute).toMetadataMap(objectMapper))
                .toList();

        val dst = toMap(objectMapper, this);
        dst.remove("signal"); // signal can be found by signalId property
        dst.put("SignalFieldName", getName());
        dst.put("attributes", attributeEntities);
        computeTimestamps(dst, this);

        dst.values().removeIf(Objects::isNull);
        return dst;
    }

    @Override
    public UdcEntityType getEntityType() {
        return UdcEntityType.FIELD;
    }

    @JsonIgnore
    public String getGroupKey() {
        return this.tag + "_$_" + this.name;
    }
}
