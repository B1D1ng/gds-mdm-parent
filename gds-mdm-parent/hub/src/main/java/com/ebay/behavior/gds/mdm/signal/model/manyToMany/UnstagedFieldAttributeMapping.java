package com.ebay.behavior.gds.mdm.signal.model.manyToMany;

import com.ebay.behavior.gds.mdm.common.model.WithId;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedAttribute;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;

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
@Table(name = "field_attribute_map")
public class UnstagedFieldAttributeMapping implements WithId {

    @Id
    @Column(name = ID)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @PositiveOrZero
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "field_id", referencedColumnName = ID)
    private UnstagedField field;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "attribute_id", referencedColumnName = ID)
    private UnstagedAttribute attribute;

    public UnstagedFieldAttributeMapping(UnstagedField field, UnstagedAttribute attribute) {
        this.field = field;
        this.attribute = attribute;
    }
}
