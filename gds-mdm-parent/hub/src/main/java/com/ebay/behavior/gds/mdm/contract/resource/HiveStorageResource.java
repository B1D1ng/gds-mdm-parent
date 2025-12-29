package com.ebay.behavior.gds.mdm.contract.resource;

import com.ebay.behavior.gds.mdm.contract.config.ContractGovernanceConfiguration;
import com.ebay.behavior.gds.mdm.contract.model.HiveStorage;
import com.ebay.behavior.gds.mdm.contract.service.HiveStorageService;
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
@Path(V1 + CMM + DEFINITION + "/hive-storage")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class HiveStorageResource {

    @Autowired
    private HiveStorageService service;

    @Autowired
    private ContractGovernanceConfiguration configuration;

    @Context
    private UriInfo uriInfo;

    @POST
    @Operation(summary = "Create Hive Storage")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created the Hive Storage",
            content = {@Content(schema = @Schema(implementation = HiveStorage.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response create(HiveStorage hiveStorage) {
        validateModerator(getRequestUser(), configuration);
        val persisted = service.create(hiveStorage);
        return created(uriInfo, persisted);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get Hive Storage by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The Hive Storage",
            content = {@Content(schema = @Schema(implementation = HiveStorage.class))}),
            @ApiResponse(responseCode = "417", description = "Id Not found",
            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getById(@PathParam(ID) Long id) {
        return Response.ok(service.getById(id)).build();
    }

    @PATCH
    @Path("/{id}")
    @Operation(summary = "Update Hive Storage")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated the Hive Storage",
            content = {@Content(schema = @Schema(implementation = HiveStorage.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id Not found",
            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response update(@PathParam(ID) Long id, HiveStorage hiveStorage) {
        validateModerator(getRequestUser(), configuration);
        validateUpdateRequestId(hiveStorage, id);
        val updated = service.update(hiveStorage);
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete Hive Storage by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted Hive Storage",
            content = {@Content(schema = @Schema(implementation = HiveStorage.class))}),
            @ApiResponse(responseCode = "500", description = "This storage can not be deleted, as it is in use.",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id Not found",
            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response delete(@PathParam(ID) Long id) {
        validateModerator(getRequestUser(), configuration);
        service.delete(id);
        return Response.noContent().build();
    }
}