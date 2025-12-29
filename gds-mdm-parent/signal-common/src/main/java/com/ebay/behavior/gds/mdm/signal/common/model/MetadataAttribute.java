package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractAuditable;
import com.ebay.behavior.gds.mdm.common.model.JavaType;
import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.val;
import org.javers.core.metamodel.annotation.DiffInclude;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class MetadataAttribute extends AbstractAuditable implements Metadata, Attribute {

    @NotNull
    @PositiveOrZero
    @Column(name = "event_id")
    private Long eventId;

    @NotBlank
    @DiffInclude
    @Column(name = "tag")
    private String tag;

    @DiffInclude
    @Column(name = "description")
    private String description;

    @NotNull
    @DiffInclude
    @Enumerated(EnumType.STRING)
    @Column(name = "java_type")
    private JavaType javaType;

    @NotBlank
    @DiffInclude
    @Column(name = "schema_path")
    private String schemaPath;

    @Builder.Default
    @DiffInclude
    @Column(name = "is_store_in_state")
    private Boolean isStoreInState = false;

    // UDC elasticsearch API returns a list of UDC data sources
    @Transient
    private Set<UdcDataSourceType> sources;

    @Override
    @JsonIgnore
    public Long getParentId() {
        return eventId;
    }

    /**
     * Converts the UnstagedAttribute to a Map representation.
     * The map keys must follow the UDC attribute schema.
     */
    @Override
    public Map<String, Object> toMetadataMap(ObjectMapper objectMapper) {
        val dst = toMap(objectMapper, this);
        dst.put("unifiedEventAttributeName", getTag());
        dst.remove("event"); // event can be found by eventId property
        computeTimestamps(dst, this);

        dst.values().removeIf(Objects::isNull);
        return dst;
    }

    @Override
    public UdcEntityType getEntityType() {
        return UdcEntityType.ATTRIBUTE;
    }
}