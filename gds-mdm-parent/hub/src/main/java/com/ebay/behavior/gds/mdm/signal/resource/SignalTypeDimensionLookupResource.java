package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.signal.common.model.SignalDimTypeLookup;
import com.ebay.behavior.gds.mdm.signal.service.SignalDimTypeLookupService;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.created;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.csvStringToSet;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.validateUpdateRequestId;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.LOOKUP;

@Validated
@Path(V1 + LOOKUP + "/signal_dimension_type")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SignalTypeDimensionLookupResource {

    @Context
    private UriInfo uriInfo;

    @Autowired
    private SignalDimTypeLookupService service;

    @POST
    @Operation(summary = "Create a new dimension")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Create a new dimension"),
            @ApiResponse(responseCode = "400", description = "Bad request")
    })
    public Response create(SignalDimTypeLookup lookup) {
        val persisted = service.create(lookup);
        return created(uriInfo, persisted);
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update existing dimension")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated the dimension",
                    content = {@Content(schema = @Schema(implementation = SignalDimTypeLookup.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response update(@PathParam(ID) Long id, SignalDimTypeLookup lookup) {
        validateUpdateRequestId(lookup, id);
        val persisted = service.update(lookup);
        return Response.ok(persisted).build();
    }

    @GET
    @Operation(summary = "Search dimensions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get all dimension",
                    content = {@Content(schema = @Schema(implementation = SignalDimTypeLookup.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getAll(@QueryParam(NAME) String names) {
        if (StringUtils.isNotBlank(names)) {
            val lookups = service.getAllByName(csvStringToSet(names));
            return Response.ok(lookups).build();
        }
        return Response.ok(service.getAll()).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get dimension by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get dimension by id",
                    content = {@Content(schema = @Schema(implementation = SignalDimTypeLookup.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getById(@PathParam("id") Long id) {
        return Response.ok(service.getById(id)).build();
    }
}
