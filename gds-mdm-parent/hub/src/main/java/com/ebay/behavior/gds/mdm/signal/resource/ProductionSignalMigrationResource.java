package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.service.PlatformAware;
import com.ebay.behavior.gds.mdm.common.util.ResourceUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.migration.SignalMigrationStatus;
import com.ebay.behavior.gds.mdm.signal.service.migration.SignalMigrationService;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.PLATFORM;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;

@Slf4j
@Validated
@Path(V1 + "/migration/signal")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ConditionalOnExpression("${controller.production-enabled}")
public class ProductionSignalMigrationResource extends PlatformAware {

    public record SignalMigrationResponse(String hostname, List<SignalMigrationStatus> statuses) {
    }

    @Autowired
    private SignalMigrationService migrationService;

    @POST
    @Path("/bulk")
    @Operation(summary = "migrate all")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "migrate all",
                    content = {@Content(array = @ArraySchema(schema = @Schema(implementation = SignalMigrationStatus.class)))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response migrateAll(@NotNull @QueryParam(PLATFORM) String platformName,
                               @QueryParam("skipRemote") @DefaultValue("false") Boolean skipRemote) {
        val env = getStagedEnvironment();
        val hostname = ResourceUtils.getHostName();

        // migrateAll executed in different thread
        migrationService.migrateAll(platformName, skipRemote, env).whenComplete((statuses, th) -> {
            if (Objects.nonNull(th)) { // failure case
                log.error("Migration failed. Please see the logs under the host: " + hostname, th);
            } else {
                log.info("Migration completed successfully. The logs are available under the host: " + hostname);
            }
        });

        val message = "Migration job is running. Please see the logs under the host: " + hostname;
        return Response.ok(message).build();
    }

    @POST
    @Path("/name/{name}")
    @Operation(summary = "migrate signal")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "migrate signal",
                    content = {@Content(array = @ArraySchema(schema = @Schema(implementation = SignalMigrationStatus.class)))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response migrateSignal(@PathParam(NAME) String signalName,
                                  @NotNull @QueryParam(PLATFORM) String platformName,
                                  @QueryParam("skipRemote") @DefaultValue("false") Boolean skipRemote) {
        val env = getStagedEnvironment();
        val hostname = ResourceUtils.getHostName();
        val statuses = migrationService.migrate(signalName, platformName, skipRemote, env);
        val response = new SignalMigrationResponse(hostname, statuses);
        return Response.ok(response).build();
    }
}
