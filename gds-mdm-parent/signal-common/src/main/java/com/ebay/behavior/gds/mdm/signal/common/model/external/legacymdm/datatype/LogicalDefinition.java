package com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.datatype;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.javers.core.metamodel.annotation.DiffIgnore;

import java.util.List;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LogicalDefinition {

    @NotEmpty
    private List<EventClassifier> eventClassifiers;

    private List<Field> fields;

    private UuidGenerator uuidGenerator;

    @NotBlank
    private String platform;

    private String retentionPeriod;

    private List<String> correlationIdFormulas;

    @DiffIgnore
    private Boolean needAccumulation;
}