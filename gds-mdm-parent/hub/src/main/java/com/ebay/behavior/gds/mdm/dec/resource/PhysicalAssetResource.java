package com.ebay.behavior.gds.mdm.dec.resource;

import com.ebay.behavior.gds.mdm.common.util.ResourceUtils;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalAsset;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalAssetAttribute;
import com.ebay.behavior.gds.mdm.dec.model.enums.DecEnvironment;
import com.ebay.behavior.gds.mdm.dec.service.PhysicalAssetAttributeService;
import com.ebay.behavior.gds.mdm.dec.service.PhysicalAssetService;
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
import java.util.Optional;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.PHYSICAL_ASSET_METADATA_API;

@Path(V1 + PHYSICAL_ASSET_METADATA_API)
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PhysicalAssetResource {

    @Autowired
    private PhysicalAssetService service;
    
    @Autowired
    private PhysicalAssetAttributeService attributeService;

    @Context
    private UriInfo uriInfo;

    @POST
    @Operation(summary = "Create new Physical Asset", tags = {"Physical Asset Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Created the Physical Asset",
                            content = {@Content(schema = @Schema(implementation = PhysicalAsset.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response create(PhysicalAsset asset) {
        val persisted = service.create(asset);
        return ResourceUtils.created(uriInfo, persisted);
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update existing Physical Asset", tags = {"Physical Asset Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Updated the Physical Asset",
                            content = {@Content(schema = @Schema(implementation = PhysicalAsset.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
                    @ApiResponse(
                            responseCode = "417",
                            description = "Id not found",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response update(@PathParam(ID) Long id, PhysicalAsset asset) {
        ResourceUtils.validateUpdateRequestId(asset, id);
        val persisted = service.update(asset);
        return Response.ok(persisted).build();
    }

    @PUT
    @Path("/{id}/ldm-mappings")
    @Operation(summary = "Update existing Physical Asset - ldm mappings", tags = {"Physical Asset Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Updated the Physical Asset ldm mappings",
                            content = {@Content(schema = @Schema(implementation = PhysicalAsset.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
                    @ApiResponse(
                            responseCode = "417",
                            description = "Id not found",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response updatePhysicalAssetLdmMappings(@PathParam(ID) Long id, Set<Long> mappings) {
        val res = service.savePhysicalAssetLdmMappings(id, mappings);
        return Response.ok(res).build();
    }

    @PUT
    @Path("/{id}/infra-mappings")
    @Operation(summary = "Update existing Physical Asset - infra mappings", tags = {"Physical Asset Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Updated the Physical Asset infra mappings",
                            content = {@Content(schema = @Schema(implementation = PhysicalAsset.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
                    @ApiResponse(
                            responseCode = "417",
                            description = "Id not found",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response updatePhysicalAssetInfraMappings(@PathParam(ID) Long assetId, Set<Long> infraIds) {
        val res = service.createPhysicalAssetInfraMappings(assetId, infraIds);
        return Response.ok(res).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get Physical Asset by id", tags = {"Physical Asset Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Get Physical Asset",
                            content = {@Content(schema = @Schema(implementation = PhysicalAsset.class))}),
                    @ApiResponse(
                            responseCode = "417",
                            description = "Id not found",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response getById(@PathParam(ID) long id) {
        val persisted = service.getByIdWithAssociations(id);
        
        // Fetch and set attributes for the asset
        List<PhysicalAssetAttribute> attributes = attributeService.getAllByAssetId(id);
        persisted.setAttributes(attributes);
        
        return Response.ok(persisted).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete a Asset", tags = {"Physical Asset Metadata"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted a Asset"),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response delete(@PathParam(ID) Long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    @GET
    @Operation(summary = "Get all assets", tags = {"Physical Asset Metadata"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Get all assets",
                            content = {@Content(schema = @Schema(oneOf = PhysicalAsset.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response getAll(@QueryParam("ldmBaseEntityId") Long ldmBaseEntityId,
                           @QueryParam("decEnvironment") Optional<String> decEnvironment) {
        List<PhysicalAsset> assets;
        
        if (ldmBaseEntityId != null) {
            if (decEnvironment.isPresent()) {
                DecEnvironment platform = DecEnvironment.valueOf(decEnvironment.get());
                assets = service.getAllWithAssociationsByLdmIdAndPlatform(ldmBaseEntityId, platform);
            } else {
                assets = service.getAllWithAssociationsByLdmId(ldmBaseEntityId);
            }
        } else {
            assets = service.getAllWithAssociations();
        }
        
        // Fetch and set attributes for each asset
        assets.forEach(asset -> {
            List<PhysicalAssetAttribute> attributes = attributeService.getAllByAssetId(asset.getId());
            asset.setAttributes(attributes);
        });
        
        return Response.ok(assets).build();
    }
}
