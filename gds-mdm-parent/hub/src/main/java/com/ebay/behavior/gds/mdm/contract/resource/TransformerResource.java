package com.ebay.behavior.gds.mdm.contract.resource;

import com.ebay.behavior.gds.mdm.contract.config.ContractGovernanceConfiguration;
import com.ebay.behavior.gds.mdm.contract.model.Filter;
import com.ebay.behavior.gds.mdm.contract.model.Transformation;
import com.ebay.behavior.gds.mdm.contract.model.Transformer;
import com.ebay.behavior.gds.mdm.contract.service.TransformerService;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.created;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.getRequestUser;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.validateUpdateRequestId;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.CMM;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.DEFINITION;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.WITH_ASSOCIATIONS;
import static com.ebay.behavior.gds.mdm.contract.util.ValidationUtils.validateModerator;

@Validated
@Path(V1 + CMM + DEFINITION + "/transformer")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TransformerResource {

    @Autowired
    private TransformerService service;

    @Autowired
    private ContractGovernanceConfiguration configuration;

    @Context
    private UriInfo uriInfo;

    @POST
    @Operation(summary = "Create new Transformer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created the Transformer",
                    content = {@Content(schema = @Schema(implementation = Transformer.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response create(Transformer transformer) {
        validateModerator(getRequestUser(), configuration);
        val persisted = service.create(transformer);
        return created(uriInfo, persisted);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get Transformer metadata by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get Transformer metadata",
                    content = {@Content(schema = @Schema(implementation = Transformer.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getById(@PathParam(ID) Long id,
                            @QueryParam(WITH_ASSOCIATIONS) @DefaultValue("false") Boolean withAssociations) {
        if (withAssociations) {
            return Response.ok(service.getByIdWithAssociations(id)).build();
        }
        return Response.ok(service.getById(id)).build();
    }

    @GET
    @Path("/{id}/transformations")
    @Operation(summary = "Get associated transformations of a Transformer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get associated transformations by Transformer id",
                    content = {@Content(array = @ArraySchema(schema = @Schema(implementation = Transformation.class)))}),
            @ApiResponse(responseCode = "404", description = "Transformer not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getTransformations(@PathParam(ID) Long id) {
        return Response.ok(service.getTransformations(id)).build();
    }

    @GET
    @Path("/{id}/filters")
    @Operation(summary = "Get associated filters of a Transformer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get associated filters by Transformer id",
                    content = {@Content(array = @ArraySchema(schema = @Schema(implementation = Filter.class)))}),
            @ApiResponse(responseCode = "404", description = "Transformer not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getFilters(@PathParam(ID) Long id) {
        return Response.ok(service.getFilters(id)).build();
    }

    @PATCH
    @Path("/{id}")
    @Operation(summary = "Update existing Transformer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated the Transformer",
                    content = {@Content(schema = @Schema(implementation = Transformer.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response update(@PathParam(ID) Long id, Transformer transformer) {
        validateModerator(getRequestUser(), configuration);
        validateUpdateRequestId(transformer, id);
        val updated = service.update(transformer);
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete Transformer metadata by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deleted Transformer metadata",
                    content = {@Content(schema = @Schema(implementation = Transformer.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response delete(@PathParam(ID) Long id) {
        validateModerator(getRequestUser(), configuration);
        service.delete(id);
        return Response.noContent().build();
    }
}
