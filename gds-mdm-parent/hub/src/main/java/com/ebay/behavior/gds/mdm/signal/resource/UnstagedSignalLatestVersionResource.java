package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.audit.AuditLogParams;
import com.ebay.behavior.gds.mdm.common.model.audit.AuditMode;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstageRequest;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.model.UpdateUnstagedSignalRequest;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedFieldGroupService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedSignalService;
import com.ebay.behavior.gds.mdm.signal.usecase.UnstageUsecase;
import com.ebay.behavior.gds.mdm.signal.util.AuditUtils;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.model.VersionedModel.VERSION;
import static com.ebay.behavior.gds.mdm.common.model.audit.AuditLogParams.MODE;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.created;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.validateUpdateRequestId;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.DEFINITION;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.WITH_ASSOCIATIONS;

@Slf4j
@Validated
@Path(V1 + DEFINITION + "/signal")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UnstagedSignalLatestVersionResource {

    @Getter
    @Autowired
    private UnstagedSignalService signalService;

    @Autowired
    private UnstagedFieldGroupService fieldGroupService;

    @Autowired
    private UnstageUsecase unstageUsecase;

    @Autowired
    private ObjectMapper objectMapper;

    @Context
    private UriInfo uriInfo;

    @POST
    @Operation(summary = "Create new UnstagedSignal")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Create new UnstagedSignal",
                    content = {@Content(schema = @Schema(implementation = SignalTemplate.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})})
    public Response create(UnstagedSignal signal) {
        val persisted = signalService.create(signal);
        return created(uriInfo, persisted);
    }

    @POST
    @Path("/from-template/{templateId}")
    @Operation(summary = "Create new UnstagedSignal and its associated records from a Signal template")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Create UnstagedSignal from a template",
                    content = {@Content(schema = @Schema(implementation = UnstagedSignal.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response createFromTemplate(@PathParam("templateId") Long templateId, @Valid UnstageRequest request) {
        Validate.isTrue(request.isNonVersioned(), "Versioned templates are not supported");
        Validate.isTrue(request.getSrcEntityId().equals(templateId), "Template id must match with request srcEntityId");

        val planId = request.getParentId();
        log.info("Requested to create new signal from template {} under a plan {}", templateId, planId);

        val persisted = unstageUsecase.copySignalFromTemplate(request);
        return created(uriInfo, persisted);
    }

    @POST
    @Path("/from-signal/{signalId}/version/{version}")
    @Operation(summary = "Create new UnstagedSignal and its associated records from an UnstagedSignal")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Create UnstagedSignal from another UnstagedSignal",
                    content = {@Content(schema = @Schema(implementation = UnstagedSignal.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response createFromSignal(@PathParam("signalId") Long signalId,
                                     @PathParam(VERSION) Integer version,
                                     @Valid UnstageRequest request) {
        Validate.isTrue(request.isVersioned(), "Non-versioned signals are not supported");
        Validate.isTrue(request.getSrcEntityId().equals(signalId), "Signal id must match with request srcEntityId");
        Validate.isTrue(request.getSrcVersion().equals(version), "Signal id must match with request srcVersion");

        val planId = request.getParentId();
        log.info("Requested to create new signal from signal [id={}, ver ={}] under a plan {}", request.getSrcEntityId(), request.getSrcVersion(), planId);

        val persisted = unstageUsecase.copySignalFromUnstaged(request);
        return created(uriInfo, persisted);
    }

    @PATCH
    @Path("/{id}")
    @Operation(summary = "Update a latest version of an UnstagedSignal by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Update a latest version of an UnstagedSignal",
                    content = {@Content(schema = @Schema(implementation = UnstagedSignal.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response updateLatestVersion(@PathParam(ID) Long id, UpdateUnstagedSignalRequest request) {
        validateUpdateRequestId(request, id);
        val updated = signalService.updateLatestVersion(request);
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete a latest version of an UnstagedSignal by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted the latest version of an UnstagedSignal"),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response deleteLatestVersion(@PathParam(ID) Long id) {
        signalService.deleteLatestVersion(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get a latest version of an UnstagedSignal by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get an UnstagedSignal by id",
                    content = {@Content(schema = @Schema(implementation = UnstagedSignal.class))}),
            @ApiResponse(responseCode = "404", description = "UnstagedSignal not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getByIdAndLatestVersion(@PathParam(ID) Long id,
                                            @QueryParam(WITH_ASSOCIATIONS) @DefaultValue("false") Boolean withAssociations) {
        UnstagedSignal signal;
        val signalId = ofLatestVersion(id);

        if (withAssociations) {
            signal = signalService.getByIdWithAssociations(signalId);
        } else {
            signal = signalService.getById(signalId);
        }
        return Response.ok(signal).build();
    }

    @GET
    @Path("/{id}/fields")
    @Operation(summary = "Get associated fields of a latest version of an UnstagedSignal")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get associated fields by UnstagedSignal id",
                    content = {@Content(array = @ArraySchema(schema = @Schema(implementation = UnstagedField.class)))}),
            @ApiResponse(responseCode = "404", description = "UnstagedSignal not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getFields(@PathParam(ID) Long id) {
        val signalId = ofLatestVersion(id);
        val fields = signalService.getFields(signalId);
        return Response.ok(fields).build();
    }

    @GET
    @Path("/{id}/field-groups")
    @Operation(summary = "Get associated field groups of a latest version of an UnstagedSignal")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get associated field groups by UnstagedSignal id",
                    content = {@Content(array = @ArraySchema(schema = @Schema(implementation = UnstagedField.class)))}),
            @ApiResponse(responseCode = "404", description = "UnstagedSignal not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getFieldGroups(@PathParam(ID) Long id) {
        val signalId = ofLatestVersion(id);
        val fields = fieldGroupService.getAll(signalId);
        return Response.ok(fields).build();
    }

    @GET
    @Path("/{id}/events")
    @Operation(summary = "Get associated events of a latest version of an UnstagedSignal")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get associated events by UnstagedSignal id",
                    content = {@Content(array = @ArraySchema(schema = @Schema(implementation = UnstagedEvent.class)))}),
            @ApiResponse(responseCode = "404", description = "UnstagedSignal not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getEvents(@PathParam(ID) Long id) {
        val signalId = ofLatestVersion(id);
        val events = signalService.getEvents(signalId);
        return Response.ok(events).build();
    }

    @GET
    @Path("/{id}/auditLog")
    @Operation(summary = "Get the audit log of an entity version")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get audit log", content = {@Content(schema = @Schema(implementation = List.class))}),
            @ApiResponse(responseCode = "417", description = "Id/ver not found", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getAuditLog(@PathParam(ID) Long id,
                                @QueryParam(MODE) @DefaultValue("BASIC") AuditMode mode) {
        val version = signalService.getLatestVersion(id);
        val auditParams = AuditLogParams.ofVersioned(id, version, mode);
        val log = getSignalService().getAuditLog(auditParams);
        val json = AuditUtils.serializeAuditRecords(objectMapper, log);
        return Response.ok(json).build();
    }

    private VersionedId ofLatestVersion(long id) {
        return VersionedId.of(id, signalService.getLatestVersion(id));
    }
}