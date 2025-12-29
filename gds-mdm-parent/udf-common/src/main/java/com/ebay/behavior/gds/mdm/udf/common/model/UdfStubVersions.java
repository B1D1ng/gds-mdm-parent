package com.ebay.behavior.gds.mdm.udf.common.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractAuditable;
import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;
import com.ebay.behavior.gds.mdm.udf.common.model.enums.UdfStubType;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@ToString(exclude = {"udfStub"})
@EqualsAndHashCode(callSuper = true, exclude = {"udfStub"})
@Entity
@Table(name = "udf_stub_versions")
public class UdfStubVersions extends AbstractAuditable {

    @NotNull
    @PositiveOrZero
    @Column(name = "udf_stub_id")
    private Long udfStubId;

    @NotNull
    @Column(name = "stub_version")
    private Long stubVersion;

    @NotBlank
    @Column(name = "git_code_link")
    private String gitCodeLink;

    @NotBlank
    @Column(name = "stub_parameters")
    private String stubParameters;

    @NotBlank
    @Column(name = "stub_runtime_context")
    private String stubRuntimeContext;

    @Enumerated(EnumType.STRING)
    @Column(name = "stub_type")
    private UdfStubType stubType;

    @JsonBackReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "udf_stub_id", insertable = false, updatable = false)
    private UdfStub udfStub;

    @JsonIgnore
    public UdfStubVersions withId(Long id) {
        return this.toBuilder().id(id).build();
    }

    @JsonIgnore
    public static UdfStubVersions setupUdfStubVersions(UdfStub udfStub, Long udfVersion) {
        return UdfStubVersions.builder()
                .udfStubId(udfStub.getId())
                .stubVersion(udfVersion)
                .gitCodeLink(udfStub.getStubCode())
                .stubParameters(udfStub.getStubParameters())
                .stubRuntimeContext(udfStub.getStubRuntimeContext())
                .createBy(udfStub.getCreateBy())
                .updateBy(udfStub.getUpdateBy())
                .stubType(udfStub.getStubType())
                .build();
    }

    @JsonIgnore
    public static UdfStubVersions of(UdfStub udfStub, Long udfVersion) {
        return UdfStubVersions.builder()
                .id(udfStub.getCurrentVersionId())
                .udfStubId(udfStub.getId())
                .stubVersion(udfVersion)
                .gitCodeLink(udfStub.getStubCode())
                .stubParameters(udfStub.getStubParameters())
                .stubRuntimeContext(udfStub.getStubRuntimeContext())
                .createBy(udfStub.getCreateBy())
                .updateBy(udfStub.getUpdateBy())
                .stubType(udfStub.getStubType())
                .build();
    }
}
