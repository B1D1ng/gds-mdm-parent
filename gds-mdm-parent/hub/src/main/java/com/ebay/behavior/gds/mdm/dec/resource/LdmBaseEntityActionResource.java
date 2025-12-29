package com.ebay.behavior.gds.mdm.dec.resource;

import com.ebay.behavior.gds.mdm.dec.model.dto.LdmBootstrapRequest;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmBootstrapResponse;
import com.ebay.behavior.gds.mdm.dec.service.LdmBaseEntityActionService;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.LDM_BASE_ENTITY_METADATA_API;

@Path(V1 + LDM_BASE_ENTITY_METADATA_API + "/{id}/action")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Validated
public class LdmBaseEntityActionResource {

    @Autowired
    private LdmBaseEntityActionService service;

    @POST
    @Path("/bootstrap")
    @Operation(summary = "Trigger DecCompiler Bootstrap", tags = {"Ldm Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Trigger DecCompiler Bootstrap",
                            content = {@Content(schema = @Schema(implementation = LdmBootstrapResponse.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
                    @ApiResponse(
                            responseCode = "500",
                            description = "DecCompiler Service Internal Error",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response bootstrap(@PathParam(ID) @NotNull Long id, @Valid LdmBootstrapRequest request) {
        val resp = service.bootstrap(id, request);
        return Response.ok(resp).build();
    }
}
