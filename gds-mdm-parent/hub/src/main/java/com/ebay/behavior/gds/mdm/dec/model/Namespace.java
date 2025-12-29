package com.ebay.behavior.gds.mdm.dec.model;

import com.ebay.behavior.gds.mdm.dec.model.enums.NamespaceType;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.javers.core.metamodel.annotation.DiffInclude;

import java.util.List;

@Data
@Getter
@Setter
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@ToString
@EqualsAndHashCode(callSuper = true)
@Table(name = "dec_namespace")
public class Namespace extends DecAuditable {

    @NotNull
    @DiffInclude
    @Column(name = "name")
    private String name;

    @NotNull
    @DiffInclude
    @Column(name = "owners")
    private String owners;

    @NotNull
    @DiffInclude
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private NamespaceType type;

    @JsonIgnore
    public Namespace withId(Long id) {
        return this.toBuilder().id(id).build();
    }

    @JsonIgnore
    public Namespace withRevision(Integer revision) {
        return this.toBuilder().revision(revision).build();
    }

    @JsonIgnore
    public List<String> getOwnersAsList() {
        if (owners == null) {
            return List.of();
        }

        return toList(owners);
    }
}
