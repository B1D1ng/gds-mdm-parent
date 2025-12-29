package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractAuditable;
import com.ebay.behavior.gds.mdm.common.model.ExpressionType;
import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.common.model.SurfaceType;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;

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

import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;

@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class MetadataEvent extends AbstractAuditable implements Metadata, Event {

    @PositiveOrZero
    @Column(name = "event_template_source_id")
    private Long eventTemplateSourceId;

    @PositiveOrZero
    @Column(name = "event_source_id")
    private Long eventSourceId;

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
    @Column(name = "type")
    private String type;

    @NotNull
    @DiffInclude
    @Column(name = "source")
    private EventSource source;

    @NotNull
    @Builder.Default
    @DiffInclude
    @Column(name = "fsm_order")
    private Integer fsmOrder = 99_999;

    @NotNull
    @Builder.Default
    @PositiveOrZero
    @DiffInclude
    @Column(name = "cardinality")
    private Integer cardinality = 1;

    @DiffInclude
    @Column(name = "github_repository_url")
    private String githubRepositoryUrl;

    @DiffInclude
    @Column(name = "surface_type")
    private SurfaceType surfaceType;

    @DiffInclude
    @Column(name = "expression")
    private String expression;

    @NotNull
    @DiffInclude
    @Enumerated(EnumType.STRING)
    @Column(name = "expression_type")
    private ExpressionType expressionType;

    // UDC elasticsearch API returns a list of UDC data sources
    @Transient
    private Set<UdcDataSourceType> sources;

    /**
     * Converts the UnstagedEvent to a Map representation.
     * getAttributes() must not return null.
     * If it does, it means UnstagedEvent sourced not by getByIdWithAssociationsRecursive() method, that is wrong by design.
     * The code must fail fast in this case. And so there shouldn't be a null check for getAttributes().
     * The map keys must follow the UDC event schema.
     */
    @Override
    public Map<String, Object> toMetadataMap(ObjectMapper objectMapper) {
        val attributes = getAttributes();
        val pageIds = getPageIds();
        val moduleIds = getModuleIds();
        val clickIds = getClickIds();
        validateAssociationsNotNull(attributes, pageIds, moduleIds, clickIds);

        val attributeEntities = attributes.stream()
                .map(attribute -> ((Metadata) attribute).toMetadataMap(objectMapper))
                .toList();

        val dst = toMap(objectMapper, this);
        dst.put("unifiedEventName", getName());
        dst.put("pageIds", pageIds);
        dst.put("moduleIds", moduleIds);
        dst.put("clickIds", clickIds);
        dst.put("attributes", attributeEntities);
        computeTimestamps(dst, this);

        dst.values().removeIf(Objects::isNull);
        return dst;
    }

    public abstract void setPageIds(Set<Long> pages);

    public abstract void setModuleIds(Set<Long> modules);

    public abstract void setClickIds(Set<Long> clicks);

    public abstract Set<Long> getPageIds();

    public abstract Set<Long> getModuleIds();

    public abstract Set<Long> getClickIds();

    @Override
    public UdcEntityType getEntityType() {
        return UdcEntityType.EVENT;
    }
}