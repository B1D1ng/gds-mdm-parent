package com.ebay.behavior.gds.mdm.signal.model.manyToMany;

import com.ebay.behavior.gds.mdm.common.model.WithId;
import com.ebay.behavior.gds.mdm.signal.common.model.SojBusinessTag;
import com.ebay.behavior.gds.mdm.signal.common.model.SojEvent;

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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode
@Entity
@Table(name = "soj_event_tag_map")
public class SojEventTagMapping implements WithId {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @PositiveOrZero
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "soj_event_id", referencedColumnName = "id")
    private SojEvent sojEvent;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "soj_tag_id", referencedColumnName = "id")
    private SojBusinessTag sojTag;

    public SojEventTagMapping(SojEvent sojEvent, SojBusinessTag sojTag) {
        this.sojEvent = sojEvent;
        this.sojTag = sojTag;
    }
}
