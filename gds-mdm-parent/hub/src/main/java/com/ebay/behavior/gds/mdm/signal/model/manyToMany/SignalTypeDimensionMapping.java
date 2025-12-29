package com.ebay.behavior.gds.mdm.signal.model.manyToMany;

import com.ebay.behavior.gds.mdm.common.model.WithId;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalDimTypeLookup;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalTypeLookup;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@Entity
@Table(name = "signal_dim_type_map")
public class SignalTypeDimensionMapping implements WithId {

    @Id
    @Column(name = ID)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @PositiveOrZero
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "signal_type_id", referencedColumnName = ID)
    private SignalTypeLookup signalType;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "dimension_id", referencedColumnName = ID)
    private SignalDimTypeLookup dimension;

    @NotNull
    @Column(name = "is_mandatory")
    private Boolean isMandatory;

    public SignalTypeDimensionMapping(SignalTypeLookup signalType, SignalDimTypeLookup dimension, Boolean isMandatory) {
        this.signalType = signalType;
        this.dimension = dimension;
        this.isMandatory = isMandatory;
    }
}
