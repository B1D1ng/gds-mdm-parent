package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.config.UdcConfiguration;
import com.ebay.behavior.gds.mdm.commonSvc.util.DbUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedAttribute;
import com.ebay.behavior.gds.mdm.signal.common.service.StagedUdcAttributeService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.FROM;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SIZE;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.ELASTICSEARCH;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.METADATA;

@Validated
@Path(V1 + METADATA + ELASTICSEARCH + "/attribute")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class StagedUdcAttributeResource {

    @Autowired
    private StagedUdcAttributeService service;

    @Autowired
    private UdcConfiguration udcConfig;

    @GET
    @Path("/{id}")
    @Operation(summary = "Get a staged Attribute by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get staged Attribute by id",
                    content = {@Content(schema = @Schema(implementation = UnstagedAttribute.class))}),
            @ApiResponse(responseCode = "404", description = "staged Attribute not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getById(@PathParam(ID) Long id) {
        val attribute = service.getById(id);
        return Response.ok(attribute).build();
    }

    @GET
    @Operation(summary = "Get all staged Attributes")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get all Attributes",
                    content = {@Content(schema = @Schema(implementation = Page.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getAll(@Min(0) @QueryParam(FROM) @DefaultValue("0") int from,
                           @Min(1) @Max(2000) @QueryParam(SIZE) @DefaultValue("20") int pageSize) {
        val pageable = DbUtils.getAuditableEsPageable(from, pageSize);
        val page = service.getAll(udcConfig.getDataSource(), pageable);
        return Response.ok(page).build();
    }

    @PUT
    @Path("/search")
    @Operation(summary = "Search for staged Attributes based on elasticsearch query")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get all Attributes",
                    content = {@Content(schema = @Schema(implementation = Page.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response search(@NotBlank String searchBuilderJson) {
        val page = service.search(searchBuilderJson);
        return Response.ok(page).build();
    }
}