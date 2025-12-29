package com.ebay.behavior.gds.mdm.signal.model.manyToMany;

import com.ebay.behavior.gds.mdm.common.model.WithId;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.model.VersionedModel.VERSION;

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@Entity
@Table(name = "signal_event_map")
public class UnstagedSignalEventMapping implements WithId {

    @Id
    @Column(name = ID)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @PositiveOrZero
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumns({
            @JoinColumn(name = "signal_id", referencedColumnName = ID),
            @JoinColumn(name = "signal_version", referencedColumnName = VERSION)
    })
    private UnstagedSignal signal;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "event_id", referencedColumnName = ID)
    private UnstagedEvent event;

    public UnstagedSignalEventMapping(UnstagedSignal signal, UnstagedEvent event) {
        this.signal = signal;
        this.event = event;
    }
}