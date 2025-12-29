package com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.response;

import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.datatype.PropertyV1;

import lombok.Getter;
import lombok.Setter;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmsPropertyResponse extends ListResponse<PropertyV1> {

    @Setter
    private List<PropertyV1> properties;

    @Override
    public List<PropertyV1> getList() {
        return properties;
    }
}