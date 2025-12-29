package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.model.audit.UnstagedSignalHistory;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedFieldGroupService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedSignalService;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.model.VersionedModel.VERSION;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.validateVersionedUpdateRequest;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.DEFINITION;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.WITH_ASSOCIATIONS;

@Slf4j
@Validated
@Path(V1 + DEFINITION + "/signal/{id}/version/{version}")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UnstagedSignalAnyVersionResource extends AbstractVersionedAuditLogResource<UnstagedSignalHistory> {

    @Getter
    @Autowired
    private UnstagedSignalService service;

    @Autowired
    private UnstagedFieldGroupService fieldGroupService;

    @PUT
    @Operation(summary = "Update an UnstagedSignal by id and version")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Update an UnstagedSignal",
                    content = {@Content(schema = @Schema(implementation = SignalTemplate.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})})
    public Response update(@PathParam(ID) Long id,
                           @PathParam(VERSION) Integer version,
                           UnstagedSignal signal) {
        validateVersionedUpdateRequest(signal, id, version);
        val updated = service.update(signal);
        return Response.ok(updated).build();
    }

    @GET
    @Operation(summary = "Get an UnstagedSignal by id and version")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get an UnstagedSignal by id",
                    content = {@Content(schema = @Schema(implementation = UnstagedSignal.class))}),
            @ApiResponse(responseCode = "404", description = "UnstagedSignal not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getById(@PathParam(ID) Long id,
                            @PathParam(VERSION) Integer version,
                            @QueryParam(WITH_ASSOCIATIONS) @DefaultValue("false") Boolean withAssociations) {
        UnstagedSignal signal;
        val signalId = VersionedId.of(id, version);

        if (withAssociations) {
            signal = service.getByIdWithAssociations(signalId);
        } else {
            signal = service.getById(signalId);
        }
        return Response.ok(signal).build();
    }

    @GET
    @Path("/fields")
    @Operation(summary = "Get associated fields of an UnstagedSignal")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get associated fields by an UnstagedSignal id",
                    content = {@Content(array = @ArraySchema(schema = @Schema(implementation = UnstagedField.class)))}),
            @ApiResponse(responseCode = "404", description = "UnstagedSignal not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getFields(@PathParam(ID) Long id, @PathParam(VERSION) Integer version) {
        val signalId = VersionedId.of(id, version);
        val fields = service.getFields(signalId);
        return Response.ok(fields).build();
    }

    @GET
    @Path("/field-groups")
    @Operation(summary = "Get associated field groups of an UnstagedSignal")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get associated field groups by an UnstagedSignal id",
                    content = {@Content(array = @ArraySchema(schema = @Schema(implementation = UnstagedField.class)))}),
            @ApiResponse(responseCode = "404", description = "UnstagedSignal not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getFieldGroups(@PathParam(ID) Long id, @PathParam(VERSION) Integer version) {
        val signalId = VersionedId.of(id, version);
        val fields = fieldGroupService.getAll(signalId);
        return Response.ok(fields).build();
    }

    @GET
    @Path("/events")
    @Operation(summary = "Get associated events of an UnstagedSignal")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get associated events by UnstagedSignal id",
                    content = {@Content(array = @ArraySchema(schema = @Schema(implementation = UnstagedEvent.class)))}),
            @ApiResponse(responseCode = "404", description = "UnstagedSignal not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getEvents(@PathParam(ID) Long id, @PathParam(VERSION) Integer version) {
        val signalId = VersionedId.of(id, version);
        val events = service.getEvents(signalId);
        return Response.ok(events).build();
    }
}