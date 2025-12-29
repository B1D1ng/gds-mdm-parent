package com.ebay.behavior.gds.mdm.dec.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
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
@EqualsAndHashCode(callSuper = true, exclude = {"views"})
@Table(name = "dec_ldm_base_entity")
public class LdmBaseEntity extends DecAuditable {

    @NotNull
    @DiffInclude
    @Column(name = "name")
    private String name;

    @NotNull
    @DiffInclude
    @Column(name = "namespace_id")
    private Long namespaceId;

    @DiffInclude
    @Column(name = "description")
    private String description;

    @DiffInclude
    @Column(name = "owners")
    private String owners;

    @DiffInclude
    @Column(name = "jira_project")
    private String jiraProject;

    @DiffInclude
    @Column(name = "domain")
    private String domain;

    @DiffInclude
    @Column(name = "pk")
    private String pk;

    @DiffInclude
    @Column(name = "team")
    private String team;

    @DiffInclude
    @Column(name = "team_dl")
    private String teamDl;

    @Transient
    private List<LdmEntity> views;

    @JsonIgnore
    public List<String> getOwnersAsList() {
        if (owners == null) {
            return List.of();
        }

        return toList(owners);
    }
}
