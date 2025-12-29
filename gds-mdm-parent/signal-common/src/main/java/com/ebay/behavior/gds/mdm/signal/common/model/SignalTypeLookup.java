package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractLookup;
import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Data
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Entity
@Table(name = "signal_type_lookup")
public class SignalTypeLookup extends AbstractLookup {

    @PositiveOrZero
    @Column(name = "platform_id")
    private Long platformId;

    @JsonIgnore
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonSerialize(using = LazyObjectSerializer.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_id", insertable = false, updatable = false)
    private PlatformLookup platform;

    @NotBlank
    @Column(name = "logical_data_entity")
    private String logicalDataEntity;

    @JsonSerialize(using = LazyObjectSerializer.class)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "signal_type_physical_storage_map",
            joinColumns = @JoinColumn(name = "signal_type_id", referencedColumnName = ID, insertable = false, updatable = false),
            inverseJoinColumns = @JoinColumn(name = "physical_storage_id"))
    private Set<SignalPhysicalStorage> physicalStorages;
}
