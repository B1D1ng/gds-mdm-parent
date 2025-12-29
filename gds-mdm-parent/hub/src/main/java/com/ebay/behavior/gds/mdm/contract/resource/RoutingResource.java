package com.ebay.behavior.gds.mdm.contract.resource;

import com.ebay.behavior.gds.mdm.contract.config.ContractGovernanceConfiguration;
import com.ebay.behavior.gds.mdm.contract.model.Routing;
import com.ebay.behavior.gds.mdm.contract.service.RoutingService;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.created;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.getRequestUser;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.validateUpdateRequestId;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.CMM;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.DEFINITION;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.RECURSIVE;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.WITH_ASSOCIATIONS;
import static com.ebay.behavior.gds.mdm.contract.util.ValidationUtils.validateModerator;

@Validated
@Path(V1 + CMM + DEFINITION + "/routing")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RoutingResource {

    @Autowired
    private RoutingService routingService;

    @Autowired
    private ContractGovernanceConfiguration configuration;

    @Context
    private UriInfo uriInfo;

    @POST
    @Operation(summary = "Create new Routing")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created the Routing",
                    content = {@Content(schema = @Schema(implementation = Routing.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response create(Routing routing) {
        validateModerator(getRequestUser(), configuration);
        val persisted = routingService.create(routing);
        return created(uriInfo, persisted);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get Routing metadata by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get Routing metadata",
                    content = {@Content(schema = @Schema(implementation = Routing.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getById(@PathParam(ID) Long id,
                            @QueryParam(WITH_ASSOCIATIONS) @DefaultValue("false") Boolean withAssociations,
                            @QueryParam(RECURSIVE) @DefaultValue("false") Boolean recursive) {
        if (withAssociations) {
            return Response.ok(routingService.getByIdWithAssociations(id, recursive)).build();
        }
        return Response.ok(routingService.getById(id)).build();
    }

    @PATCH
    @Path("/{id}")
    @Operation(summary = "Update existing Routing")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated the Routing",
                    content = {@Content(schema = @Schema(implementation = Routing.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response update(@PathParam(ID) Long id, Routing routing) {
        validateModerator(getRequestUser(), configuration);
        validateUpdateRequestId(routing, id);
        val created = routingService.update(routing);
        return Response.ok(created).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete Routing metadata by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deleted Routing metadata",
                    content = {@Content(schema = @Schema(implementation = Routing.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response delete(@PathParam(ID) Long id) {
        validateModerator(getRequestUser(), configuration);
        routingService.delete(id);
        return Response.noContent().build();
    }

    @PUT
    @Path("/{id}/components")
    @Operation(summary = "Update binding between Routing and Components")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated the binding",
                    content = {@Content(schema = @Schema(implementation = Routing.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response updateBinding(@PathParam("id") Long routingId, List<Long> componentIds) {
        validateModerator(getRequestUser(), configuration);
        routingService.updateComponentsMapping(routingId, componentIds);
        val routings = routingService.getByIdWithAssociations(routingId);
        return Response.ok(routings).build();
    }
}
