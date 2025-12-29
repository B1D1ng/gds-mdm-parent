package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion;
import com.ebay.behavior.gds.mdm.commonSvc.util.DbUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.AttributeTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.EventTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.TemplateQuestion;
import com.ebay.behavior.gds.mdm.signal.common.model.audit.EventTemplateHistory;
import com.ebay.behavior.gds.mdm.signal.common.model.search.EventSearchBy;
import com.ebay.behavior.gds.mdm.signal.service.EventTemplateService;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import lombok.Getter;
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
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.validateUpdateRequestId;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.TEMPLATE;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.WITH_ASSOCIATIONS;

@Validated
@Path(V1 + TEMPLATE + "/event")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EventTemplateResource extends AbstractAuditLogResource<EventTemplateHistory> {

    @Autowired
    @Getter
    private EventTemplateService service;

    @Context
    private UriInfo uriInfo;

    @POST
    @Operation(summary = "Create a new Event Template")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Create new Event Template",
                    content = {@Content(schema = @Schema(implementation = EventTemplate.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response create(EventTemplate eventTemplate) {
        val persisted = service.create(eventTemplate);
        return created(uriInfo, persisted);
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update an Event Template by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Update Event Template",
                    content = {@Content(schema = @Schema(implementation = EventTemplate.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response update(@PathParam(ID) Long id, EventTemplate event) {
        validateUpdateRequestId(event, id);
        val updated = service.update(event);
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete an Event Template by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted Event Template"),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response delete(@PathParam(ID) Long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get an Event Template by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get Event Template by id",
                    content = {@Content(schema = @Schema(implementation = EventTemplate.class))}),
            @ApiResponse(responseCode = "404", description = "Event Template not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getById(@PathParam(ID) Long id, @QueryParam(WITH_ASSOCIATIONS) @DefaultValue("false") Boolean withAssociations) {
        EventTemplate event;
        if (withAssociations) {
            event = service.getByIdWithAssociations(id);
        } else {
            event = service.getById(id);
        }
        return Response.ok(event).build();
    }

    @GET
    @Operation(summary = "Get all Event Templates based on a search criterion")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get all Event Templates",
                    content = {@Content(schema = @Schema(implementation = Page.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getAll(@NotNull @QueryParam(SEARCH_TERM) String searchTerm,
                           @NotNull @QueryParam(SEARCH_BY) EventSearchBy searchBy,
                           @NotNull @QueryParam(SEARCH_CRITERION) SearchCriterion searchCriterion,
                           @Min(0) @QueryParam(Search.PAGE_NUMBER) @DefaultValue("0") int pageNumber,
                           @Min(1) @Max(2000) @QueryParam(Search.PAGE_SIZE) @DefaultValue("20") int pageSize) {
        val pageable = DbUtils.getAuditablePageable(pageNumber, pageSize);
        val search = new Search(searchBy.name(), searchTerm, searchCriterion, pageable);
        val page = service.getAll(search);

        return Response.ok(page).build();
    }

    @GET
    @Path("/{id}/attributes")
    @Operation(summary = "Get associated attributes of an Event Template")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get associated attributes by Event Template id",
                    content = {@Content(array = @ArraySchema(schema = @Schema(implementation = AttributeTemplate.class)))}),
            @ApiResponse(responseCode = "404", description = "Event Template not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getAttributes(@PathParam(ID) Long id) {
        val attributes = service.getAttributes(id);
        return Response.ok(attributes).build();
    }

    @GET
    @Path("/{id}/questions")
    @Operation(summary = "Get associated questions of an Event Template")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get associated questions by Event Template id",
                    content = {@Content(array = @ArraySchema(schema = @Schema(implementation = TemplateQuestion.class)))}),
            @ApiResponse(responseCode = "404", description = "Event Template not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getQuestions(@PathParam(ID) Long id) {
        val questions = service.getQuestions(id);
        return Response.ok(questions).build();
    }
}

