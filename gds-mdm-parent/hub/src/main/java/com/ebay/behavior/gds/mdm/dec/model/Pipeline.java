package com.ebay.behavior.gds.mdm.dec.model;

import com.ebay.behavior.gds.mdm.dec.util.StringListConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.javers.core.metamodel.annotation.DiffInclude;

import java.util.List;

@Data
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@ToString
@EqualsAndHashCode(callSuper = true)
@Table(name = "dec_pipeline")
public class Pipeline extends DecAuditable {

    @NotNull
    @DiffInclude
    @Column(name = "pipeline_id")
    private String pipelineId;

    @DiffInclude
    @Column(name = "workspace_id")
    private String workspaceId;

    @DiffInclude
    @Column(name = "name")
    private String name;

    @DiffInclude
    @Column(name = "code")
    private String code;

    @DiffInclude
    @Column(name = "tasks")
    @Convert(converter = StringListConverter.class)
    private List<String> tasks;
}
