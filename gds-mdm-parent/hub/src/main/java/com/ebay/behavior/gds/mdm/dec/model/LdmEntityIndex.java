package com.ebay.behavior.gds.mdm.dec.model;

import com.ebay.behavior.gds.mdm.dec.model.enums.ViewType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.javers.core.metamodel.annotation.DiffInclude;

@Data
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@ToString
@EqualsAndHashCode(callSuper = true)
@Table(name = "dec_ldm_entity_index")
public class LdmEntityIndex extends AbstractIndex {

    @NotNull
    @DiffInclude
    @Column(name = "base_entity_id")
    private Long baseEntityId;

    @NotNull
    @DiffInclude
    @Enumerated(EnumType.STRING)
    @Column(name = "view_type")
    private ViewType viewType;

    @DiffInclude
    @Column(name = "name")
    private String name;
}
