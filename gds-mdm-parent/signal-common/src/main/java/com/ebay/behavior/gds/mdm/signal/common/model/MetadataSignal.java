package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractVersionedAuditable;
import com.ebay.behavior.gds.mdm.common.model.CompletionStatus;
import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;
import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;
import com.ebay.behavior.gds.mdm.common.serde.ObjectListToStringDeserializer;
import com.ebay.behavior.gds.mdm.signal.common.model.converter.UdcDataSourceTypeConverter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;

@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class MetadataSignal extends AbstractVersionedAuditable implements Signal, Metadata {

    @NotNull
    @PositiveOrZero
    @Column(name = "plan_id")
    private Long planId;

    @PositiveOrZero
    @Column(name = "signal_template_source_id")
    private Long signalTemplateSourceId;

    @PositiveOrZero
    @Column(name = "signal_source_id")
    private Long signalSourceId;

    @Positive
    @Column(name = "signal_source_version")
    private Integer signalSourceVersion;

    @NotBlank
    @DiffInclude
    @Column(name = NAME)
    private String name;

    @NotBlank
    @DiffInclude
    @Column(name = "description")
    private String description;

    @DiffInclude
    @Column(name = "domain")
    private String domain;

    @DiffInclude
    @Column(name = "owners")
    @JsonDeserialize(using = ObjectListToStringDeserializer.class)
    private String owners;

    @NotBlank
    @DiffInclude
    @Column(name = "type")
    private String type;

    @DiffInclude
    @Column(name = "retention_period")
    private Long retentionPeriod;

    @PositiveOrZero
    @Column(name = "platform_id")
    private Long platformId;

    @JsonIgnore
    @JsonSerialize(using = LazyObjectSerializer.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_id", insertable = false, updatable = false)
    private PlatformLookup platform;

    @DiffInclude
    @Column(name = "uuid_generator_type")
    private String uuidGeneratorType;

    @DiffInclude
    @Column(name = "uuid_generator_expression")
    private String uuidGeneratorExpression;

    @DiffInclude
    @Column(name = "correlation_id_expression")
    private String correlationIdExpression;

    @DiffInclude
    @Column(name = "need_accumulation")
    private Boolean needAccumulation;

    @DiffInclude
    @Column(name = "ref_version")
    private Integer refVersion;

    @DiffInclude
    @Column(name = "legacyId")
    private String legacyId;

    @NotNull
    @DiffInclude
    @Column(name = "udc_data_source")
    @Convert(converter = UdcDataSourceTypeConverter.class)
    private UdcDataSourceType dataSource;

    @NotNull
    @Builder.Default
    @DiffInclude
    @Enumerated(EnumType.STRING)
    @Column(name = "completion_status")
    private CompletionStatus completionStatus = CompletionStatus.COMPLETED;

    // UDC elasticsearch API returns a list of UDC data sources
    @Transient
    private Set<UdcDataSourceType> sources;

    @Override
    public Long getParentId() {
        return planId;
    }

    @JsonIgnore
    // Named "assign" (instead of "set") in order to prevent this setter to be recognized as a property setter by spring BeanWrapper
    public void assignTemplateSource(VersionedId signalId) {
        this.signalTemplateSourceId = signalId.getId();
    }

    @JsonIgnore
    // Named "assign" (instead of "set") in order to prevent this setter to be recognized as a property setter by spring BeanWrapper
    public void assignUnstagedSource(VersionedId signalId) {
        this.signalSourceId = signalId.getId();
        this.signalSourceVersion = signalId.getVersion();
    }

    /**
     * Converts the UnstagedSignal to a Map representation.
     * getEvents() and getFields() must not return null.
     * If it does, it means UnstagedSignal sourced not by getByIdWithAssociationsRecursive() method, that is wrong by design.
     * The code must fail fast in this case. And so there shouldn't be a null check for getEvents() and getFields().
     * The map keys must follow the UDC field schema.
     */
    @Override
    public Map<String, Object> toMetadataMap(ObjectMapper objectMapper) {
        val events = getEvents();
        val fields = getFields();
        validateAssociationsNotNull(events, fields);

        val eventEntities = events.stream()
                .map(event -> ((Metadata) event).toMetadataMap(objectMapper))
                .toList();

        val fieldEntities = fields.stream()
                .map(field -> ((Metadata) field).toMetadataMap(objectMapper))
                .toList();

        val dst = toMap(objectMapper, this);
        dst.put("SignalName", getName());
        dst.put("events", eventEntities);
        dst.put("fields", fieldEntities);
        computeTimestamps(dst, this);

        dst.remove("VersionedId");
        dst.remove("dataSource");
        //TODO: remove this when UDC schema is updated
        dst.remove("refVersion");

        dst.values().removeIf(Objects::isNull);
        return dst;
    }

    @Override
    public UdcEntityType getEntityType() {
        return UdcEntityType.SIGNAL;
    }
}
