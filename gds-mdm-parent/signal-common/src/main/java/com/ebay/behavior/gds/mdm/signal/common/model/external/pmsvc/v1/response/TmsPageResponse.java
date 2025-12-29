package com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.response;

import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.datatype.PageV1;

import lombok.Getter;
import lombok.Setter;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmsPageResponse extends ListResponse<PageV1> {

    @Setter
    protected List<PageV1> pages;

    @Override
    public List<PageV1> getList() {
        return pages;
    }
}
