package com.ebay.behavior.gds.mdm.contract.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractAuditable;
import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.JoinTable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.javers.core.metamodel.annotation.DiffInclude;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "hive_config")
public class HiveConfig extends AbstractAuditable {

    @NotNull
    @PositiveOrZero
    @Column(name = "component_id")
    private Long componentId;

    @NotNull
    @DiffInclude
    @Enumerated(EnumType.STRING)
    @Column(name = "environment")
    private Environment env;

    @JsonBackReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_id", insertable = false, updatable = false)
    private Component component;

    @JsonSerialize(using = LazyObjectSerializer.class)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinTable(
            name = "config_storage_mapping",
            joinColumns = @JoinColumn(name = "config_id", referencedColumnName = ID),
            inverseJoinColumns = @JoinColumn(name = "storage_id", referencedColumnName = ID)
    )
    private HiveStorage hiveStorage;
}
