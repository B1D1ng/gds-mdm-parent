package com.ebay.behavior.gds.mdm.udf.common.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractAuditable;
import com.ebay.behavior.gds.mdm.udf.common.model.enums.UdfStubLanguage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@Entity
@Table(name = "udf_stub_module")
public class UdfStubModule extends AbstractAuditable {

    @NotNull
    @Column(name = "udf_module_id")
    private Long udfModuleId;

    @NotNull
    @Column(name = "udf_stub_module_name")
    private String udfStubModuleName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "platform")
    private UdfStubLanguage platform;

    @NotNull
    @Column(name = "git_branch")
    private String gitBranch;

    @NotNull
    @Column(name = "git_commit")
    private String gitCommit;

    @NotNull
    @Column(name = "version")
    private String version;

    @NotNull
    @Column(name = "snapshot")
    private String snapshot;
}
