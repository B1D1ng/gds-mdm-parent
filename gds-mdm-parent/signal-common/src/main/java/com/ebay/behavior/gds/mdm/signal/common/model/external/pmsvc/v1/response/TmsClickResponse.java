package com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.response;

import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.datatype.ClickV1;

import lombok.Getter;
import lombok.Setter;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmsClickResponse extends ListResponse<ClickV1> {

    @Setter
    private List<ClickV1> click;

    @Override
    public List<ClickV1> getList() {
        return click;
    }
}
