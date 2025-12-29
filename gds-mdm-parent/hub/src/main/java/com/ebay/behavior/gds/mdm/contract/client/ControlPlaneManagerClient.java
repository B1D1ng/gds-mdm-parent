package com.ebay.behavior.gds.mdm.contract.client;

import com.ebay.behavior.gds.mdm.contract.model.client.ControlPlaneRequest;
import com.ebay.behavior.gds.mdm.contract.model.client.ControlPlaneResponse;
import com.ebay.behavior.gds.mdm.contract.model.exception.ControlPlaneManagerException;

import jakarta.inject.Named;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ControlPlaneManagerClient {

    public static final String CONTRACT_RESOURCE = "CONTRACT";

    @Autowired
    @Named("controlPlaneManager")
    private WebTarget target;

    public <CTX> ControlPlaneResponse requestWorkflow(@NotNull @Valid ControlPlaneRequest<CTX> request) {
        try (val res = target.request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(request))) {
            if (res.getStatus() != HttpStatus.SC_CREATED) {
                log.error("ControlPlaneManagerClient request {} failed with status code {}", request, res.getStatus());
                throw new ControlPlaneManagerException(res.readEntity(String.class));
            }
            return res.readEntity(ControlPlaneResponse.class);
        } catch (Exception ex) {
            log.error("Error deploying contract", ex);
            throw new ControlPlaneManagerException(ex);
        }
    }
}
