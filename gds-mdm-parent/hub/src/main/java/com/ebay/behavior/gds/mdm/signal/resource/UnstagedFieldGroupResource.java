package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.signal.common.model.FieldGroup;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedFieldGroupService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedSignalService;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.val;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.TAG;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.DEFINITION;

@Validated
@Path(V1 + DEFINITION + "/signal/{id}/field-group")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UnstagedFieldGroupResource {

    @Autowired
    private UnstagedFieldGroupService fieldGroupService;

    @Autowired
    private UnstagedSignalService signalService;

    @GET
    @Operation(summary = "Get associated field groups of a latest version of an UnstagedSignal")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get associated field groups by UnstagedSignal id",
                    content = {@Content(array = @ArraySchema(schema = @Schema(implementation = UnstagedField.class)))}),
            @ApiResponse(responseCode = "404", description = "UnstagedSignal not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getAll(@PathParam(ID) Long id) {
        val signalId = ofLatestVersion(id);
        val fields = fieldGroupService.getAll(signalId);
        return Response.ok(fields).build();
    }

    @PATCH
    @Operation(summary = "Update a FieldGroup")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Update FieldGroup",
                    content = {@Content(schema = @Schema(implementation = UnstagedField.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response update(@PathParam(ID) Long id, FieldGroup<UnstagedField> fieldGroup) {
        val signalId = ofLatestVersion(id);
        Validate.isTrue(signalId.equals(fieldGroup.getSignalId()), "Signal id mismatch");
        fieldGroupService.update(fieldGroup);
        return Response.ok().build();
    }

    // TODO remove TAG query param after UI fix
    @DELETE
    @Operation(summary = "Delete a FieldGroup by groupKey")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted an UnstagedField"),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response delete(@PathParam(ID) Long id,
                                @QueryParam(TAG) String tag,
                                @QueryParam("key") String key) {
        val signalId = ofLatestVersion(id);
        if (Objects.nonNull(tag)) {
            fieldGroupService.deleteByTag(signalId, tag);
        } else if (Objects.nonNull(key)) {
            fieldGroupService.deleteByKey(signalId, key);
        } else {
            throw new IllegalArgumentException("Either tag or groupKey must be provided");
        }
        return Response.noContent().build();
    }

    private VersionedId ofLatestVersion(long id) {
        return VersionedId.of(id, signalService.getLatestVersion(id));
    }
}