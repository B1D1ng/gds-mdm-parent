package com.ebay.behavior.gds.mdm.contract.resource;

import com.ebay.behavior.gds.mdm.contract.config.ContractGovernanceConfiguration;
import com.ebay.behavior.gds.mdm.contract.model.ContractPipeline;
import com.ebay.behavior.gds.mdm.contract.model.UpdatePipelineRequest;
import com.ebay.behavior.gds.mdm.contract.service.ContractPipelineService;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
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
import static com.ebay.behavior.gds.mdm.contract.util.ValidationUtils.validServerCall;
import static com.ebay.behavior.gds.mdm.contract.util.ValidationUtils.validateModerator;

@Validated
@Path(V1 + CMM + DEFINITION + "/pipeline")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ContractPipelineResource {

    @Autowired
    private ContractPipelineService service;

    @Autowired
    private ContractGovernanceConfiguration configuration;

    @GET
    @Path("/{id}")
    @Operation(summary = "Get Pipeline metadata by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get Pipeline metadata",
                    content = {@Content(schema = @Schema(implementation = ContractPipeline.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getById(@PathParam(ID) Long id) {
        return Response.ok(service.getById(id)).build();
    }

    @PATCH
    @Path("/{id}")
    @Operation(summary = "Update existing Pipeline")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated the Pipeline",
                    content = {@Content(schema = @Schema(implementation = ContractPipeline.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response updatePipeline(@PathParam(ID) Long id, UpdatePipelineRequest request, @Context HttpServletRequest httpRequest) {
        if (!validServerCall(httpRequest)) {
            validateModerator(getRequestUser(), configuration);
        }
        validateUpdateRequestId(request, id);
        val pipeline = service.update(request);
        return Response.ok(pipeline).build();
    }
}
