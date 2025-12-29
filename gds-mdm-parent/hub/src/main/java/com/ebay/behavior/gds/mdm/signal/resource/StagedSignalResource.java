package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.audit.AuditLogParams;
import com.ebay.behavior.gds.mdm.common.model.audit.AuditMode;
import com.ebay.behavior.gds.mdm.common.model.search.RelationalSearchRequest;
import com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion;
import com.ebay.behavior.gds.mdm.common.service.PlatformAware;
import com.ebay.behavior.gds.mdm.signal.common.model.AbstractStagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.response.SignalApiResponse;
import com.ebay.behavior.gds.mdm.signal.service.MetricsService;
import com.ebay.behavior.gds.mdm.signal.service.PlatformLookupService;
import com.ebay.behavior.gds.mdm.signal.service.StagedFieldService;
import com.ebay.behavior.gds.mdm.signal.service.StagedSignalService;
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
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.Environment.PRODUCTION;
import static com.ebay.behavior.gds.mdm.common.model.Environment.STAGING;
import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType.STAGED;
import static com.ebay.behavior.gds.mdm.common.model.VersionedModel.VERSION;
import static com.ebay.behavior.gds.mdm.common.model.audit.AuditLogParams.MODE;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.DATA_SOURCE;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.ENVIRONMENT;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.PLATFORM;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.METADATA;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.USE_CACHE;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.WITH_ASSOCIATIONS;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.WITH_LATEST_VERSIONS;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.WITH_LEGACY_FORMAT;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.WITH_UNSTAGED_DETAILS;

