package com.ebay.behavior.gds.mdm.dec.resource;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.util.ResourceUtils;
import com.ebay.behavior.gds.mdm.dec.model.Dataset;
import com.ebay.behavior.gds.mdm.dec.model.DatasetPhysicalStorageMapping;
import com.ebay.behavior.gds.mdm.dec.model.dto.DatasetStatusUpdateRequest;
import com.ebay.behavior.gds.mdm.dec.model.enums.MappingSaveMode;
import com.ebay.behavior.gds.mdm.dec.service.DatasetPhysicalStorageMappingService;
import com.ebay.behavior.gds.mdm.dec.service.DatasetService;
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
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.DATASET_METADATA_API;

@Path(V1 + DATASET_METADATA_API)
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DatasetResource {

    @Autowired
    private DatasetService service;

    @Autowired
    private DatasetPhysicalStorageMappingService mappingService;

    @Context
    private UriInfo uriInfo;

    @POST
    @Operation(summary = "Create new Dataset", tags = {"Dataset Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Created the Dataset",
                            content = {@Content(schema = @Schema(implementation = Dataset.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response create(Dataset dataset) {
        val persisted = service.create(dataset);
        return ResourceUtils.created(uriInfo, persisted);
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update existing dataset", tags = {"Dataset Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Updated the Dataset",
                            content = {@Content(schema = @Schema(implementation = Dataset.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
                    @ApiResponse(
                            responseCode = "417",
                            description = "Id not found",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response update(@PathParam(ID) Long id, Dataset dataset) {
        ResourceUtils.validateUpdateRequestId(dataset, id);
        val persisted = service.update(dataset);
        return Response.ok(persisted).build();
    }

    @PUT
    @Path("/{id}/status/{status}")
    @Operation(summary = "Update existing dataset", tags = {"Dataset Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Updated the Dataset",
                            content = {@Content(schema = @Schema(implementation = Dataset.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
                    @ApiResponse(
                            responseCode = "417",
                            description = "Id not found",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response updateStatus(@PathParam(ID) Long id, @PathParam("status") String status, DatasetStatusUpdateRequest request) {
        val persisted = service.updateStatus(id, status, request);
        return Response.ok(persisted).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get dataset by id", tags = {"Dataset Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Get Dataset",
                            content = {@Content(schema = @Schema(implementation = Dataset.class))}),
                    @ApiResponse(
                            responseCode = "417",
                            description = "Id not found",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response getById(@PathParam(ID) long id, @QueryParam("version") Integer version) {
        if (version != null) {
            val res = service.getById(VersionedId.of(id, version));
            return Response.ok(res).build();
        }
        val persisted = service.getByIdCurrentVersion(id);
        return Response.ok(persisted).build();
    }

    @GET
    @Operation(summary = "Get all datasets", tags = {"Dataset Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Get all datasets",
                            content = {@Content(schema = @Schema(oneOf = Dataset.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response getAll(@QueryParam(NAME) String name, @QueryParam("namespace") String namespaceName,
                           @QueryParam("ldmEntityId") Long ldmEntityId, @QueryParam("ldmVersion") Integer ldmVersion) {
        if (StringUtils.isNotBlank(name) || StringUtils.isNotBlank(namespaceName)) {
            val res = service.searchByNameAndNamespace(name, namespaceName);
            return Response.ok(res).build();
        } else if (ldmEntityId != null && ldmVersion != null) {
            val res = service.getAllByLdmEntityIdCurrentVersion(ldmEntityId, ldmVersion);
            return Response.ok(res).build();
        } else if (ldmEntityId != null) {
            val res = service.getAllByLdmEntityId(ldmEntityId);
            return Response.ok(res).build();
        }
        val res = service.getAllCurrentVersion();
        return Response.ok(res).build();
    }

    @PUT
    @Path("/{id}/physical-mappings")
    @Operation(summary = "Update all physical mappings of a dataset", tags = {"Dataset Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Updated physical mappings of the Dataset",
                            content = {@Content(schema = @Schema(implementation = Dataset.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
                    @ApiResponse(
                            responseCode = "417",
                            description = "Id not found",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response updatePhysicalMappings(@PathParam(ID) long id, @QueryParam("mode") MappingSaveMode mode, Set<DatasetPhysicalStorageMapping> mappings) {
        val res = service.savePhysicalMappings(id, mappings, mode);
        return Response.ok(res).build();
    }

    @GET
    @Path("/{id}/physical-mappings")
    @Operation(summary = "Get all physical mappings of a dataset", tags = {"Dataset Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Get all physical mappings of a dataset",
                            content = {@Content(schema = @Schema(oneOf = DatasetPhysicalStorageMapping.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response getPhysicalMappingByDatasetId(@PathParam(ID) long id, @QueryParam("env") String env,
                                                  @QueryParam("system_context") String systemContext, @QueryParam("latest") Boolean latest) {
        if (StringUtils.isNotBlank(systemContext)) {
            val res = mappingService.getAllByDatasetIdAndEnvironmentAndStorageContexts(id, env, systemContext, latest);
            return Response.ok(res).build();
        }
        val res = mappingService.getAllByDatasetIdAndEnvironment(id, env, latest);
        return Response.ok(res).build();
    }

    @POST
    @Path("/{id}/version")
    @Operation(summary = "Create new Dataset Version", tags = {"Dataset Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Created the Dataset Version",
                            content = {@Content(schema = @Schema(implementation = Dataset.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response createNewVersion(@PathParam(ID) long id, Dataset dataset) {
        ResourceUtils.validateUpdateRequestId(dataset, id);
        val persisted = service.saveAsNewVersion(dataset);
        return ResourceUtils.created(uriInfo, persisted);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete Dataset", tags = {"Dataset Metadata"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted a dataset"),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response delete(@PathParam(ID) Long id) {
        service.delete(id);
        return Response.noContent().build();
    }
}
