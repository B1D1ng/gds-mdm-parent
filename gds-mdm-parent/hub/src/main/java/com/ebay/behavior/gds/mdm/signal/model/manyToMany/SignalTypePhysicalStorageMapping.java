package com.ebay.behavior.gds.mdm.signal.model.manyToMany;

import com.ebay.behavior.gds.mdm.common.model.WithId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@Entity
@Table(name = "signal_type_physical_storage_map")
public class SignalTypePhysicalStorageMapping implements WithId {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @PositiveOrZero
    private Long id;

    @NotNull
    @Column(name = "signal_type_id")
    private Long signalTypeId;

    @NotNull
    @Column(name = "physical_storage_id")
    private Long physicalStorageId;

    public SignalTypePhysicalStorageMapping(Long signalTypeId, Long physicalStorageId) {
        this.signalTypeId = signalTypeId;
        this.physicalStorageId = physicalStorageId;
    }
}
