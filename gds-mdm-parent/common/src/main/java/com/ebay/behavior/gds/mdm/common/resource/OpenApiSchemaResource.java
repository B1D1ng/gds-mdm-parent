package com.ebay.behavior.gds.mdm.common.resource;

import io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.ServletConfig;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.springframework.web.bind.annotation.RestController;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;

@RestController
@Path(V1 + "/openapi.{type:yaml|json}")
public class OpenApiSchemaResource extends BaseOpenApiResource {

    @Context
    private ServletConfig config;

    @Context
    private Application app;

    @GET
    @Operation(summary = "Get openApi schema in yaml or json formats")
    @Produces({"application/json", "application/yaml"})
    public Response get(@Context HttpHeaders headers, @Context UriInfo uriInfo, @PathParam("type") String type) throws Exception {
        return super.getOpenApi(headers, this.config, this.app, uriInfo, type);
    }
}