package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.config.UdcConfiguration;
import com.ebay.behavior.gds.mdm.common.service.udc.UdcQueryBuilder;
import com.ebay.behavior.gds.mdm.commonSvc.util.DbUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.service.StagedUdcSignalService;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
import lombok.val;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.DOMAIN;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.FROM;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SIZE;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.TYPE;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.ELASTICSEARCH;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.METADATA;
import static java.util.Locale.US;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;

@Validated
@Path(V1 + METADATA + ELASTICSEARCH + "/signal")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class StagedUdcSignalResource {

    @Autowired
    private StagedUdcSignalService service;

    @Autowired
    private UdcQueryBuilder queryBuilder;

    @Autowired
    private UdcConfiguration udcConfig;

    @GET
    @Path("/{id}")
    @Operation(summary = "Get a staged Signal by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get staged Signal by id",
                    content = {@Content(schema = @Schema(implementation = StagedSignal.class))}),
            @ApiResponse(responseCode = "404", description = "staged StagedSignal not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getById(@PathParam(ID) Long id) {
        val signal = service.getById(id);
        return Response.ok(signal).build();
    }

    @GET
    @Operation(summary = "Get all staged Signals")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get all Signals",
                    content = {@Content(schema = @Schema(implementation = Page.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getAll(@QueryParam(NAME) String name,
                           @QueryParam(TYPE) String type,
                           @QueryParam(DOMAIN) String domain,
                           @Min(0) @QueryParam(FROM) @DefaultValue("0") int from,
                           @Min(1) @Max(2000) @QueryParam(SIZE) @DefaultValue("20") int pageSize) {
        val pageable = DbUtils.getAuditableEsPageable(from, pageSize);
        if (Objects.isNull(name) && Objects.isNull(type) && Objects.isNull(domain)) {
            val page = service.getAll(udcConfig.getDataSource(), pageable);
            return Response.ok(page).build();
        }

        val queryBuilder = QueryBuilders.boolQuery();
        addMustMatch(name, NAME, queryBuilder);
        addMustMatch(type, TYPE, queryBuilder);
        addMustMatch(domain, DOMAIN, queryBuilder);

        val page = service.search(this.queryBuilder.toSearchSourceBuilder(pageable, queryBuilder));
        return Response.ok(page).build();
    }

    @PUT
    @Path("/search")
    @Operation(summary = "Search for staged Signals based on elasticsearch query")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get all Signals",
                    content = {@Content(schema = @Schema(implementation = Page.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response search(@NotBlank String searchBuilderJson) {
        val page = service.search(searchBuilderJson);
        return Response.ok(page).build();
    }

    private static void addMustMatch(String searchTerm, String searchBy, BoolQueryBuilder queryBuilder) {
        if (Objects.nonNull(searchTerm)) {
            queryBuilder.must(matchQuery(searchBy, searchTerm.toLowerCase(US)));
        }
    }
}