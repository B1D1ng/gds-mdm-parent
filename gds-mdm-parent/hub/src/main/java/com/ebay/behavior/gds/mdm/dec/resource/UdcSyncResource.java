package com.ebay.behavior.gds.mdm.dec.resource;

import com.ebay.behavior.gds.mdm.common.model.IdWithStatus;
import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;
import com.ebay.behavior.gds.mdm.dec.service.udc.UdcSyncService;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.UDC_API;

/**
 * This is for ingesting LDM and Dataset to UDC, which could be used to test udc sync functions independently.
 * In the long-term, after the ingestion is stable, this data sync process should be embedded in current LDM & Dataset creation/update process.
 * And below udc sync APIs should not be exposed.
 */
@Path(V1 + UDC_API)
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UdcSyncResource {

    @Autowired
    private UdcSyncService udcSyncService;

    @PUT
    @Path("/ldms/{id}")
    @Operation(summary = "Sync LDM to UDC", tags = {"UDC Ingestion"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Sync ldm",
                            content = {@Content(schema = @Schema(implementation = IdWithStatus.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response syncLdm(@PathParam(ID) long id) {
        val result = udcSyncService.syncLdm(id);
        return Response.ok(result).build();
    }

    @PUT
    @Path("/ldms/{id}/signal-lineages")
    @Operation(summary = "Sync LDM Signal Lineage to UDC", tags = {"UDC Ingestion"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Sync ldm signal lineage",
                            content = {@Content(schema = @Schema(implementation = IdWithStatus.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response syncLdmSignalLineage(@PathParam(ID) long id, @QueryParam("signalId") Long signalId) {
        val result = udcSyncService.syncSignalLineage(id, signalId);
        return Response.ok(result).build();
    }

    @PUT
    @Path("/datasets/{id}")
    @Operation(summary = "Sync Dataset to UDC", tags = {"UDC Ingestion"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Sync Dataset to UDC",
                            content = {@Content(schema = @Schema(implementation = IdWithStatus.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response syncDataset(@PathParam(ID) long id) {
        val result = udcSyncService.syncDataset(id);
        return Response.ok(result).build();
    }

    @DELETE
    @Path("/{entityType}/{id}")
    @Operation(summary = "Delete entities in UDC", tags = {"UDC Ingestion"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted an entity"),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response delete(@PathParam("entityType") UdcEntityType entityType, @PathParam(ID) String id) {
        udcSyncService.delete(entityType, id);
        return Response.noContent().build();
    }
}