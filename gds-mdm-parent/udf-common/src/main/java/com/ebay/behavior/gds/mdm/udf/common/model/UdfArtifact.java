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

import java.sql.Timestamp;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@Entity
@Table(name = "udf_artifact")
public class UdfArtifact extends AbstractAuditable {

    @NotNull
    @Column(name = "udf_module_id")
    private Long udfModuleId;

    @NotNull
    @Column(name = "version")
    private String version;

    @NotNull
    @Column(name = "build_time")
    private Timestamp buildTime;

    @NotNull
    @Column(name = "is_latest")
    private Boolean isLatest;
}
