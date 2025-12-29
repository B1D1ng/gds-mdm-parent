package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.signal.common.model.SignalTypeLookup;
import com.ebay.behavior.gds.mdm.signal.model.manyToMany.SignalTypeDimensionMapping;
import com.ebay.behavior.gds.mdm.signal.service.SignalTypeLookupService;
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
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.IS_MANDATORY;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.created;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.csvStringToSet;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.validateUpdateRequestId;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.LOOKUP;

@Validated
@Path(V1 + LOOKUP + "/signal-type")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SignalTypeLookupResource {

    @Context
    private UriInfo uriInfo;

    @Autowired
    private SignalTypeLookupService service;

    @POST
    @Operation(summary = "Create a new signal type")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Create a new signalType"),
            @ApiResponse(responseCode = "400", description = "Bad request")
    })
    public Response create(SignalTypeLookup lookup) {
        val persisted = service.create(lookup);
        return created(uriInfo, persisted);
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update existing signal type")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated the signalType",
                    content = {@Content(schema = @Schema(implementation = SignalTypeLookup.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response update(@PathParam(ID) Long id, SignalTypeLookup lookup) {
        validateUpdateRequestId(lookup, id);
        val persisted = service.update(lookup);
        return Response.ok(persisted).build();
    }

    @POST
    @Path("/{id}/physical-storage/{storageId}")
    @Operation(summary = "Create an associate between a signal and a physical storage")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Create a new association"),
            @ApiResponse(responseCode = "400", description = "Bad request")
    })
    public Response createMapping(@PathParam(ID) Long id, @PathParam("storageId") Long storageId) {
        val persisted = service.createPhysicalStorageMapping(id, storageId);
        return created(uriInfo, persisted);
    }

    @DELETE
    @Path("/{id}/physical-storage/{storageId}")
    @Operation(summary = "Delete an association between a signal type and a physical storage")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Delete an association"),
            @ApiResponse(responseCode = "400", description = "Bad request")
    })
    public Response deleteMapping(@PathParam(ID) Long id, @PathParam("storageId") Long storageId) {
        service.deletePhysicalStorageMapping(id, storageId);
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/dimension/{dimensionId}")
    @Operation(summary = "Create an association between a signal type and a dimension")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Create a new association"),
            @ApiResponse(responseCode = "400", description = "Bad request")
    })
    public Response createDimensionMapping(@PathParam(ID) Long id, @PathParam("dimensionId") Long dimId,
                                           @QueryParam(IS_MANDATORY) @DefaultValue("false") Boolean isMandatory) {
        val persisted = service.createDimensionMapping(id, dimId, isMandatory);
        return created(uriInfo, persisted);
    }

    @PUT
    @Path("/{id}/dimension/{dimensionId}")
    @Operation(summary = "Update an association between a signal type and a dimension")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated the association",
                    content = {@Content(schema = @Schema(implementation = SignalTypeDimensionMapping.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response updateDimensionMapping(@PathParam(ID) Long id, @PathParam("dimensionId") Long dimId,
                                           @QueryParam(IS_MANDATORY) @DefaultValue("false") Boolean isMandatory) {
        val persisted = service.updateDimensionMapping(id, dimId, isMandatory);
        return Response.ok(persisted).build();
    }

    @DELETE
    @Path("/{id}/dimension/{dimensionId}")
    @Operation(summary = "Delete an association between a signal type and a dimension")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Delete an association"),
            @ApiResponse(responseCode = "400", description = "Bad request")
    })
    public Response deleteDimensionMapping(@PathParam(ID) Long id, @PathParam("dimensionId") Long dimId) {
        service.deleteDimensionMapping(id, dimId);
        return Response.noContent().build();
    }

    @GET
    @Operation(summary = "Search signal types")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get all signal types",
                    content = {@Content(schema = @Schema(implementation = SignalTypeLookup.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
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
    @Operation(summary = "Get signal type by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get signal type by id",
                    content = {@Content(schema = @Schema(implementation = SignalTypeLookup.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getById(@PathParam("id") Long id) {
        return Response.ok(service.getById(id)).build();
    }

    @GET
    @Path("/{id}/dimensions")
    @Operation(summary = "Get dimensions by signal-type id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get dimensions by signal-type id",
                    content = {@Content(schema = @Schema(implementation = SignalTypeLookup.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getDimensionsById(@PathParam("id") Long id) {
        return Response.ok(service.getDimensionsBySignalTypeId(id)).build();
    }
}
