package com.ebay.behavior.gds.mdm.dec.model;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;
import com.ebay.behavior.gds.mdm.dec.model.enums.DatasetStatus;
import com.ebay.behavior.gds.mdm.dec.model.enums.Environment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.javers.core.metamodel.annotation.DiffInclude;

import java.util.List;
import java.util.Set;

@Data
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@ToString
@EqualsAndHashCode(callSuper = true, exclude = {"deployments"})
@IdClass(VersionedId.class)
@Table(name = "dec_dataset")
public class Dataset extends DecVersionedAuditable {

    @Id
    @Column(name = "id")
    private Long id;

    @NotNull
    @DiffInclude
    @Column(name = "ldm_entity_id")
    private Long ldmEntityId;

    @NotNull
    @DiffInclude
    @Column(name = "ldm_version")
    private Integer ldmVersion;

    @NotNull
    @DiffInclude
    @Column(name = "name")
    private String name;

    @DiffInclude
    @Column(name = "owners")
    private String owners;

    @NotNull
    @DiffInclude
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DatasetStatus status;

    @DiffInclude
    @Column(name = "ir")
    private String ir;

    @DiffInclude
    @Column(name = "access_details")
    private String accessDetails;

    @DiffInclude
    @Column(name = "consumption_parameters")
    private String consumptionParameters;

    @DiffInclude
    @Column(name = "runtime_configurations")
    private String runtimeConfigurations;

    @DiffInclude
    @Column(name = "namespace_id")
    private Long namespaceId;

    @DiffInclude
    @Column(name = "is_dcs")
    private Boolean isDcs;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "environment")
    private Environment environment = Environment.UNSTAGED;

    @DiffInclude
    @JsonManagedReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "dataset")
    private Set<DatasetDeployment> deployments;

    @JsonIgnore
    public List<String> getOwnersAsList() {
        if (owners == null) {
            return List.of();
        }

        return toList(owners);
    }
}