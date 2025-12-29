package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.model.VersionedIdWithStatus;
import com.ebay.behavior.gds.mdm.signal.usecase.PromoteToProductionUsecase;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.DEFINITION;

@Validated
@Path(V1 + DEFINITION + "/plan/{planId}/action/production")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ConditionalOnExpression("${controller.production-enabled}")
public class PromoteToProductionResource {

    @Autowired
    private PromoteToProductionUsecase productionUsecase;

    @PUT
    @Path("/promote")
    @Operation(summary = "Promote all signals under a plan to STAGING environment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Promote all signals under a plan into a staging environment",
                    content = {@Content(schema = @Schema(implementation = VersionedIdWithStatus.class))}),
            @ApiResponse(responseCode = "417", description = "Plan id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response promote(@PathParam("planId") Long planId) {
        val result = productionUsecase.localPromote(planId);
        return Response.ok(result).build();
    }
}
