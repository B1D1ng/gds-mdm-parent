package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;
import com.ebay.behavior.gds.mdm.common.service.udc.MetadataReadService;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.service.SignalImportService;
import com.ebay.behavior.gds.mdm.signal.service.StagedSignalService;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.DATA_SOURCE;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.created;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.UDC;

@Validated
@Path(V1 + UDC)
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ConditionalOnExpression("${controller.staging-enabled}")
public class StagingUdcInjectionResource {

    @Context
    private UriInfo uriInfo;

    @Autowired
    private MetadataReadService udcReadService;

    @Autowired
    private SignalImportService signalImportService;

    @Autowired
    private StagedSignalService stagedSignalService;

    @POST
    @Operation(summary = "Inject a StagedSignal with all associations onto staging UDC Metadata management system. Import the signal into the DB.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Inject a StagedSignal with all associations onto UDC",
                    content = {@Content(schema = @Schema(implementation = UnstagedEvent.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response injectAndImport(@QueryParam(DATA_SOURCE) String dataSource, UnstagedSignal signal) {
        val gdpSource = UdcDataSourceType.fromValue(dataSource);
        val entityId = signalImportService.injectAndImport(gdpSource, signal);

        return created(uriInfo, entityId);
    }

    @PATCH
    @Operation(summary = "Update a StagedSignal environment to PRODUCTION.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Update a StagedSignal environment to PRODUCTION",
                    content = {@Content(schema = @Schema(implementation = UnstagedEvent.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response updateToProductionEnvironment(VersionedId signalId) {
        stagedSignalService.updateToProductionEnvironment(signalId);
        return Response.ok(signalId).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get an entity from staging UDC (by entity id)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get an entity from the UDC (by entity id)",
                    content = {@Content(schema = @Schema(implementation = UnstagedField.class))}),
            @ApiResponse(responseCode = "404", description = "StagedSignal not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getByEntityId(@PathParam(ID) String entityId) {
        val entity = udcReadService.getEntityById(entityId);
        return Response.ok(entity).build();
    }

    @GET
    @Path("/signal/{id}")
    @Operation(summary = "Get a Signal entity from staging UDC (by Signal id)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get a Signal entity from the UDC (by Signal id)",
                    content = {@Content(schema = @Schema(implementation = UnstagedField.class))}),
            @ApiResponse(responseCode = "404", description = "StagedSignal not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getBySignalId(@PathParam(ID) Long signalId) {
        val entity = udcReadService.getEntityById(UdcEntityType.SIGNAL, signalId);
        return Response.ok(entity).build();
    }
}
