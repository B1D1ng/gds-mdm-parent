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

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@Entity
@Table(name = "event_click_map")
public class UnstagedEventClickMapping implements WithId {

    @Id
    @Column(name = ID)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @PositiveOrZero
    private Long id;

    @NotNull
    @Column(name = "event_id")
    private Long eventId;

    @NotNull
    @Column(name = "click_id")
    private Long clickId;

    public UnstagedEventClickMapping(Long eventId, Long clickId) {
        this.eventId = eventId;
        this.clickId = clickId;
    }
}