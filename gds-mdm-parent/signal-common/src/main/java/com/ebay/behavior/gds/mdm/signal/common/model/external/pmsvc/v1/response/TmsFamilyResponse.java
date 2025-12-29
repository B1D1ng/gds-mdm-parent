package com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.response;

import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.datatype.FamilyV1;

import lombok.Getter;
import lombok.Setter;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmsFamilyResponse extends ListResponse<FamilyV1> {

    @Setter
    private List<FamilyV1> eventFamilies;

    @Override
    public List<FamilyV1> getList() {
        return eventFamilies;
    }
}

