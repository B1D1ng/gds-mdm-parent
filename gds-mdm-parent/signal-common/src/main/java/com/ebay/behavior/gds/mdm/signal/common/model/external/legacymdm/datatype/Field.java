package com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.datatype;

import com.ebay.behavior.gds.mdm.signal.common.model.JaversIdentity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.val;

import java.util.List;

import static com.ebay.behavior.gds.mdm.common.model.Model.COMMA;
import static com.ebay.behavior.gds.mdm.common.util.ServiceConstants.UNDERSCORE;
import static org.apache.commons.lang3.StringUtils.defaultString;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Field implements JaversIdentity {

    @NotEmpty
    private List<String> readyStates;

    private String clz;

    private String type;

    @NotBlank
    private String name;

    private String formula;

    private boolean cached;

    @JsonIgnore
    private String compareKey; // This property used only for Javers library is used to identify changes

    @Override
    @JsonIgnore
    public void setCompareKey() {
        val states = readyStates == null ? "" : String.join(COMMA, readyStates);
        compareKey = defaultString(this.name) + UNDERSCORE + defaultString(this.type) + UNDERSCORE + states;
    }
}
