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

import java.sql.Timestamp;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder(toBuilder = true)
@Entity
@Table(name = "udf_stub_artifact")
public class UdfStubArtifact extends AbstractAuditable {

    @Column(name = "udf_stub_module_id")
    private Long udfStubModuleId;

    @NotNull
    @Column(name = "udf_stub_id")
    private Long udfStubId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "platform")
    private UdfStubLanguage platform;

    @NotNull
    @Column(name = "uri")
    private String uri;

    @NotNull
    @Column(name = "version")
    private String version;

    @Column(name = "build_time")
    private Timestamp buildTime;

    @Column(name = "deploy_time")
    private Timestamp deployTime;

    @NotNull
    @Column(name = "is_latest")
    private Boolean isLatest;
}