@Validated
@Path(V1 + METADATA + "/signal")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class StagedSignalResource extends PlatformAware {

    @Autowired
    private StagedSignalService service;

    @Autowired
    private StagedFieldService fieldService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlatformLookupService platformService;

    @Autowired
    private MetricsService metricsService;

    @DELETE
    @Path("/{id}/version/{version}")
    @Operation(summary = "Delete migrated UnstagedSignal and StagedSignal by id and version")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted migrated UnstagedSignal and StagedSignal"),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response deleteMigrated(@PathParam(ID) Long id, @PathParam(VERSION) Integer version) {
        service.deleteMigrated(VersionedId.of(id, version));
        return Response.noContent().build();
    }

    @DELETE
    @Path("/bulk")
    @Operation(summary = "Delete migrated UnstagedSignals and StagedSignals by a set of signalIds (id and version)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted migrated UnstagedSignals and StagedSignals"),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response deleteMigrated(Set<VersionedId> signalIds) {
        service.deleteMigrated(signalIds);
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}/version/{version}")
    @Operation(summary = "Get an StagedSignal by id and version")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get an StagedSignal by id and version",
                    content = {@Content(schema = @Schema(implementation = StagedSignal.class))}),
            @ApiResponse(responseCode = "404", description = "StagedSignal not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getById(@PathParam(ID) Long id,
                            @PathParam(VERSION) Integer version,
                            @QueryParam(WITH_ASSOCIATIONS) @DefaultValue("false") Boolean withAssociations,
                            @QueryParam(WITH_UNSTAGED_DETAILS) @DefaultValue("false") Boolean withUnstagedDetails) {
        StagedSignal signal;
        val signalId = VersionedId.of(id, version);

        if (withAssociations) {
            signal = service.getByIdWithAssociations(signalId);
        } else if (withUnstagedDetails) {
            signal = service.getEnrichedById(signalId);
        } else {
            signal = service.getById(signalId);
        }

        return Response.ok(signal).build();
    }

    @GET
    @Path("/{id}/environment/{environment}")
    @Operation(summary = "Get an StagedSignal by id and environment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get an StagedSignal by id and environment",
                    content = {@Content(schema = @Schema(implementation = StagedSignal.class))}),
            @ApiResponse(responseCode = "404", description = "StagedSignal not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getById(@PathParam(ID) Long id,
                            @PathParam(ENVIRONMENT) Environment env,
                            @QueryParam(WITH_ASSOCIATIONS) @DefaultValue("false") Boolean withAssociations,
                            @QueryParam(WITH_UNSTAGED_DETAILS) @DefaultValue("false") Boolean withUnstagedDetails) {
        val latest = service.getLatestVersionById(id, env);
        return getById(latest.getId(), latest.getVersion(), withAssociations, withUnstagedDetails);
    }

    @GET
    @Path("/{id}/version/{version}/fields")
    @Operation(summary = "Get associated fields of a StagedSignal")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get associated fields by a StagedSignal id",
                    content = {@Content(array = @ArraySchema(schema = @Schema(implementation = StagedField.class)))}),
            @ApiResponse(responseCode = "404", description = "StagedSignal not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getFields(@PathParam(ID) Long id, @PathParam(VERSION) Integer version) {
        val signalId = VersionedId.of(id, version);
        val fields = service.getFields(signalId);
        return Response.ok(fields).build();
    }

    @GET
    @Path("/{id}/version/{version}/field-groups")
    @Operation(summary = "Get associated field groups of an StagedSignal")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get associated field groups by an StagedSignal id",
                    content = {@Content(array = @ArraySchema(schema = @Schema(implementation = StagedField.class)))}),
            @ApiResponse(responseCode = "404", description = "StagedSignal not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getFieldGroups(@PathParam(ID) Long id, @PathParam(VERSION) Integer version) {
        val signalId = VersionedId.of(id, version);
        val fields = fieldService.getAllFieldGroups(signalId);
        return Response.ok(fields).build();
    }

    @GET
    @Path("/{id}/version/{version}/events")
    @Operation(summary = "Get associated events of an StagedSignal")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get associated events by StagedSignal id",
                    content = {@Content(array = @ArraySchema(schema = @Schema(implementation = StagedEvent.class)))}),
            @ApiResponse(responseCode = "404", description = "StagedSignal not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getEvents(@PathParam(ID) Long id, @PathParam(VERSION) Integer version) {
        val signalId = VersionedId.of(id, version);
        val events = service.getEvents(signalId);
        return Response.ok(events).build();
    }

    @GET
    @Operation(summary = "Get all staged Signals for the specified platform. "
            + "To be used with Flink jobs and service components. "
            + "Return cached data, if required. No pagination support.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get all staged Signals for the specified platform",
                    content = {@Content(array = @ArraySchema(schema = @Schema(implementation = StagedSignal.class)))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getAll(@NotNull @QueryParam(PLATFORM) String platformName,
                           @NotNull @QueryParam(USE_CACHE) @DefaultValue("true") Boolean useCache,
                           @NotNull @QueryParam(WITH_LEGACY_FORMAT) @DefaultValue("false") Boolean withLegacyFormat,
                           @NotNull @QueryParam(WITH_LATEST_VERSIONS) @DefaultValue("true") Boolean withLatestVersions) {
        try {
            val dataSource = STAGED;
            var env = getStagedEnvironment();
            Set<? extends AbstractStagedSignal> signals;

            val platformId = platformService.getPlatformId(platformName);

            if (useCache) {
                if (withLatestVersions && PRODUCTION == env) { // latest PRODUCTION versions
                    signals = service.getAllProductionLatestVersionsCached(dataSource, platformId);
                } else if (withLatestVersions) { // latest STAGING versions
                    signals = service.getAllStagingLatestVersionsCached(dataSource, platformId);
                } else { // all versions
                    signals = service.getAllVersionsCached(env, dataSource, platformId);
                }

                if (withLegacyFormat) {
                    val legacySignals = service.toSignalDefinitions(signals);
                    metricsService.getSignalSuccessCounter().increment();
                    return Response.ok(legacySignals).build();
                } else {
                    metricsService.getSignalSuccessCounter().increment();
                    return Response.ok(signals).build();
                }
            }

            // without cache
            if (withLatestVersions && PRODUCTION == env) { // latest PRODUCTION versions
                signals = service.getAllProductionLatestVersions(dataSource, platformId);
            } else if (withLatestVersions) { // latest STAGING versions
                signals = service.getAllStagingLatestVersions(dataSource, platformId);
            } else { // all versions
                signals = service.getAllVersions(env, dataSource, platformId);
            }

            if (!withLegacyFormat) {
                metricsService.getSignalSuccessCounter().increment();
                return Response.ok(signals).build();
            }

            val legacySignals = new HashSet<>(service.toSignalDefinitions(signals));
            metricsService.getSignalSuccessCounter().increment();
            return Response.ok(legacySignals).build();
        } catch (Exception e) {
            metricsService.getSignalErrorCounter().increment();
            throw e;
        }
    }

    @PUT
    @Operation(summary = "Search for staged Signals based on search query specification")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search for staged Signals based on search query specification",
                    content = {@Content(schema = @Schema(implementation = SignalApiResponse.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response search(@QueryParam(WITH_UNSTAGED_DETAILS) @DefaultValue("false") Boolean withUnstagedDetails,
                           @QueryParam(WITH_LEGACY_FORMAT) @DefaultValue("false") Boolean withLegacyFormat,
                           @NotNull @QueryParam(WITH_LATEST_VERSIONS) @DefaultValue("true") Boolean withLatestVersions,
                           @Valid RelationalSearchRequest request) {
        enrichRequest(request);
        var env = getStagedEnvironment();
        Page<? extends AbstractStagedSignal> page;

        if (withLegacyFormat) {
            if (withLatestVersions && PRODUCTION == env) { // latest PRODUCTION versions
                page = service.searchProductionLatestVersions(false, request);
            } else if (withLatestVersions) { // latest STAGING versions
                page = service.searchStagingLatestVersions(false, request);
            } else { // all versions
                page = service.searchAllVersions(false, request);
            }
            val legacyPage = service.toSignalDefinitionsPage(page);
            return Response.ok(legacyPage).build();
        }

        if (withLatestVersions && PRODUCTION == env) { // latest PRODUCTION versions
            page = service.searchProductionLatestVersions(withUnstagedDetails, request);
        } else if (withLatestVersions) { // latest STAGING versions
            page = service.searchStagingLatestVersions(withUnstagedDetails, request);
        } else { // all versions
            page = service.searchAllVersions(withUnstagedDetails, request);
        }

        return Response.ok(page).build();
    }

    @GET
    @Path("/{id}/auditLog")
    @Operation(summary = "Get the audit log of all production StagedSignal versions by signal id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get audit log", content = {@Content(schema = @Schema(implementation = List.class))}),
            @ApiResponse(responseCode = "417", description = "Id/ver not found", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getAuditLog(@PathParam(ID) Long id,
                                @QueryParam(MODE) @DefaultValue("BASIC") AuditMode mode,
                                @NotNull @QueryParam(DATA_SOURCE) @DefaultValue("cjs") String dataSource) {
        val gdpSource = UdcDataSourceType.fromValue(dataSource);
        val auditParams = AuditLogParams.ofNonVersioned(id, mode);
        val log = service.getAuditLog(gdpSource, auditParams);
        val json = AuditUtils.serializeAuditRecords(objectMapper, log);
        return Response.ok(json).build();
    }

    /**
     * Adds an environment filter to the search request.
     */
    private void enrichRequest(RelationalSearchRequest request) {
        // add default sort
        if (request.getSort() == null) {
            request.setSort(new RelationalSearchRequest.SortRequest(ID, Sort.Direction.ASC));
        }

        // add current environment filter, if absent
        val maybeEnvFilter = CollectionUtils.emptyIfNull(request.getFilters()).stream()
                .filter(filter -> ENVIRONMENT.equalsIgnoreCase(filter.getField()))
                .findFirst();

        if (maybeEnvFilter.isPresent()) {
            return;
        }

        var env = getStagedEnvironment();
        if (STAGING.equals(env)) {
            return; // there is no need to add env filter on staging since staging signals can have both in staging and production env
        }

        val currEnvFilter = new RelationalSearchRequest.Filter(ENVIRONMENT, SearchCriterion.EXACT_MATCH_IGNORE_CASE, env.name()); // must ignore case for enums
        if (request.getFilters() == null) {
            request.setFilters(List.of(currEnvFilter));
        } else {
            request.getFilters().add(currEnvFilter);
        }
    }
}
