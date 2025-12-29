package com.ebay.behavior.gds.mdm.contract.resource;

import com.ebay.behavior.gds.mdm.contract.config.ContractGovernanceConfiguration;
import com.ebay.behavior.gds.mdm.contract.model.LdmViewSink;
import com.ebay.behavior.gds.mdm.contract.service.LdmViewSinkService;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.created;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.getRequestUser;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.validateUpdateRequestId;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.CMM;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.DEFINITION;
import static com.ebay.behavior.gds.mdm.contract.util.ValidationUtils.validateModerator;

@Validated
@Path(V1 + CMM + DEFINITION + "/ldm-view-sink")
@RestController
@Consumes("application/json")
@Produces("application/json")
public class LdmViewSinkResource {

    @Autowired
    private LdmViewSinkService service;
    @Autowired
    private ContractGovernanceConfiguration configuration;
    @Context
    private UriInfo uriInfo;

    @POST
    @Operation(summary = "Create new LdmViewSink")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created the LdmViewSink"),
            @ApiResponse(responseCode = "400", description = "Invalid LdmViewSink details supplied")
    })
    public Response create(LdmViewSink ldmViewSink) {
        validateModerator(getRequestUser(), configuration);
        var persisted = service.create(ldmViewSink);
        return created(uriInfo, persisted);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get LdmViewSink by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "LdmViewSink found",
                    content = {@Content(schema = @Schema(implementation = LdmViewSink.class))}),
            @ApiResponse(responseCode = "417", description = "LdmViewSink not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getById(@PathParam(ID) Long id) {
        var entity = service.getById(id);
        return Response.ok(entity).build();
    }

    @PATCH
    @Path("/{id}")
    @Operation(summary = "Update LdmViewSink by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated the LdmViewSink",
                    content = {@Content(schema = @Schema(implementation = LdmViewSink.class))}),
            @ApiResponse(responseCode = "400", description = "Bad Request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "LdmViewSink not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response update(@PathParam(ID) Long id, LdmViewSink ldmViewSink) {
        validateModerator(getRequestUser(), configuration);
        validateUpdateRequestId(ldmViewSink, id);
        var updated = service.update(ldmViewSink);
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete LdmViewSink by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deleted the LdmViewSink"),
            @ApiResponse(responseCode = "417", description = "LdmViewSink not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response delete(@PathParam(ID) Long id) {
        validateModerator(getRequestUser(), configuration);
        service.delete(id);
        return Response.noContent().build();
    }
}
