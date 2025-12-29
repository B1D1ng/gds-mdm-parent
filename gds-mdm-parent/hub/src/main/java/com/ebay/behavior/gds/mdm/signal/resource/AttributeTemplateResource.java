package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion;
import com.ebay.behavior.gds.mdm.commonSvc.util.DbUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.AttributeTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.search.AttributeSearchBy;
import com.ebay.behavior.gds.mdm.signal.service.AttributeTemplateService;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
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
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_BY;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_CRITERION;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_TERM;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.created;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.TEMPLATE;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.WITH_ASSOCIATIONS;

@Validated
@Path(V1 + TEMPLATE + "/attribute")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AttributeTemplateResource {

    @Autowired
    private AttributeTemplateService service;

    @Context
    private UriInfo uriInfo;

    @POST
    @Operation(summary = "Create a new UnstagedAttribute Template and associate with an Event Template")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Create new UnstagedAttribute Template",
                    content = {@Content(schema = @Schema(implementation = AttributeTemplate.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response create(AttributeTemplate attribute) {
        val persisted = service.create(attribute);
        return created(uriInfo, persisted);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete an UnstagedAttribute Template by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted UnstagedAttribute Template"),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response delete(@PathParam(ID) Long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get an UnstagedAttribute Template by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get UnstagedAttribute Template by id",
                    content = {@Content(schema = @Schema(implementation = AttributeTemplate.class))}),
            @ApiResponse(responseCode = "404", description = "UnstagedAttribute Template not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getById(@PathParam(ID) Long id, @QueryParam(WITH_ASSOCIATIONS) @DefaultValue("false") Boolean withAssociations) {
        AttributeTemplate attribute;
        if (withAssociations) {
            attribute = service.getByIdWithAssociations(id);
        } else {
            attribute = service.getById(id);
        }
        return Response.ok(attribute).build();
    }

    @GET
    @Operation(summary = "Get all UnstagedAttribute Templates based on a search criterion")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get all UnstagedAttribute Templates",
                    content = {@Content(schema = @Schema(implementation = Page.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getAll(@NotNull @QueryParam(SEARCH_TERM) String searchTerm,
                           @NotNull @QueryParam(SEARCH_BY) AttributeSearchBy searchBy,
                           @NotNull @QueryParam(SEARCH_CRITERION) SearchCriterion searchCriterion,
                           @Min(0) @QueryParam(Search.PAGE_NUMBER) @DefaultValue("0") int pageNumber,
                           @Min(1) @Max(2000) @QueryParam(Search.PAGE_SIZE) @DefaultValue("20") int pageSize) {
        val pageable = DbUtils.getAuditablePageable(pageNumber, pageSize);
        val search = new Search(searchBy.name(), searchTerm, searchCriterion, pageable);
        val page = service.getAll(search);

        return Response.ok(page).build();
    }
}