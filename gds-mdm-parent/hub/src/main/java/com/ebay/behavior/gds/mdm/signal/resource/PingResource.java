package com.ebay.behavior.gds.mdm.signal.resource;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.web.bind.annotation.RestController;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;

@Path(V1)
@RestController
public class PingResource {

    @GET
    @Path("/signal/ping")
    @Operation(summary = "Test operation")
    public Response ping() {
        return Response.ok().type(MediaType.APPLICATION_JSON).entity("{\"ping\" : \"pong\"}").build();
    }
}
