package com.ebay.behavior.gds.mdm.contract.model.manyToMany;

import com.ebay.behavior.gds.mdm.contract.model.Component;
import com.ebay.behavior.gds.mdm.contract.model.Routing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@Entity
@Table(name = "routing_component_mapping")
public class RoutingComponentMapping {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @PositiveOrZero
    private Long id;

    @ManyToOne
    @JoinColumn(name = "routing_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Routing routing;

    @NotNull
    @Column(name = "routing_id")
    private Long routingId;

    @ManyToOne
    @JoinColumn(name = "component_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Component component;

    @NotNull
    @Column(name = "component_id")
    private Long componentId;

    @Column(name = "order_index")
    private Integer orderIndex;

    public RoutingComponentMapping(Long routingId, Long componentId, int orderIndex) {
        this.routingId = routingId;
        this.componentId = componentId;
        this.orderIndex = orderIndex;
    }
}