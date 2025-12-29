package com.ebay.behavior.gds.mdm.dec.resource;

import com.ebay.behavior.gds.mdm.common.util.ResourceUtils;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalAssetAttribute;
import com.ebay.behavior.gds.mdm.dec.model.enums.PhysicalAssetAttributeName;
import com.ebay.behavior.gds.mdm.dec.service.PhysicalAssetAttributeService;
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

import java.util.List;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.PHYSICAL_ASSET_ATTRIBUTE_API;

@Path(V1 + PHYSICAL_ASSET_ATTRIBUTE_API)
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PhysicalAssetAttributeResource {

    @Autowired
    private PhysicalAssetAttributeService service;

    @Context
    private UriInfo uriInfo;

    @POST
    @Operation(summary = "Create new Physical Asset Attribute", tags = {"Physical Asset Attributes"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Created the Physical Asset Attribute",
                            content = {@Content(schema = @Schema(implementation = PhysicalAssetAttribute.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response create(PhysicalAssetAttribute attribute) {
        val persisted = service.create(attribute);
        return ResourceUtils.created(uriInfo, persisted);
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update existing Physical Asset Attribute", tags = {"Physical Asset Attributes"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Updated the Physical Asset Attribute",
                            content = {@Content(schema = @Schema(implementation = PhysicalAssetAttribute.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
                    @ApiResponse(
                            responseCode = "417",
                            description = "Id not found",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response update(@PathParam(ID) Long id, PhysicalAssetAttribute attribute) {
        ResourceUtils.validateUpdateRequestId(attribute, id);
        val persisted = service.update(attribute);
        return Response.ok(persisted).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get Physical Asset Attribute by id", tags = {"Physical Asset Attributes"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Get Physical Asset Attribute",
                            content = {@Content(schema = @Schema(implementation = PhysicalAssetAttribute.class))}),
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
    @Operation(summary = "Delete a Physical Asset Attribute", tags = {"Physical Asset Attributes"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted a Physical Asset Attribute"),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response delete(@PathParam(ID) Long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    @GET
    @Operation(summary = "Get all Physical Asset Attributes", tags = {"Physical Asset Attributes"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Get all Physical Asset Attributes",
                            content = {@Content(schema = @Schema(implementation = PhysicalAssetAttribute.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response getAll(@QueryParam("assetId") Long assetId) {
        List<PhysicalAssetAttribute> result;
        if (assetId != null) {
            result = service.getAllByAssetId(assetId);
        } else {
            result = service.getAll();
        }
        return Response.ok(result).build();
    }

    @POST
    @Path("/asset/{assetId}")
    @Operation(summary = "Create or update an attribute for a Physical Asset", tags = {"Physical Asset Attributes"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Created or updated the Physical Asset Attribute",
                            content = {@Content(schema = @Schema(implementation = PhysicalAssetAttribute.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
                    @ApiResponse(
                            responseCode = "417",
                            description = "Asset Id not found",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response createOrUpdateAttribute(
            @PathParam("assetId") Long assetId,
            @QueryParam("name") String attributeName,
            @QueryParam("value") String attributeValue) {
        val persisted = service.createOrUpdateAttribute(assetId, PhysicalAssetAttributeName.valueOf(attributeName), attributeValue);
        return Response.ok(persisted).build();
    }
}