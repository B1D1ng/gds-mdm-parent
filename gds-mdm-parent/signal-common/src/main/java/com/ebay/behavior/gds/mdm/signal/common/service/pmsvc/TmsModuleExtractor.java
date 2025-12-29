package com.ebay.behavior.gds.mdm.signal.common.service.pmsvc;

import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.datatype.ModuleV1;
import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.response.TmsModuleResponse;

import jakarta.inject.Named;
import jakarta.ws.rs.client.WebTarget;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.AUTOWIRING_INSPECTION;

@Getter
@Component
public class TmsModuleExtractor extends TmsExtractor<ModuleV1> {

    private final Class<ModuleV1> type = ModuleV1.class;

    private final Class<TmsModuleResponse> responseType = TmsModuleResponse.class;

    @Getter
    @Autowired
    @Named(PMSVC_GINGER_CLIENT_NAME)
    @SuppressWarnings(AUTOWIRING_INSPECTION)
    private WebTarget target;

    @Override
    public String getPath() {
        return "tms/searchModule";
    }
}
