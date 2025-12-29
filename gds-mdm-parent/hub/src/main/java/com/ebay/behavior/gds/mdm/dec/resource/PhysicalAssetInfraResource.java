package com.ebay.behavior.gds.mdm.dec.resource;

import com.ebay.behavior.gds.mdm.common.util.ResourceUtils;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalAssetInfra;
import com.ebay.behavior.gds.mdm.dec.model.enums.InfraType;
import com.ebay.behavior.gds.mdm.dec.model.enums.PlatformEnvironment;
import com.ebay.behavior.gds.mdm.dec.model.enums.PropertyType;
import com.ebay.behavior.gds.mdm.dec.service.PhysicalAssetInfraService;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.PHYSICAL_ASSET_INFRA_API;

@Path(V1 + PHYSICAL_ASSET_INFRA_API)
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PhysicalAssetInfraResource {

    @Autowired
    private PhysicalAssetInfraService service;

    @Context
    private UriInfo uriInfo;

    @POST
    @Operation(summary = "Create new Physical Asset Infrastructure", tags = {"Physical Asset Infrastructure"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Created the Physical Asset Infrastructure",
                            content = {@Content(schema = @Schema(implementation = PhysicalAssetInfra.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response create(PhysicalAssetInfra assetInfra) {
        val persisted = service.create(assetInfra);
        return ResourceUtils.created(uriInfo, persisted);
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update existing Physical Asset Infrastructure", tags = {"Physical Asset Infrastructure"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Updated the Physical Asset Infrastructure",
                            content = {@Content(schema = @Schema(implementation = PhysicalAssetInfra.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
                    @ApiResponse(
                            responseCode = "417",
                            description = "Id not found",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response update(@PathParam(ID) Long id, PhysicalAssetInfra assetInfra) {
        ResourceUtils.validateUpdateRequestId(assetInfra, id);
        val persisted = service.update(assetInfra);
        return Response.ok(persisted).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get Physical Asset Infrastructure by id", tags = {"Physical Asset Infrastructure"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Get Physical Asset Infrastructure",
                            content = {@Content(schema = @Schema(implementation = PhysicalAssetInfra.class))}),
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
    @Operation(summary = "Delete a Physical Asset Infrastructure", tags = {"Physical Asset Infrastructure"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted a Physical Asset Infrastructure"),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response delete(@PathParam(ID) Long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    @GET
    @Operation(summary = "Get all Physical Asset Infrastructures", tags = {"Physical Asset Infrastructure"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Get all Physical Asset Infrastructures",
                            content = {@Content(schema = @Schema(oneOf = PhysicalAssetInfra.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response getAll(
            @QueryParam("infraType") InfraType infraType,
            @QueryParam("propertyType") PropertyType propertyType,
            @QueryParam("environment") PlatformEnvironment environment) {
        List<PhysicalAssetInfra> res;

        if (infraType != null && propertyType != null && environment != null) {
            res = service.getAllByInfraTypeAndPropertyTypeAndEnvironment(infraType, propertyType, environment);
        } else if (infraType != null && propertyType != null) {
            res = service.getAllByInfraTypeAndPropertyType(infraType, propertyType);
        } else if (infraType != null) {
            res = service.getAllByInfraType(infraType);
        } else if (propertyType != null) {
            res = service.getAllByPropertyType(propertyType);
        } else if (environment != null) {
            res = service.getAllByPlatformEnvironment(environment);
        } else {
            res = service.getAll();
        }
        return Response.ok(res).build();
    }
}