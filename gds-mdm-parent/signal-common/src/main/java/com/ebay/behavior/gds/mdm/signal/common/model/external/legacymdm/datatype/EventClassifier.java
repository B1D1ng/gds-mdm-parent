package com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.datatype;

import com.ebay.behavior.gds.mdm.signal.common.model.JaversIdentity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.javers.core.metamodel.annotation.DiffIgnore;

import static com.ebay.behavior.gds.mdm.common.util.ServiceConstants.UNDERSCORE;
import static org.apache.commons.lang3.StringUtils.defaultString;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventClassifier implements JaversIdentity {

    @NotBlank
    private String type;

    @NotBlank
    private String name;

    @DiffIgnore
    private Integer fsmOrder;

    private String source;

    private String filter;

    @JsonIgnore
    private String compareKey; // This property used only for Javers library is used to identify changes

    @Override
    @JsonIgnore
    public void setCompareKey() {
        compareKey = defaultString(this.type) + UNDERSCORE + defaultString(this.name) + UNDERSCORE + defaultString(this.source);
    }
}
