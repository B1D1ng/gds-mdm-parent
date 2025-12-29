package com.ebay.behavior.gds.mdm.contract.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractAuditable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@Entity
@ToString(callSuper = true)
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "component")
public class Component extends AbstractAuditable {
    @NotBlank
    @Column(name = NAME)
    private String name;

    @NotBlank
    @Column(name = "owners")
    private String owners;

    @Column(name = "entity_type")
    private String entityType;

    @NotBlank
    @Column(name = "dl")
    private String dl;

    @NotBlank
    @Column(name = "description")
    private String description;

    @NotBlank
    @Column(name = "type")
    private String type;
}