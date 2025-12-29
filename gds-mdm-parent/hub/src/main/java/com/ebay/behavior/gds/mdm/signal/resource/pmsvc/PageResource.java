package com.ebay.behavior.gds.mdm.signal.resource.pmsvc;

import com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.request.SearchIn;
import com.ebay.behavior.gds.mdm.signal.common.service.pmsvc.PmsvcService;
import com.ebay.behavior.gds.mdm.signal.util.ServiceUtils;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RestController;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_BY;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_CRITERION;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_TERM;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;

@Path(V1 + "/metadata/page")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PageResource {

    @Autowired
    private PmsvcService service;

    @GET
    @Path("/{id}")
    @Operation(summary = "Get pmsvc Page by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get pmsvc Page by id",
                    content = {@Content(schema = @Schema(implementation = UnstagedField.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getById(@PathParam(ID) long id) {
        val persisted = service.getPageById(id);
        return Response.ok(persisted).build();
    }

    @GET
    @Operation(summary = "Get pmsvc Pages by a list of ids")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get pmsvc Pages by a list of ids",
                    content = {@Content(schema = @Schema(implementation = UnstagedField.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getByIds(@NotNull @QueryParam("ids") String csvIds) {
        val ids = ServiceUtils.toIdSet(csvIds);
        val persisted = service.getPageByIds(ids);
        return Response.ok(persisted).build();
    }

    @GET
    @Path("/search")
    @Operation(summary = "Get all pmsvc Pages based on a search criterion")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get all pmsvc Pages",
                    content = {@Content(schema = @Schema(implementation = Page.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getAll(@NotNull @QueryParam(SEARCH_TERM) String searchTerm,
                           @NotNull @QueryParam(SEARCH_BY) SearchIn searchBy,
                           @NotNull @QueryParam(SEARCH_CRITERION) SearchCriterion searchCriterion) {
        return Response.ok(service.searchPages(searchTerm, searchBy, searchCriterion)).build();
    }
}
