package com.ebay.behavior.gds.mdm.udf.common.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractAuditable;
import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;
import com.ebay.behavior.gds.mdm.udf.common.model.enums.UdfStubLanguage;
import com.ebay.behavior.gds.mdm.udf.common.model.enums.UdfStubType;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
@ToString(exclude = {"udf", "udfStubVersions"})
@EqualsAndHashCode(callSuper = true, exclude = {"udf", "udfStubVersions"})
@Entity
@Table(name = "udf_stub")
public class UdfStub extends AbstractAuditable {

    @NotNull
    @PositiveOrZero
    @Column(name = "udf_id")
    private Long udfId;

    @NotNull
    @Column(name = "stub_name")
    private String stubName;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "language")
    private UdfStubLanguage language;

    @NotNull
    @Column(name = "stub_code")
    private String stubCode;

    @Column(name = "stub_parameters")
    private String stubParameters;

    @Column(name = "stub_runtime_context")
    private String stubRuntimeContext;

    @Column(name = "owners")
    private String owners;

    @Column(name = "current_version_id")
    private Long currentVersionId;

    @Column(name = "current_udf_version_id")
    private Long currentUdfVersionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "stub_type")
    private UdfStubType stubType;

    @JsonBackReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "udf_id", insertable = false, updatable = false)
    private Udf udf;

    @JsonManagedReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @OneToMany(fetch = LAZY, mappedBy = "udfStub")
    private Set<UdfStubVersions> udfStubVersions;

    @JsonIgnore
    public List<String> getOwnersAsList() {
        if (owners == null) {
            return List.of();
        }

        return Arrays.asList(owners.split(COMMA));
    }

    @JsonIgnore
    public UdfStub withId(Long id) {
        return this.toBuilder().id(id).build();
    }
}
