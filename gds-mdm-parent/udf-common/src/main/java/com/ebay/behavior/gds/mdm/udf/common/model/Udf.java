package com.ebay.behavior.gds.mdm.udf.common.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractAuditable;
import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;
import com.ebay.behavior.gds.mdm.udf.common.model.enums.FunctionSourceType;
import com.ebay.behavior.gds.mdm.udf.common.model.enums.Language;
import com.ebay.behavior.gds.mdm.udf.common.model.enums.UdfType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static jakarta.persistence.FetchType.LAZY;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@ToString(exclude = {"udfVersions", "udfStubs", "udfUsages"})
@EqualsAndHashCode(callSuper = true, exclude = {"udfVersions", "udfStubs", "udfUsages"})
@Entity
@Table(name = "udf")
public class Udf extends AbstractAuditable {

    @NotBlank
    @Column(name = "name")
    private String name;

    @NotBlank
    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "language")
    private Language language;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private UdfType type;

    @NotBlank
    @Column(name = "code")
    private String code;

    @NotBlank
    @Column(name = "parameters")
    private String parameters;

    @NotBlank
    @Column(name = "domain")
    private String domain;

    @NotBlank
    @Column(name = "owners")
    private String owners;

    @Column(name = "current_version_id")
    private Long currentVersionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "function_source_type")
    private FunctionSourceType functionSourceType;

    @JsonManagedReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @OneToMany(fetch = LAZY, mappedBy = "udf")
    private Set<UdfVersions> udfVersions;

    @JsonManagedReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @OneToMany(fetch = LAZY, mappedBy = "udf")
    private Set<UdfStub> udfStubs;

    @JsonManagedReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @OneToMany(fetch = LAZY, mappedBy = "udf")
    private Set<UdfUsage> udfUsages;

    @JsonIgnore
    public List<String> getOwnersAsList() {
        if (owners == null) {
            return List.of();
        }

        return Arrays.asList(owners.split(COMMA));
    }

    @JsonIgnore
    public Udf withId(Long id) {
        return this.toBuilder().id(id).build();
    }
}
