package com.ebay.behavior.gds.mdm.contract.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractAuditable;
import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Embedded;
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

import java.util.List;

import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@ToString(callSuper = true, exclude = {"componentChain", "unstagedContract"})
@EqualsAndHashCode(callSuper = true, exclude = {"componentChain", "unstagedContract"})
@Entity
@Table(name = "routing")
public class Routing extends AbstractAuditable {

    @NotNull
    @PositiveOrZero
    @Column(name = "contract_id")
    private Long contractId;

    @NotNull
    @Positive
    @Column(name = "contract_version")
    private Integer contractVersion;

    @NotBlank
    @Column(name = NAME)
    private String name;

    @JsonSerialize(using = LazyObjectSerializer.class)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "routing_component_mapping",
            joinColumns = @JoinColumn(name = "routing_id", referencedColumnName = ID, insertable = false, updatable = false),
            inverseJoinColumns = @JoinColumn(name = "component_id", referencedColumnName = ID, insertable = false, updatable = false)
    )
    @OrderColumn(name = "order_index")
    private List<Component> componentChain;

    @JsonBackReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "contract_id", insertable = false, updatable = false),
            @JoinColumn(name = "contract_version", insertable = false, updatable = false)
    })
    private UnstagedContract unstagedContract;

    @JsonSerialize(using = LazyObjectSerializer.class)
    @Embedded
    private Sla sla;

    @Override
    public Long getParentId() {
        return contractId;
    }
}