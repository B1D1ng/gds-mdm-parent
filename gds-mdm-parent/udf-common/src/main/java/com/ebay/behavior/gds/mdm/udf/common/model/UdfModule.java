package com.ebay.behavior.gds.mdm.udf.common.model;

import com.ebay.behavior.gds.mdm.common.model.AbstractAuditable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "udf_module")
public class UdfModule extends AbstractAuditable {

    @NotNull
    @Column(name = "module_name")
    private String moduleName;

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
