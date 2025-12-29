package com.ebay.behavior.gds.mdm.dec.resource;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.util.ResourceUtils;
import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmErrorHandlingStorageMapping;
import com.ebay.behavior.gds.mdm.dec.model.LdmField;
import com.ebay.behavior.gds.mdm.dec.model.LdmFieldPhysicalStorageMapping;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmEntityRequest;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmEntityWrapper;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmFieldPhysicalMappingRequest;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmRollbackRequest;
import com.ebay.behavior.gds.mdm.dec.model.dto.StatusUpdateRequest;
import com.ebay.behavior.gds.mdm.dec.model.enums.LdmStatus;
import com.ebay.behavior.gds.mdm.dec.service.LdmActionService;
import com.ebay.behavior.gds.mdm.dec.service.LdmBaseEntityService;
import com.ebay.behavior.gds.mdm.dec.service.LdmEntityService;
import com.ebay.behavior.gds.mdm.dec.service.LdmErrorHandlingStorageMappingService;
import com.ebay.behavior.gds.mdm.dec.service.LdmFieldService;
import com.ebay.behavior.gds.mdm.dec.service.LdmReadService;
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
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.LDM_METADATA_API;

@Path(V1 + LDM_METADATA_API)
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class LdmEntityResource {

    @Autowired
    private LdmEntityService service;

    @Autowired
    private LdmActionService actionService;

    @Autowired
    private LdmFieldService fieldService;

    @Autowired
    private LdmBaseEntityService baseEntityService;

    @Autowired
    private LdmReadService readService;

    @Autowired
    private LdmErrorHandlingStorageMappingService errorStorageMappingService;

    @Context
    private UriInfo uriInfo;

    @POST
    @Operation(summary = "Create new ldm", tags = {"Ldm Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Created the Ldm",
                            content = {@Content(schema = @Schema(implementation = LdmEntity.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response create(LdmEntityRequest entity) {
        val persisted = service.create(entity.toLdmEntity());
        return ResourceUtils.created(uriInfo, persisted);
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update existing Ldm", tags = {"Ldm Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Updated the Ldm",
                            content = {@Content(schema = @Schema(implementation = LdmEntity.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
                    @ApiResponse(
                            responseCode = "417",
                            description = "Id not found",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response update(@PathParam(ID) Long id, LdmEntityRequest entity) {
        ResourceUtils.validateUpdateRequestId(entity.toLdmEntity(), id);
        val persisted = service.update(entity.toLdmEntity());
        return Response.ok(persisted).build();
    }

    @PUT
    @Path("/{id}/rollback")
    @Operation(summary = "Rollback existing Ldm", tags = {"Ldm Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Rollback the Ldm",
                            content = {@Content(schema = @Schema(implementation = LdmEntity.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
                    @ApiResponse(
                            responseCode = "417",
                            description = "Id not found",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response rollback(@PathParam(ID) Long id, LdmRollbackRequest request) {
        val persisted = actionService.rollback(id, request);
        return Response.ok(persisted).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get Ldm by id", tags = {"Ldm Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Get Ldm",
                            content = {@Content(schema = @Schema(implementation = LdmEntityWrapper.class))}),
                    @ApiResponse(
                            responseCode = "417",
                            description = "Id not found",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response getById(@PathParam(ID) long id, @QueryParam("ext") Boolean withExtendedInfo, @QueryParam("env") String env,
                            @QueryParam("version") Integer version) {
        if (version != null) {
            val res = readService.getByIdInWrapper(VersionedId.of(id, version), withExtendedInfo, env);
            return Response.ok(res).build();
        }
        val res = readService.getByIdInWrapper(id, withExtendedInfo, env);
        return Response.ok(res).build();
    }

    @GET
    @Operation(summary = "Get all Ldm", tags = {"Ldm Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Get all Ldm",
                            content = {@Content(schema = @Schema(oneOf = LdmEntity.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response getAll(@QueryParam(NAME) String name,
                           @QueryParam("viewType") String viewType,
                           @QueryParam("namespace") String namespaceName,
                           @QueryParam("lite") Boolean excludeTextFields) {
        if (StringUtils.isNotBlank(name) || StringUtils.isNotBlank(viewType) || StringUtils.isNotBlank(namespaceName)) {
            val res = readService.searchByNameAndNamespace(name, viewType, namespaceName, excludeTextFields);
            return Response.ok(res).build();
        }
        val res = readService.getAllCurrentVersion(excludeTextFields);
        return Response.ok(res).build();
    }

    @POST
    @Path("/{id}/version")
    @Operation(summary = "Create new ldm", tags = {"Ldm Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Created the Ldm",
                            content = {@Content(schema = @Schema(implementation = LdmEntity.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response createNewVersion(@PathParam(ID) long id, LdmEntityRequest entity) {
        ResourceUtils.validateUpdateRequestId(entity.toLdmEntity(), id);
        val persisted = service.saveAsNewVersion(entity.toLdmEntity(), null, false);
        return ResourceUtils.created(uriInfo, persisted);
    }

    @PUT
    @Path("/{id}/status/{status}")
    @Operation(summary = "Update ldm status", tags = {"Ldm Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Updated ldm",
                            content = {@Content(schema = @Schema(implementation = LdmEntity.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
                    @ApiResponse(
                            responseCode = "417",
                            description = "Id not found",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response updateStatus(@PathParam(ID) Long id, @PathParam("status") LdmStatus status, StatusUpdateRequest request) {
        val persisted = actionService.updateStatus(id, status, request);
        return Response.ok(persisted).build();
    }

    @POST
    @Path("/initialize")
    @Operation(summary = "Initialize new ldm in bootstrap phase", tags = {"Base Ldm Metadata Governance"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Initialized ldm",
                            content = {@Content(schema = @Schema(implementation = LdmBaseEntity.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response initialize(LdmBaseEntity entity) {
        val persisted = baseEntityService.create(entity);
        return ResourceUtils.created(uriInfo, persisted);
    }

    @PUT
    @Path("/{id}/fields")
    @Operation(summary = "Bulk update ldm field", tags = {"Ldm Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Updated ldm",
                            content = {@Content(schema = @Schema(oneOf = LdmField.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
                    @ApiResponse(
                            responseCode = "417",
                            description = "Id not found",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response updateFields(@PathParam(ID) Long id, Set<LdmField> fields) {
        val persisted = service.updateFields(id, fields);
        return Response.ok(persisted).build();
    }

    @PUT
    @Path("/{id}/fields/physical-mappings")
    @Operation(summary = "Bulk update new ldm field physical mappings", tags = {"Ldm Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Updated ldm",
                            content = {@Content(schema = @Schema(oneOf = LdmFieldPhysicalStorageMapping.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
                    @ApiResponse(
                            responseCode = "417",
                            description = "Id not found",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response updateFieldPhysicalMappings(@PathParam(ID) Long id, Set<LdmFieldPhysicalMappingRequest> fieldPhysicalMappings) {
        val persisted = fieldService.updateFieldPhysicalMappings(id, fieldPhysicalMappings);
        return Response.ok(persisted).build();
    }

    @PUT
    @Path("/{id}/fields/physical-storage-mappings")
    @Operation(summary = "Bulk update new ldm field physical mappings", tags = {"Ldm Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Updated ldm",
                            content = {@Content(schema = @Schema(oneOf = LdmFieldPhysicalStorageMapping.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
                    @ApiResponse(
                            responseCode = "417",
                            description = "Id not found",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response updateFieldPhysicalStorageMappings(@PathParam(ID) Long id, Set<LdmFieldPhysicalStorageMapping> fieldPhysicalMappings) {
        val persisted = fieldService.updateFieldPhysicalStorageMappings(id, fieldPhysicalMappings);
        return Response.ok(persisted).build();
    }

    @PUT
    @Path("/{id}/error-handling-physical-mappings")
    @Operation(summary = "Update error handling physical mappings of an LDM entity", tags = {"Ldm Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Updated error handling physical mappings of the LDM entity",
                            content = {@Content(schema = @Schema(implementation = LdmErrorHandlingStorageMapping.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
                    @ApiResponse(
                            responseCode = "417",
                            description = "Id not found",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response updateErrorHandlingPhysicalMappings(@PathParam(ID) long id, Set<LdmErrorHandlingStorageMapping> mappings) {
        LdmEntity entity = service.getByIdCurrentVersion(id);
        val res = errorStorageMappingService.saveErrorHandlingStorageMappings(id, entity.getVersion(), mappings);
        return Response.ok(res).build();
    }

    @GET
    @Path("/{id}/error-handling-physical-mappings")
    @Operation(summary = "Get error handling physical mappings of an LDM entity", tags = {"Ldm Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Get error handling physical mappings of the LDM entity",
                            content = {@Content(schema = @Schema(implementation = LdmErrorHandlingStorageMapping.class))}),
                    @ApiResponse(
                            responseCode = "417",
                            description = "Id not found",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response getErrorHandlingPhysicalMappings(@PathParam(ID) long id, @QueryParam("env") String env,
                                                     @QueryParam("version") Integer version) {
        if (version != null) {
            val res = errorStorageMappingService.getAllByLdmEntityIdAndVersionAndEnvironment(id, version, env);
            return Response.ok(res).build();
        } else {
            val res = errorStorageMappingService.getAllByLdmEntityIdAndEnvironment(id, env, true);
            return Response.ok(res).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete ldm", tags = {"Ldm Metadata"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted a ldm"),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response delete(@PathParam(ID) Long id) {
        actionService.delete(id);
        return Response.noContent().build();
    }
}
