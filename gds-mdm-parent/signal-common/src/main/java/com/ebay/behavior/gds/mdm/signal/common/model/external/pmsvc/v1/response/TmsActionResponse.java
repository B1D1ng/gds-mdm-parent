package com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.response;

import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.datatype.ActionV1;

import lombok.Getter;
import lombok.Setter;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmsActionResponse extends ListResponse<ActionV1> {

    @Setter
    private List<ActionV1> eventAction;

    @Override
    public List<ActionV1> getList() {
        return eventAction;
    }
}

