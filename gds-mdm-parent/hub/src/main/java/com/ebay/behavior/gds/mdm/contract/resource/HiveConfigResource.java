package com.ebay.behavior.gds.mdm.contract.resource;

import com.ebay.behavior.gds.mdm.contract.config.ContractGovernanceConfiguration;
import com.ebay.behavior.gds.mdm.contract.model.HiveConfig;
import com.ebay.behavior.gds.mdm.contract.service.HiveConfigService;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotNull;
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

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.created;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.getRequestUser;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.validateUpdateRequestId;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.CMM;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.DEFINITION;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.WITH_ASSOCIATIONS;
import static com.ebay.behavior.gds.mdm.contract.util.ValidationUtils.validateModerator;

@Validated
@Path(V1 + CMM + DEFINITION + "/hive-config")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class HiveConfigResource {

    @Autowired
    private HiveConfigService service;

    @Context
    private UriInfo uriInfo;

    @Autowired
    private ContractGovernanceConfiguration configuration;

    @POST
    @Operation(summary = "Create a new Hive Config")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created the Hive Config",
            content = {@Content(schema = @Schema(implementation = HiveConfig.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response create(HiveConfig hiveConfig) {
        validateModerator(getRequestUser(), configuration);
        val persisted = service.create(hiveConfig);
        return created(uriInfo, persisted);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get Hive Config by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The Hive Config",
                    content = {@Content(schema = @Schema(implementation = HiveConfig.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
    })
    public Response getById(@PathParam(ID) Long id, @QueryParam(WITH_ASSOCIATIONS) @DefaultValue("false") boolean withAssociations) {
        if (withAssociations) {
            return Response.ok(service.getByIdWithAssociations(id)).build();
        }
        return Response.ok(service.getById(id)).build();
    }

    @PATCH
    @Path("/{id}")
    @Operation(summary = "Update Hive Config")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated the Hive Config",
                    content = {@Content(schema = @Schema(implementation = HiveConfig.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response update(@PathParam(ID) Long id, HiveConfig hiveConfig) {
        validateModerator(getRequestUser(), configuration);
        validateUpdateRequestId(hiveConfig, id);
        val updated = service.update(hiveConfig);
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete Hive Config by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted Hive Config",
                    content = {@Content(schema = @Schema(implementation = HiveConfig.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response delete(@PathParam(ID) Long id) {
        validateModerator(getRequestUser(), configuration);
        service.delete(id);
        return Response.noContent().build();
    }

    @PUT
    @Path("/{id}/storage")
    @Operation(summary = "Update storage associated with the Hive Config")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated the storage associated with the Hive Config",
            content = {@Content(schema = @Schema(implementation = HiveConfig.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response updateStorage(@PathParam(ID) Long configId, @NotNull Long storageId) {
        validateModerator(getRequestUser(), configuration);
        service.updateMapping(configId, storageId);
        return Response.ok(service.getByIdWithAssociations(configId)).build();
    }
}