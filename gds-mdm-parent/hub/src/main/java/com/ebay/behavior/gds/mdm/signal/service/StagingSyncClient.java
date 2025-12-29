package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.service.AbstractRestPostClient;
import com.ebay.behavior.gds.mdm.common.service.token.TokenGenerator;

import jakarta.inject.Named;
import jakarta.ws.rs.client.WebTarget;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.AUTOWIRING_INSPECTION;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.GDS_MDM_STAGING_CLIENT_NAME;

@Component
@Validated
public class StagingSyncClient extends AbstractRestPostClient {

    @Getter
    @Autowired
    @Named(GDS_MDM_STAGING_CLIENT_NAME)
    @SuppressWarnings(AUTOWIRING_INSPECTION)
    private WebTarget target;

    @Getter
    private final TokenGenerator tokenGenerator = null; // needed for AbstractRestPostClient logic

    @Getter
    private final String path = "udc"; // a path under webTarget
}
