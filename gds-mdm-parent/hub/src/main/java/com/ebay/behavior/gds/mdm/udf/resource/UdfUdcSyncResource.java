package com.ebay.behavior.gds.mdm.udf.resource;

import com.ebay.behavior.gds.mdm.common.model.IdWithStatus;
import com.ebay.behavior.gds.mdm.udf.service.udc.UdfUdcSyncService;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import static com.ebay.behavior.gds.mdm.udf.util.UdfUtils.UDC;
import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;

@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(V1 + UDC)
public class UdfUdcSyncResource {
    @Autowired
    private UdfUdcSyncService udfUdcSyncService;

    @PUT
    @Path("/udf/{id}")
    @Operation(summary = "Sync Unstaged UDF to UDC", tags = {"UDC Ingestion"})
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Sync Unstaged UDF",
                            content = {@Content(schema = @Schema(implementation = IdWithStatus.class))}),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
            })
    public Response syncUdf(@PathParam(ID) long id) {
        val result = udfUdcSyncService.udcSyncUdf(id);
        return Response.ok(result).build();
    }
}
