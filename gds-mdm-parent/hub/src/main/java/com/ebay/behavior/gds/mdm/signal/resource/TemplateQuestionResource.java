package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.signal.common.model.CreateTemplateQuestionRequest;
import com.ebay.behavior.gds.mdm.signal.common.model.TemplateQuestion;
import com.ebay.behavior.gds.mdm.signal.service.TemplateQuestionService;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.created;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.validateUpdateRequestId;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.TEMPLATE;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.WITH_ASSOCIATIONS;

@Validated
@Path(V1 + TEMPLATE + "/question")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TemplateQuestionResource {

    @Autowired
    private TemplateQuestionService service;

    @Context
    private UriInfo uriInfo;

    @POST
    @Operation(summary = "Create a new TemplateQuestion and associate with an EventTemplate")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Create new TemplateQuestion",
                    content = {@Content(schema = @Schema(implementation = CreateTemplateQuestionRequest.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response create(CreateTemplateQuestionRequest request) {
        val question = request.question();
        val persisted = service.create(question, request.eventTemplateIds());
        return created(uriInfo, persisted);
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update existing TemplateQuestion")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Update TemplateQuestion",
                    content = {@Content(schema = @Schema(implementation = TemplateQuestion.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response update(@PathParam(ID) Long id, TemplateQuestion question) {
        validateUpdateRequestId(question, id);
        val persisted = service.update(question);
        return Response.ok(persisted).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete an TemplateQuestion")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted TemplateQuestion"),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response delete(@PathParam(ID) Long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get an TemplateQuestion by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get TemplateQuestion by id",
                    content = {@Content(schema = @Schema(implementation = TemplateQuestion.class))}),
            @ApiResponse(responseCode = "404", description = "TemplateQuestion not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getById(@PathParam(ID) Long id, @QueryParam(WITH_ASSOCIATIONS) @DefaultValue("false") Boolean withAssociations) {
        TemplateQuestion question;
        if (withAssociations) {
            question = service.getByIdWithAssociations(id);
        } else {
            question = service.getById(id);
        }
        return Response.ok(question).build();
    }

    @PUT
    @Path("/{id}/event/{eventId}")
    @Operation(summary = "Associate an existing TemplateQuestion with an existing EventTemplate")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Associate TemplateQuestion to EventTemplate",
                    content = {@Content(schema = @Schema(implementation = TemplateQuestion.class))}),
            @ApiResponse(responseCode = "417", description = "Id or eventTemplateId not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response associate(@PathParam(ID) Long id, @PathParam("eventId") Long eventId) {
        val persisted = service.createEventMapping(id, eventId);
        return Response.ok(persisted.getQuestion()).build();
    }
}