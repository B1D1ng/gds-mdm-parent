package com.ebay.behavior.gds.mdm.dec.resource;

import com.ebay.behavior.gds.mdm.common.util.ResourceUtils;
import com.ebay.behavior.gds.mdm.dec.model.LdmChangeRequest;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmChangeRequestLogRecord;
import com.ebay.behavior.gds.mdm.dec.service.LdmChangeRequestService;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.CHANGE_REQUEST_API;

@Path(V1 + CHANGE_REQUEST_API)
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class LdmChangeRequestResource {

    @Autowired
    private LdmChangeRequestService service;

    @Context
    private UriInfo uriInfo;

    @POST
    @Operation(summary = "Create new ldm change request", tags = {"Base Ldm Metadata Governance"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Created the ldm change request",
                            content = {@Content(schema = @Schema(implementation = LdmChangeRequest.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response create(LdmChangeRequest ldmChangeRequest) {
        val persisted = service.create(ldmChangeRequest);
        return ResourceUtils.created(uriInfo, persisted);
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update existing ldm change request", tags = {"Base Ldm Metadata Governance"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Updated the ldm change request",
                            content = {@Content(schema = @Schema(implementation = LdmChangeRequest.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
                    @ApiResponse(
                            responseCode = "417",
                            description = "Id not found",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response update(@PathParam(ID) Long id, LdmChangeRequest ldmChangeRequest) {
        ResourceUtils.validateUpdateRequestId(ldmChangeRequest, id);
        val persisted = service.update(ldmChangeRequest);
        return Response.ok(persisted).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete a change request", tags = {"Base Ldm Metadata Governance"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted a request"),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response delete(@PathParam(ID) Long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get change request by id", tags = {"Base Ldm Metadata Governance"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Get change request",
                            content = {@Content(schema = @Schema(implementation = LdmChangeRequest.class))}),
                    @ApiResponse(
                            responseCode = "417",
                            description = "Id not found",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response getById(@PathParam(ID) long id) {
        val persisted = service.getById(id);
        return Response.ok(persisted).build();
    }

    @GET
    @Operation(summary = "Get all change requests", tags = {"Base Ldm Metadata Governance"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Get all change requests",
                            content = {@Content(schema = @Schema(oneOf = LdmChangeRequest.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response getAll() {
        val res = service.getAll();
        return Response.ok(res).build();
    }

    @PUT
    @Path("/{id}/approve")
    @Operation(summary = "Approve existing ldm change request", tags = {"Base Ldm Metadata Governance"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Approved the ldm change request",
                            content = {@Content(schema = @Schema(implementation = LdmChangeRequest.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
                    @ApiResponse(
                            responseCode = "417",
                            description = "Id not found",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response approve(@PathParam(ID) Long id) {
        val persisted = service.approve(id);
        return Response.ok(persisted).build();
    }

    @PUT
    @Path("/{id}/reject")
    @Operation(summary = "Reject existing ldm change request", tags = {"Base Ldm Metadata Governance"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Approved the ldm change request",
                            content = {@Content(schema = @Schema(implementation = LdmChangeRequest.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
                    @ApiResponse(
                            responseCode = "417",
                            description = "Id not found",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response reject(@PathParam(ID) Long id, LdmChangeRequestLogRecord comment) {
        val persisted = service.reject(id, comment);
        return Response.ok(persisted).build();
    }
}
