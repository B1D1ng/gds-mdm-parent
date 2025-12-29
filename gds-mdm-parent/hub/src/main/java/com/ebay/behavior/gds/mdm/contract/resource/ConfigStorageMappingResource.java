package com.ebay.behavior.gds.mdm.contract.resource;

import com.ebay.behavior.gds.mdm.contract.config.ContractGovernanceConfiguration;
import com.ebay.behavior.gds.mdm.contract.model.ConfigStorageMapping;
import com.ebay.behavior.gds.mdm.contract.service.ConfigStorageMappingService;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
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
import static com.ebay.behavior.gds.mdm.contract.util.ValidationUtils.validateModerator;

@Validated
@Path(V1 + CMM + DEFINITION + "/config-storage-mapping")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ConfigStorageMappingResource {

    @Autowired
    private ConfigStorageMappingService service;

    @Autowired
    private ContractGovernanceConfiguration configuration;

    @Context
    private UriInfo uriInfo;

    @POST
    @Operation(summary = "Create a new ConfigStorageMapping")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created the Hive Config",
                    content = {@Content(schema = @Schema(implementation = ConfigStorageMapping.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response create(ConfigStorageMapping configStorageMapping) {
        validateModerator(getRequestUser(), configuration);
        val persisted = service.create(configStorageMapping);
        return created(uriInfo, persisted);
    }

    @PATCH
    @Path("/{id}")
    @Operation(summary = "Update ConfigStorageMapping by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated the Hive Config",
                    content = {@Content(schema = @Schema(implementation = ConfigStorageMapping.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response update(@PathParam(ID) Long id, ConfigStorageMapping configStorageMapping) {
        validateModerator(getRequestUser(), configuration);
        validateUpdateRequestId(configStorageMapping, id);
        val updated = service.update(configStorageMapping);
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete ConfigStorageMapping by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted Hive Config",
                    content = {@Content(schema = @Schema(implementation = ConfigStorageMapping.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response delete(@PathParam(ID) Long id) {
        validateModerator(getRequestUser(), configuration);
        service.delete(id);
        return Response.noContent().build();
    }
}
