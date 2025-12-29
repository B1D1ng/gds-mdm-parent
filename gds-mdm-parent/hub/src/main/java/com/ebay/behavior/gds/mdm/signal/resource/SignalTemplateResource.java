package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion;
import com.ebay.behavior.gds.mdm.commonSvc.util.DbUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.EventTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.FieldTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.TemplateQuestion;
import com.ebay.behavior.gds.mdm.signal.common.model.audit.SignalTemplateHistory;
import com.ebay.behavior.gds.mdm.signal.common.model.search.SignalSearchBy;
import com.ebay.behavior.gds.mdm.signal.service.SignalTemplateService;
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
@Path(V1 + TEMPLATE + "/signal")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SignalTemplateResource extends AbstractAuditLogResource<SignalTemplateHistory> {

    @Autowired
    @Getter
    private SignalTemplateService service;

    @Context
    private UriInfo uriInfo;

    @POST
    @Operation(summary = "Create a new Signal Template")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Create new Signal Template",
                    content = {@Content(schema = @Schema(implementation = SignalTemplate.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})})
    public Response create(SignalTemplate signalTemplate) {
        val persisted = service.create(signalTemplate);
        return created(uriInfo, persisted);
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update a Signal Template by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Update Signal Template",
                    content = {@Content(schema = @Schema(implementation = SignalTemplate.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})})
    public Response update(@PathParam(ID) Long id, SignalTemplate signal) {
        validateUpdateRequestId(signal, id);
        val updated = service.update(signal);
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete a Signal Template by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted Signal Template"),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})})
    public Response delete(@PathParam(ID) Long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get a Signal Template by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get Signal Template by id",
                    content = {@Content(schema = @Schema(implementation = SignalTemplate.class))}),
            @ApiResponse(responseCode = "404", description = "Signal Template not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})})
    public Response getById(@PathParam(ID) Long id, @QueryParam(WITH_ASSOCIATIONS) @DefaultValue("false") boolean withAssociations) {
        if (withAssociations) {
            val signal = service.getByIdWithAssociations(id);
            return Response.ok(signal).build();
        }
        val signal = service.getById(id);
        return Response.ok(signal).build();
    }

    @GET
    @Operation(summary = "Get all Signal Templates based on a search criterion")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get all Signal Templates",
                    content = {@Content(schema = @Schema(implementation = Page.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})})
    public Response getAll(@NotNull @QueryParam(SEARCH_TERM) String searchTerm,
                           @NotNull @QueryParam(SEARCH_BY) SignalSearchBy searchBy,
                           @NotNull @QueryParam(SEARCH_CRITERION) SearchCriterion searchCriterion,
                           @Min(0) @QueryParam(Search.PAGE_NUMBER) @DefaultValue("0") int pageNumber,
                           @Min(1) @Max(2000) @QueryParam(Search.PAGE_SIZE) @DefaultValue("20") int pageSize) {
        val pageable = DbUtils.getAuditablePageable(pageNumber, pageSize);
        val search = new Search(searchBy.name(), searchTerm, searchCriterion, pageable);
        val page = service.getAll(search);
        return Response.ok(page).build();
    }

    @GET
    @Path("/{id}/fields")
    @Operation(summary = "Get associated fields of a Signal Template")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get associated fields by Signal Template id",
                    content = {@Content(array = @ArraySchema(schema = @Schema(implementation = FieldTemplate.class)))}),
            @ApiResponse(responseCode = "404", description = "Signal Template not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})})
    public Response getFields(@PathParam(ID) Long id) {
        val fields = service.getFields(id);
        return Response.ok(fields).build();
    }

    @GET
    @Path("/{id}/events")
    @Operation(summary = "Get associated events of a Signal Template")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get associated events by Signal Template id",
                    content = {@Content(array = @ArraySchema(schema = @Schema(implementation = EventTemplate.class)))}),
            @ApiResponse(responseCode = "404", description = "Signal Template not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})})
    public Response getEvents(@PathParam(ID) Long id) {
        val events = service.getEvents(id);
        return Response.ok(events).build();
    }

    @GET
    @Path("/{id}/questions")
    @Operation(summary = "Get associated questions of a Signal Template")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get associated questions by Signal Template id",
                    content = {@Content(array = @ArraySchema(schema = @Schema(implementation = TemplateQuestion.class)))}),
            @ApiResponse(responseCode = "404", description = "Event Template not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getQuestions(@PathParam(ID) Long id) {
        val questions = service.getQuestions(id);
        return Response.ok(questions).build();
    }

    @GET
    @Path("/types")
    @Operation(summary = "Get all Signal Template types filtered by platformId")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get all signal template types filtered by platformId",
                    content = {@Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))})})
    public Response getTypes(@NotNull @QueryParam("platform") Long platformId) {
        val types = service.getTypesByPlatformId(platformId);
        return Response.ok(types).build();
    }
}
