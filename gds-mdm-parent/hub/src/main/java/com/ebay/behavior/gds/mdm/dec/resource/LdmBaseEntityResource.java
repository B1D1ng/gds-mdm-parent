package com.ebay.behavior.gds.mdm.dec.resource;

import com.ebay.behavior.gds.mdm.common.util.ResourceUtils;
import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.service.LdmBaseEntityService;
import com.ebay.behavior.gds.mdm.dec.service.LdmEntityService;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.val;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.LDM_BASE_ENTITY_METADATA_API;

@Path(V1 + LDM_BASE_ENTITY_METADATA_API)
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class LdmBaseEntityResource {

    @Autowired
    private LdmEntityService service;

    @Autowired
    private LdmBaseEntityService baseEntityService;

    @GET
    @Path("/{id}")
    @Operation(summary = "Get Ldm entity by id", tags = {"Ldm Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Get Ldm entity ",
                            content = {@Content(schema = @Schema(implementation = LdmBaseEntity.class))}),
                    @ApiResponse(
                            responseCode = "417",
                            description = "Id not found",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response getEntityById(@PathParam(ID) long id, @QueryParam("env") String env) {
        val res = baseEntityService.getByIdWithAssociations(id, env);
        return Response.ok(res).build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update existing Ldm entity", tags = {"Ldm Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Updated the Ldm",
                            content = {@Content(schema = @Schema(implementation = LdmBaseEntity.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
                    @ApiResponse(
                            responseCode = "417",
                            description = "Id not found",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response updateEntityById(@PathParam(ID) Long id, LdmBaseEntity entity) {
        ResourceUtils.validateUpdateRequestId(entity, id);
        val persisted = service.updateBaseEntity(entity);
        return Response.ok(persisted).build();
    }

    @GET
    @Operation(summary = "Get all Ldm entities", tags = {"Ldm Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Get all Ldm entities",
                            content = {@Content(schema = @Schema(oneOf = LdmBaseEntity.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response getAllEntities(@QueryParam(NAME) String name,
                                   @QueryParam("namespace") String namespaceName,
                                   @QueryParam("lite") Boolean excludeTextFields) {
        if (StringUtils.isNotBlank(name) || StringUtils.isNotBlank(namespaceName)) {
            val res = baseEntityService.searchByNameAndNamespace(name, namespaceName);
            return Response.ok(res).build();
        }
        val res = baseEntityService.getAll(excludeTextFields);
        return Response.ok(res).build();
    }
}
