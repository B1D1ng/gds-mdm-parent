package com.ebay.behavior.gds.mdm.contract.model.manyToMany;

import com.ebay.behavior.gds.mdm.contract.model.UnstagedContract;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@Entity
@Table(name = "contract_signal_mapping")
public class ContractSignalMapping {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @PositiveOrZero
    private Long id;

    @ManyToOne
    @JoinColumns({
            @JoinColumn(name = "contract_id", referencedColumnName = ID),
            @JoinColumn(name = "contract_version", referencedColumnName = "version")
    })
    private UnstagedContract contract;

    @ManyToOne
    @JoinColumns({
            @JoinColumn(name = "signal_id", referencedColumnName = ID),
            @JoinColumn(name = "signal_version", referencedColumnName = "version")
    })
    private UnstagedSignal signal;

    public ContractSignalMapping(UnstagedContract contract, UnstagedSignal signal) {
        this.contract = contract;
        this.signal = signal;
    }
}