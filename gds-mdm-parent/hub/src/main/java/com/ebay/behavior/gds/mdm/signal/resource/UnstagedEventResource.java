package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion;
import com.ebay.behavior.gds.mdm.commonSvc.util.DbUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.AttributeTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.EvaluateEventExpressionRequest;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstageRequest;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.UpdateUnstagedEventRequest;
import com.ebay.behavior.gds.mdm.signal.common.model.audit.UnstagedEventHistory;
import com.ebay.behavior.gds.mdm.signal.common.model.search.EventSearchBy;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedEventService;
import com.ebay.behavior.gds.mdm.signal.usecase.UnstageUsecase;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
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
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.Validate;
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
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.DEFINITION;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.WITH_ASSOCIATIONS;

@Slf4j
@Validated
@Path(V1 + DEFINITION + "/event")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UnstagedEventResource extends AbstractAuditLogResource<UnstagedEventHistory> {

    @Autowired
    @Getter
    private UnstagedEventService service;

    @Autowired
    private UnstageUsecase unstageUsecase;

    @Context
    private UriInfo uriInfo;

    @POST
    @Path("/from-template/{templateId}")
    @Operation(summary = "Create new UnstagedEvent from an event template and associate it with UnstagedSignal")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Create UnstagedEvent from a event template and associated with signal",
                    content = {@Content(schema = @Schema(implementation = UnstagedEvent.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response createFromTemplate(@PathParam("templateId") Long templateId, @Valid UnstageRequest request) {
        Validate.isTrue(request.isNonVersioned(), "Versioned templates are not supported");
        Validate.isTrue(request.getSrcEntityId().equals(templateId), "Template id must match with request srcEntityId");
        log.info("Requested to create new event under a signal {} from event template {}", request.getParentId(), templateId);

        val persisted = unstageUsecase.copyEventFromTemplate(request);
        return created(uriInfo, persisted);
    }

    @PATCH
    @Path("/{id}")
    @Operation(summary = "Update an UnstagedEvent by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Update UnstagedEvent",
                    content = {@Content(schema = @Schema(implementation = UnstagedEvent.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response update(@PathParam(ID) Long id, UpdateUnstagedEventRequest request) {
        validateUpdateRequestId(request, id);
        val updated = service.update(request);
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete an UnstagedEvent by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted an UnstagedEvent"),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response delete(@PathParam(ID) Long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get an UnstagedEvent by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get an UnstagedEvent by id",
                    content = {@Content(schema = @Schema(implementation = UnstagedEvent.class))}),
            @ApiResponse(responseCode = "404", description = "UnstagedEvent not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getById(@PathParam(ID) Long id, @QueryParam(WITH_ASSOCIATIONS) @DefaultValue("false") Boolean withAssociations) {
        UnstagedEvent event;
        if (withAssociations) {
            event = service.getByIdWithAssociations(id);
        } else {
            event = service.getById(id);
        }
        return Response.ok(event).build();
    }

    @GET
    @Operation(summary = "Get all Unstaged Events based on a search criterion")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get all Unstaged Events",
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
    @Operation(summary = "Get associated attributes of an UnstagedEvent")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get associated attributes by UnstagedEvent id",
                    content = {@Content(array = @ArraySchema(schema = @Schema(implementation = AttributeTemplate.class)))}),
            @ApiResponse(responseCode = "404", description = "UnstagedEvent not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getAttributes(@PathParam(ID) Long id) {
        val attributes = service.getAttributes(id);
        return Response.ok(attributes).build();
    }

    @PUT
    @Path("/{id}/business-fields")
    @Operation(summary = "Evaluates new expression effect on business fields. "
            + "Returns current business fields with addition of new business fields based on the new expression")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Evaluate new expression effect on business fields. Get current and new event business fields",
                    content = {@Content(array = @ArraySchema(schema = @Schema(implementation = AttributeTemplate.class)))}),
            @ApiResponse(responseCode = "404", description = "UnstagedEvent not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response evaluateExpressionUpdate(@PathParam(ID) Long id, EvaluateEventExpressionRequest request) {
        val response = service.evaluateExpressionUpdate(id, request.expression(), request.expressionType());
        return Response.ok(response).build();
    }
}