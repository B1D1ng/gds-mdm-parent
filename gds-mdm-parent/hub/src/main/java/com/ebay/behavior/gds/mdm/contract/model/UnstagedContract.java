package com.ebay.behavior.gds.mdm.contract.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractVersionedAuditable;
import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.javers.core.metamodel.annotation.DiffInclude;

import java.util.List;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;
import static org.hibernate.id.OptimizableGenerator.INCREMENT_PARAM;
import static org.hibernate.id.enhanced.TableGenerator.SEGMENT_VALUE_PARAM;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@Entity
@ToString(callSuper = true, exclude = {"routings", "pipelines"})
@EqualsAndHashCode(callSuper = true, exclude = {"routings", "pipelines"})
@IdClass(VersionedId.class)
@Table(name = "unstaged_contract")
public class UnstagedContract extends AbstractVersionedAuditable {
    // The ID column uses custom table generator since GenerationType.IDENTITY is not supported by hibernate for composite keys
    // and GenerationType.SEQUENCE is not supported by mySql
    // Since it must use the custom table generator, it cannot be placed in the superclass
    @Id
    @Column(name = ID)
    @PositiveOrZero
    @DiffInclude
    @GeneratedValue(generator = "custom-table-generator")
    @GenericGenerator(
            name = "custom-table-generator",
            strategy = "com.ebay.behavior.gds.mdm.common.service.CustomTableGenerator",
            parameters = {
                    @Parameter(name = SEGMENT_VALUE_PARAM, value = "contract_seq"),
                    @Parameter(name = INCREMENT_PARAM, value = "5")
            })
    private Long id;

    @NotBlank
    @DiffInclude
    @Column(name = NAME)
    private String name;

    @NotBlank
    @DiffInclude
    @Column(name = "owners")
    private String owners;

    @NotBlank
    @DiffInclude
    @Column(name = "dl")
    private String dl;

    @NotBlank
    @DiffInclude
    @Column(name = "description")
    private String description;

    @NotBlank
    @DiffInclude
    @Column(name = "domain")
    private String domain;

    @NotNull
    @DiffInclude
    @Enumerated(EnumType.STRING)
    private ContractStatus status;

    @NotNull
    @DiffInclude
    @Enumerated(EnumType.STRING)
    @Column(name = "environment")
    private Environment environment = Environment.UNSTAGED;

    @JsonManagedReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "unstagedContract")
    private Set<Routing> routings;

    @JsonManagedReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "unstagedContract")
    private Set<ContractPipeline> pipelines;

    @Column(name = "comment")
    private String comment;

    @Transient
    private List<ContractUserAction> operations;

    @JsonIgnore
    public List<String> getOwnersAsList() {
        if (owners == null) {
            return List.of();
        }

        return toList(owners);
    }
}
