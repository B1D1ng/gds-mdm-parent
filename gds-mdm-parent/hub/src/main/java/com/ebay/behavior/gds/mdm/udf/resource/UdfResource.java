package com.ebay.behavior.gds.mdm.udf.resource;

import com.ebay.behavior.gds.mdm.commonSvc.util.DbUtils;
import com.ebay.behavior.gds.mdm.udf.common.model.Udf;
import com.ebay.behavior.gds.mdm.udf.service.UdfService;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
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
import lombok.NonNull;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.PAGE_NUMBER;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.PAGE_SIZE;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.created;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.csvStringToSet;
import static com.ebay.behavior.gds.mdm.udf.util.UdfUtils.UDFMM;
import static com.ebay.behavior.gds.mdm.udf.util.UdfUtils.WITH_ASSOCIATIONS;
import static com.ebay.behavior.gds.mdm.udf.util.UdfUtils.toIdSet;

@Validated
@Path(V1 + UDFMM + "/udf")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UdfResource {

    @Autowired
    private UdfService service;

    @Context
    private UriInfo uriInfo;

    @POST
    @Operation(summary = "Create new Udf")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created the udf",
                    content = {@Content(schema = @Schema(implementation = Udf.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response create(Udf udf) {
        val names = csvStringToSet(udf.getName());
        val persisted = service.create(udf, names);
        return created(uriInfo, persisted);
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update Udf by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Update the udf by id",
                    content = {@Content(schema = @Schema(implementation = Udf.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response update(@PathParam(ID) Long id, Udf udf) {
        val persisted = service.update(udf, id);
        return Response.ok(persisted).build();
    }

    @GET
    @Path("/ids")
    @Operation(summary = "Get udf by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get Udf",
                    content = {@Content(schema = @Schema(implementation = Udf.class))}),
            @ApiResponse(responseCode = "417", description = "Udf not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getByIds(@NonNull @QueryParam("ids") String id, @QueryParam(WITH_ASSOCIATIONS) @DefaultValue("false") Boolean withAssociations) {
        val ids = toIdSet(id);
        val udfList = service.getByIds(ids, withAssociations);
        return Response.ok(udfList).build();
    }

    @GET
    @Path("/names")
    @Operation(summary = "Get udf by name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get Udf",
                    content = {@Content(schema = @Schema(implementation = Udf.class))}),
            @ApiResponse(responseCode = "417", description = "Udf not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getByNames(@NonNull @QueryParam("names") String name, @QueryParam(WITH_ASSOCIATIONS) @DefaultValue("false") Boolean withAssociations) {
        val names = csvStringToSet(name);
        val udfList = service.getByNames(names, withAssociations);
        return Response.ok(udfList).build();
    }

    @GET
    @Operation(summary = "Get all Udfs based on a search criterion")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get all Udfs",
                    content = {@Content(schema = @Schema(implementation = Page.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getAll(@Min(0) @QueryParam(PAGE_NUMBER) @DefaultValue("0") int pageNumber,
                           @Min(1) @Max(2000) @QueryParam(PAGE_SIZE) @DefaultValue("20") int pageSize) {
        val pageable = DbUtils.getAuditablePageable(pageNumber, pageSize);
        val page = service.getAll(pageable);
        return Response.ok(page).build();
    }
}
