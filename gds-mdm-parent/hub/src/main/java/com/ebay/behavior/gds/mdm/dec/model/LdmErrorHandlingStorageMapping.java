package com.ebay.behavior.gds.mdm.dec.model;

import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
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
import org.javers.core.metamodel.annotation.DiffInclude;

/**
 * Entity representing the error-handling physical storage mapping for LDM materialization.
 */
@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@ToString
@EqualsAndHashCode(callSuper = true)
@Table(name = "dec_ldm_error_handling_storage_mapping")
public class LdmErrorHandlingStorageMapping extends DecAuditable {

    @NotNull
    @DiffInclude
    @Column(name = "physical_storage_id")
    private Long physicalStorageId;

    @NotNull
    @Column(name = "ldm_entity_id")
    private Long ldmEntityId;

    @NotNull
    @Column(name = "ldm_version")
    private Integer ldmVersion;

    @JsonBackReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(
                    name = "ldm_entity_id",
                    referencedColumnName = "id",
                    insertable = false,
                    updatable = false),
            @JoinColumn(
                    name = "ldm_version",
                    referencedColumnName = "version",
                    insertable = false,
                    updatable = false)
    })
    private LdmEntity entity;
}
