package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.service.PlatformAware;
import com.ebay.behavior.gds.mdm.common.util.ResourceUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedSignal;
import com.ebay.behavior.gds.mdm.signal.service.StagedSignalExportService;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.METADATA;

@Slf4j
@Validated
@Path(V1 + METADATA + "/export/signal")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class StagedSignalExportResource extends PlatformAware {

    @Autowired(required = false)
    private StagedSignalExportService service;

    @POST
    @Operation(summary = "Export signals to HDFS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Exported signals", content = {@Content(schema = @Schema(implementation = StagedSignal.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response export() {
        var env = getStagedEnvironment();
        val hostname = ResourceUtils.getHostName();
        service.export(hostname, env);
        return Response.ok(hostname).build();
    }

    @POST
    @Path("/cleanup")
    @Operation(summary = "Clean up exported signal files in HDFS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cleanup completed"),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ErrorMessageV3.class)))
    })
    public Response cleanupExportedSignals() {
        var countOfFilesDeleted = service.deleteOldSignalFiles();
        log.info("HDFS cleanup completed. Files deleted (older than 1 week): {}", countOfFilesDeleted);

        return Response.noContent().build();
    }
}