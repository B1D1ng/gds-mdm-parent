package com.ebay.behavior.gds.mdm.dec.resource;

import com.ebay.behavior.gds.mdm.common.util.ResourceUtils;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalAssetInfraGlobalProperty;
import com.ebay.behavior.gds.mdm.dec.model.enums.InfraType;
import com.ebay.behavior.gds.mdm.dec.model.enums.PropertyType;
import com.ebay.behavior.gds.mdm.dec.service.PhysicalAssetInfraGlobalPropertyService;
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
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.PHYSICAL_ASSET_INFRA_GP_API;

@Path(V1 + PHYSICAL_ASSET_INFRA_GP_API)
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PhysicalAssetInfraGlobalPropertyResource {

    @Autowired
    private PhysicalAssetInfraGlobalPropertyService service;

    @Context
    private UriInfo uriInfo;

    @POST
    @Operation(summary = "Create new Physical Asset Infra Global Property", tags = {"Physical Asset Infra Global Property"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Created the Physical Asset Infra Global Property",
                            content = {@Content(schema = @Schema(implementation = PhysicalAssetInfraGlobalProperty.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response create(PhysicalAssetInfraGlobalProperty prop) {
        val persisted = service.create(prop);
        return ResourceUtils.created(uriInfo, persisted);
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update existing Physical Asset Infra Global Property", tags = {"Physical Asset Infra Global Property"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Updated the Physical Asset Infra Global Property",
                            content = {@Content(schema = @Schema(implementation = PhysicalAssetInfraGlobalProperty.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
                    @ApiResponse(
                            responseCode = "417",
                            description = "Id not found",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response update(@PathParam(ID) Long id, PhysicalAssetInfraGlobalProperty prop) {
        ResourceUtils.validateUpdateRequestId(prop, id);
        val persisted = service.update(prop);
        return Response.ok(persisted).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get Physical Asset Infra Global Property by id", tags = {"Physical Asset Infra Global Property"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Get Physical Asset Infra Global Property",
                            content = {@Content(schema = @Schema(implementation = PhysicalAssetInfraGlobalProperty.class))}),
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
    @Operation(summary = "Delete a Physical Asset Infra Global Property", tags = {"Physical Asset Infra Global Property"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted a Physical Asset Infra Global Property"),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response delete(@PathParam(ID) Long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    @GET
    @Operation(summary = "Get all Physical Asset Infra Global Properties", tags = {"Physical Asset Infra Global Property"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Get all Physical Asset Infra Global Properties",
                            content = {@Content(schema = @Schema(oneOf = PhysicalAssetInfraGlobalProperty.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response getAll(@QueryParam("infraType") InfraType infraType, @QueryParam("propertyType") PropertyType propertyType) {
        List<PhysicalAssetInfraGlobalProperty> res;

        if (infraType != null && propertyType != null) {
            res = service.getAllByInfraTypeAndPropertyType(infraType, propertyType);
        } else if (infraType != null) {
            res = service.getAllByInfraType(infraType);
        } else if (propertyType != null) {
            res = service.getAllByPropertyType(propertyType);
        } else {
            res = service.getAll();
        }

        return Response.ok(res).build();
    }
}