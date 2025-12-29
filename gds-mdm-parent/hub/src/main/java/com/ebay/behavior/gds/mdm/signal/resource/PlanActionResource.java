package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.signal.common.model.Plan;
import com.ebay.behavior.gds.mdm.signal.service.PlanActionService;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.DEFINITION;

@Validated
@Path(V1 + DEFINITION + "/plan/{planId}/action")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PlanActionResource {

    @Autowired
    private PlanActionService service;

    @PUT
    @Path("/complete")
    @Operation(summary = "Updated the Plan")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated the Plan", content = {@Content(schema = @Schema(implementation = Plan.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response complete(@PathParam("planId") Long id) {
        return Response.ok(service.complete(id)).build();
    }

    @PUT
    @Path("/submit-for-review")
    @Operation(summary = "Updated the Plan")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated the Plan", content = {@Content(schema = @Schema(implementation = Plan.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response submitForReview(@PathParam("planId") Long id) {
        return Response.ok(service.submitForReview(id)).build();
    }

    @PUT
    @Path("/approve")
    @Operation(summary = "Updated the Plan")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated the Plan", content = {@Content(schema = @Schema(implementation = Plan.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response approve(@PathParam("planId") Long id) {
        return Response.ok(service.approve(id)).build();
    }

    @PUT
    @Path("/reject")
    @Operation(summary = "Updated the Plan")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated the Plan", content = {@Content(schema = @Schema(implementation = Plan.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response reject(@PathParam("planId") Long id, Map<String, String> commentBody) {
        return Response.ok(service.reject(id, commentBody.get("comment"))).build();
    }

    @PUT
    @Path("/hide")
    @Operation(summary = "Updated the Plan")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated the Plan", content = {@Content(schema = @Schema(implementation = Plan.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response hide(@PathParam("planId") Long id) {
        return Response.ok(service.hide(id)).build();
    }

    @PUT
    @Path("/cancel")
    @Operation(summary = "Updated the Plan")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated the Plan", content = {@Content(schema = @Schema(implementation = Plan.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response cancel(@PathParam("planId") Long id) {
        return Response.ok(service.cancel(id)).build();
    }
}
