package com.ebay.behavior.gds.mdm.contract.resource;

import com.ebay.behavior.gds.mdm.contract.config.ContractGovernanceConfiguration;
import com.ebay.behavior.gds.mdm.contract.model.StreamingConfig;
import com.ebay.behavior.gds.mdm.contract.service.StreamingConfigService;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.getRequestUser;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.validateUpdateRequestId;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.CMM;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.DEFINITION;
import static com.ebay.behavior.gds.mdm.contract.util.ValidationUtils.validateModerator;

@Validated
@Path(V1 + CMM + DEFINITION + "/streaming-config")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class StreamingConfigResource {

    @Autowired
    private StreamingConfigService streamingConfigService;

    @Autowired
    private ContractGovernanceConfiguration configuration;

    @POST
    @Operation(summary = "Create new Streaming Config")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created the Streaming Config",
                    content = {@Content(schema = @Schema(implementation = StreamingConfig.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response create(StreamingConfig streamingConfig) {
        validateModerator(getRequestUser(), configuration);
        val persisted = streamingConfigService.create(streamingConfig);
        return Response.ok(persisted).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get Streaming Config metadata by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get Streaming Config metadata",
                    content = {@Content(schema = @Schema(implementation = StreamingConfig.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getById(@PathParam(ID) Long id) {
        return Response.ok(streamingConfigService.getById(id)).build();
    }

    @PATCH
    @Path("/{id}")
    @Operation(summary = "Update existing Streaming Config")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated the Streaming Config",
                    content = {@Content(schema = @Schema(implementation = StreamingConfig.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response update(@PathParam(ID) Long id, StreamingConfig streamingConfig) {
        validateModerator(getRequestUser(), configuration);
        validateUpdateRequestId(streamingConfig, id);
        val updated = streamingConfigService.update(streamingConfig);
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete Streaming Config metadata by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deleted Streaming Config metadata",
                    content = {@Content(schema = @Schema(implementation = StreamingConfig.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response delete(@PathParam(ID) Long id) {
        validateModerator(getRequestUser(), configuration);
        streamingConfigService.delete(id);
        return Response.noContent().build();
    }
}
