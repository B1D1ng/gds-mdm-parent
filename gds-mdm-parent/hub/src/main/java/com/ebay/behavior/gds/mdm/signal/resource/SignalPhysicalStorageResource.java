package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalPhysicalStorage;
import com.ebay.behavior.gds.mdm.signal.service.SignalPhysicalStorageService;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.ENVIRONMENT;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.created;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.validateUpdateRequestId;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.LOOKUP;

@Validated
@Path(V1 + LOOKUP + "/physical-storage")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SignalPhysicalStorageResource {

    @Context
    private UriInfo uriInfo;

    @Autowired
    private SignalPhysicalStorageService service;

    @POST
    @Operation(summary = "Create new physical storage")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created the physical storage",
                    content = {@Content(schema = @Schema(implementation = SignalPhysicalStorage.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response create(SignalPhysicalStorage storage) {
        val persisted = service.create(storage);
        return created(uriInfo, persisted);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get physical storage by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get physical storage by id",
                    content = {@Content(schema = @Schema(implementation = SignalPhysicalStorage.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getById(@PathParam(ID) Long id) {
        return Response.ok(service.getById(id)).build();
    }

    @GET
    @Operation(summary = "Get all physical storages")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get all physical storages",
                    content = {@Content(schema = @Schema(implementation = SignalPhysicalStorage.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getAll() {
        return Response.ok(service.getAll()).build();
    }

    @GET
    @Path("/search")
    @Operation(summary = "Get a storage based on a search criteria")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get SignalPhysicalStorage based on a search criteria",
                    content = {@Content(schema = @Schema(implementation = Page.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response search(@QueryParam("signalId") Long id,
                           @QueryParam("signalVersion") Integer version,
                           @QueryParam(ENVIRONMENT) Environment env,
                           @QueryParam("kafkaTopic") String topic) {
        SignalPhysicalStorage persisted;

        if (id != null && version != null) {
            Validate.isTrue(Objects.isNull(env) && Objects.isNull(topic));
            persisted = service.getBySignalId(VersionedId.of(id, version));
        } else if (topic != null && env != null) {
            Validate.isTrue(Objects.isNull(id) && Objects.isNull(version));
            persisted = service.getByKafkaTopicAndEnvironment(topic, env);
        } else {
            throw new IllegalArgumentException(
                    "Invalid search criteria. Allowed pairs: (signalId, signalVersion), (platform, environment), (kafkaTopic, environment).");
        }

        return Response.ok(persisted).build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update existing physical storage")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated the physical storage",
                    content = {@Content(schema = @Schema(implementation = SignalPhysicalStorage.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response update(@PathParam(ID) Long id, SignalPhysicalStorage storage) {
        validateUpdateRequestId(storage, id);
        val persisted = service.update(storage);
        return Response.ok(persisted).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete physical storage by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deleted Physical storage by id",
                    content = {@Content(schema = @Schema(implementation = SignalPhysicalStorage.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response delete(@PathParam(ID) Long id) {
        service.delete(id);
        return Response.noContent().build();
    }
}
