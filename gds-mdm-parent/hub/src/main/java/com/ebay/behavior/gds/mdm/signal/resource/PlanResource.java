package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.util.DbUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.Plan;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.model.audit.PlanHistory;
import com.ebay.behavior.gds.mdm.signal.common.model.search.PlanSearchBy;
import com.ebay.behavior.gds.mdm.signal.service.OwnershipService;
import com.ebay.behavior.gds.mdm.signal.service.PlanService;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import lombok.Getter;
import lombok.val;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.DOMAIN;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.PAGE_NUMBER;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.PAGE_SIZE;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.PLATFORM;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_BY;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_TERM;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.created;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.getRequestUser;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.validateUpdateRequestId;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.DEFINITION;
import static com.ebay.behavior.gds.mdm.signal.util.ValidationUtils.validatePlanSearchParams;

@Validated
@Path(V1 + DEFINITION + "/plan")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PlanResource extends AbstractAuditLogResource<PlanHistory> {

    @Autowired
    @Getter
    private PlanService service;

    @Autowired
    private OwnershipService ownershipService;

    @Context
    private UriInfo uriInfo;

    @POST
    @Operation(summary = "Create new Plan")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created the Plan", content = {@Content(schema = @Schema(implementation = Plan.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response create(Plan plan) {
        val persisted = service.create(plan);
        return created(uriInfo, persisted);
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update existing plan")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated the Plan", content = {@Content(schema = @Schema(implementation = Plan.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response update(@PathParam(ID) Long id, Plan plan) {
        validateUpdateRequestId(plan, id);
        val persisted = service.update(plan);
        return Response.ok(persisted).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get plan by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get Plan", content = {@Content(schema = @Schema(implementation = Plan.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getById(@PathParam(ID) long id) {
        val persisted = service.getById(id);
        val user = getRequestUser();
        ownershipService.setUserPermissions(persisted, user);
        return Response.ok(persisted).build();
    }

    @GET
    @Operation(summary = "Search plans")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search Plans", content = {@Content(schema = @Schema(implementation = Page.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getAll(@QueryParam(Search.OWNED_BY_ME) boolean ownedByMe,
                           @QueryParam(DOMAIN) String domain,
                           @QueryParam(PLATFORM) Long platformId,
                           @QueryParam(SEARCH_TERM) String searchTerm,
                           @QueryParam(SEARCH_BY) PlanSearchBy searchBy,
                           @Min(0) @QueryParam(PAGE_NUMBER) @DefaultValue("0") int pageNumber,
                           @Min(1) @Max(2000) @QueryParam(PAGE_SIZE) @DefaultValue("20") int pageSize) {
        if (searchTerm != null || searchBy != null) {
            validatePlanSearchParams(searchBy, searchTerm, ownedByMe);
        }

        val user = getRequestUser();
        val pageable = DbUtils.getAuditablePageable(pageNumber, pageSize);
        Optional<String> maybeUser = Optional.empty();

        if (BooleanUtils.isTrue(ownedByMe)) {
            maybeUser = Optional.ofNullable(user);
        }

        val page = service.getAll(maybeUser, searchBy, searchTerm, Optional.ofNullable(domain), Optional.ofNullable(platformId), pageable);
        page.get().forEach(plan -> ownershipService.setUserPermissions(plan, user));
        return Response.ok(page).build();
    }

    @GET
    @Path("/{id}/signals")
    @Operation(summary = "Get associated UnstagedSignals of a Plan")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get associated UnstagedSignals by Plan id",
                    content = {@Content(array = @ArraySchema(schema = @Schema(implementation = UnstagedSignal.class)))}),
            @ApiResponse(responseCode = "404", description = "Plan not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getSignals(@PathParam(ID) Long id) {
        val signals = service.getSignals(id);
        return Response.ok(signals).build();
    }
}
