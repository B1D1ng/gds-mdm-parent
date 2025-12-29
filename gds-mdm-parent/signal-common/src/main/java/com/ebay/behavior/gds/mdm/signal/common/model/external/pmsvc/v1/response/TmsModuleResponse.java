package com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.response;

import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.datatype.ModuleV1;

import lombok.Getter;
import lombok.Setter;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmsModuleResponse extends ListResponse<ModuleV1> {

    @Setter
    protected List<ModuleV1> module;

    @Override
    public List<ModuleV1> getList() {
        return module;
    }
}
