package com.ebay.behavior.gds.mdm.udf.common.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractAuditable;
import com.ebay.behavior.gds.mdm.common.serde.LazyObjectSerializer;
import com.ebay.behavior.gds.mdm.udf.common.model.enums.FunctionSourceType;
import com.ebay.behavior.gds.mdm.udf.common.model.enums.UdfStatus;

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

import java.util.Arrays;
import java.util.List;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@ToString(exclude = {"udf"})
@EqualsAndHashCode(callSuper = true, exclude = {"udf"})
@Entity
@Table(name = "udf_versions")
public class UdfVersions extends AbstractAuditable {

    @NotNull
    @PositiveOrZero
    @Column(name = "udf_id")
    private Long udfId;

    @NotNull
    @Column(name = "version")
    private Long version;

    @NotBlank
    @Column(name = "git_code_link")
    private String gitCodeLink;

    @NotBlank
    @Column(name = "parameters")
    private String parameters;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private UdfStatus status;

    @NotBlank
    @Column(name = "domain")
    private String domain;

    @NotBlank
    @Column(name = "owners")
    private String owners;

    @Enumerated(EnumType.STRING)
    @Column(name = "function_source_type")
    private FunctionSourceType functionSourceType;

    @JsonBackReference
    @JsonSerialize(using = LazyObjectSerializer.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "udf_id", insertable = false, updatable = false)
    private Udf udf;

    @JsonIgnore
    public List<String> getOwnersAsList() {
        if (owners == null) {
            return List.of();
        }

        return Arrays.asList(owners.split(COMMA));
    }

    @JsonIgnore
    public UdfVersions withId(Long id) {
        return this.toBuilder().id(id).build();
    }

    @JsonIgnore
    public static UdfVersions setupUdfVersions(Udf udf, Long udfVersion) {
        return UdfVersions.builder()
                .udfId(udf.getId())
                .version(udfVersion)
                .gitCodeLink(udf.getCode())
                .parameters(udf.getParameters())
                .status(UdfStatus.CREATED)
                .domain(udf.getDomain())
                .owners(udf.getOwners())
                .createBy(udf.getCreateBy())
                .updateBy(udf.getUpdateBy())
                .functionSourceType(udf.getFunctionSourceType())
                .build();
    }

    @JsonIgnore
    public static UdfVersions of(Udf udf, Long udfVersion) {
        return UdfVersions.builder()
                .id(udf.getCurrentVersionId())
                .udfId(udf.getId())
                .version(udfVersion)
                .gitCodeLink(udf.getCode())
                .parameters(udf.getParameters())
                .status(UdfStatus.CREATED)
                .domain(udf.getDomain())
                .owners(udf.getOwners())
                .createBy(udf.getCreateBy())
                .updateBy(udf.getUpdateBy())
                .functionSourceType(udf.getFunctionSourceType())
                .build();
    }
}
