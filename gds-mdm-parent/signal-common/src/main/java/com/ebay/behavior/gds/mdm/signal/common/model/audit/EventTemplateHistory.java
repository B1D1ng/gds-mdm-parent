package com.ebay.behavior.gds.mdm.signal.common.model.audit;

import com.ebay.behavior.gds.mdm.common.model.ExpressionType;
import com.ebay.behavior.gds.mdm.common.model.SurfaceType;
import com.ebay.behavior.gds.mdm.common.model.audit.AbstractHistoryAuditable;
import com.ebay.behavior.gds.mdm.common.model.audit.ChangeType;
import com.ebay.behavior.gds.mdm.signal.common.model.EventSource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "event_template_history")
public class EventTemplateHistory extends AbstractHistoryAuditable {

    @NotBlank
    @Column(name = NAME)
    private String name;

    @NotBlank
    @Column(name = "description")
    private String description;

    @NotBlank
    @Column(name = "type")
    private String type;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "source")
    private EventSource source;

    @NotNull
    @Column(name = "fsm_order")
    private Integer fsmOrder;

    @NotNull
    @PositiveOrZero
    @Column(name = "cardinality")
    private Integer cardinality;

    @Enumerated(EnumType.STRING)
    @Column(name = "surface_type")
    private SurfaceType surfaceType;

    @Column(name = "expression")
    private String expression;

    @Enumerated(EnumType.STRING)
    @Column(name = "expression_type")
    private ExpressionType expressionType;

    @JsonIgnore
    public EventTemplateHistory withOriginalId(Long id) {
        return this.toBuilder().originalId(id).build();
    }

    @JsonIgnore
    public EventTemplateHistory withOriginalRevision(Integer revision) {
        return this.toBuilder().originalRevision(revision).build();
    }

    @JsonIgnore
    public EventTemplateHistory withType(ChangeType changeType) {
        return this.toBuilder().changeType(changeType).build();
    }
}
