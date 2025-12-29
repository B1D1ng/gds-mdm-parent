package com.ebay.behavior.gds.mdm.dec.resource;

import com.ebay.behavior.gds.mdm.common.util.ResourceUtils;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalStorage;
import com.ebay.behavior.gds.mdm.dec.model.Pipeline;
import com.ebay.behavior.gds.mdm.dec.model.enums.MappingSaveMode;
import com.ebay.behavior.gds.mdm.dec.service.PhysicalStorageService;
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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.PHYSICAL_STORAGE_METADATA_API;

@Path(V1 + PHYSICAL_STORAGE_METADATA_API)
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PhysicalStorageResource {

    @Autowired
    private PhysicalStorageService service;

    @Context
    private UriInfo uriInfo;

    @POST
    @Operation(summary = "Create new Physical Storage", tags = {"Physical Storage Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Created the Physical Storage",
                            content = {@Content(schema = @Schema(implementation = PhysicalStorage.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response create(PhysicalStorage storage) {
        val persisted = service.create(storage);
        return ResourceUtils.created(uriInfo, persisted);
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update existing Physical Storage", tags = {"Physical Storage Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Updated the Physical Storage",
                            content = {@Content(schema = @Schema(implementation = PhysicalStorage.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
                    @ApiResponse(
                            responseCode = "417",
                            description = "Id not found",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response update(@PathParam(ID) Long id, PhysicalStorage storage) {
        ResourceUtils.validateUpdateRequestId(storage, id);
        val persisted = service.update(storage);
        return Response.ok(persisted).build();
    }

    @PUT
    @Path("/{id}/pipeline-mappings")
    @Operation(summary = "Update existing Physical Storage - pipeline mappings", tags = {"Physical Storage Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Updated the Physical Storage pipeline mappings",
                            content = {@Content(schema = @Schema(implementation = PhysicalStorage.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
                    @ApiResponse(
                            responseCode = "417",
                            description = "Id not found",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response updatePipelineMappings(@PathParam(ID) Long id, Set<Pipeline> mappings, @QueryParam("mode") MappingSaveMode mode) {
        val res = service.savePipelineMappings(id, mappings, mode);
        return Response.ok(res).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get Physical Storage by id", tags = {"Physical Storage Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Get PhysicalStorage",
                            content = {@Content(schema = @Schema(implementation = PhysicalStorage.class))}),
                    @ApiResponse(
                            responseCode = "417",
                            description = "Id not found",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response getById(@PathParam(ID) long id) {
        val persisted = service.getByIdWithAssociations(id);
        return Response.ok(persisted).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete a storage", tags = {"Physical Storage Metadata"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted a storage"),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response delete(@PathParam(ID) Long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    @GET
    @Operation(summary = "Get all storages", tags = {"Physical Storage Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Get all storages",
                            content = {@Content(schema = @Schema(oneOf = PhysicalStorage.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response getAll(@QueryParam("ldmEntityId") Long ldmEntityId, @QueryParam("datasetId") Long datasetId,
                           @QueryParam("exclusive") Boolean exclusive, @QueryParam("cascade") Boolean cascade, @QueryParam("env") String env) {
        if (ldmEntityId != null) {
            if (cascade != null && cascade) {
                val res = service.getAllByLdmEntityIdWithCascade(ldmEntityId, exclusive, env);
                return Response.ok(res).build();
            } else {
                val res = service.getAllByLdmEntityId(ldmEntityId, exclusive, env);
                return Response.ok(res).build();
            }
        }
        if (datasetId != null) {
            val res = service.getAllByDatasetId(datasetId, exclusive, env);
            return Response.ok(res).build();
        }
        val res = service.getAll(env);
        return Response.ok(res).build();
    }
}
