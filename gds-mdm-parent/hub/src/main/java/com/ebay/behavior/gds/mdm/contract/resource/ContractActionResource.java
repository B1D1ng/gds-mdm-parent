package com.ebay.behavior.gds.mdm.contract.resource;

import com.ebay.behavior.gds.mdm.contract.model.ContractActionRequest;
import com.ebay.behavior.gds.mdm.contract.model.DeployContractRequest;
import com.ebay.behavior.gds.mdm.contract.model.UnstagedContract;
import com.ebay.behavior.gds.mdm.contract.service.ContractActionService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.validateUpdateRequestId;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.LCM;

@Validated
@Path(V1 + LCM + "/contract/{id}/action")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ContractActionResource {

    @Autowired
    private ContractActionService service;

    @PUT
    @Path("/update")
    @Operation(summary = "Update the Contract")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated the Contract",
                    content = {@Content(schema = @Schema(implementation = UnstagedContract.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response update(@PathParam(ID) Long id) {
        return Response.ok(service.update(id)).build();
    }

    @PUT
    @Path("/submit")
    @Operation(summary = "Submit the Contract")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Submitted the Contract",
                    content = {@Content(schema = @Schema(implementation = UnstagedContract.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response submit(@PathParam(ID) Long id) {
        return Response.ok(service.submit(id)).build();
    }

    @PUT
    @Path("/approve")
    @Operation(summary = "Approve the Contract")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Approved the Contract",
                    content = {@Content(schema = @Schema(implementation = UnstagedContract.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response approve(@PathParam(ID) Long id) {
        return Response.ok(service.approve(id)).build();
    }

    @PUT
    @Path("/reject")
    @Operation(summary = "Reject the Contract")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rejected the Contract",
                    content = {@Content(schema = @Schema(implementation = UnstagedContract.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response reject(@PathParam(ID) Long id, @RequestBody ContractActionRequest request) {
        return Response.ok(service.reject(id, request)).build();
    }

    @PUT
    @Path("/test")
    @Operation(summary = "Validate the Contract by deploy the staging/preprod pipeline")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Test the Contract",
                    content = {@Content(schema = @Schema(implementation = UnstagedContract.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response test(@PathParam(ID) Long id, @RequestBody DeployContractRequest request) {
        validateUpdateRequestId(request, id);
        return Response.ok(service.test(request)).build();
    }

    @PUT
    @Path("/complete-test")
    @Operation(summary = "User confirm the testing completed")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Complete test of the Contract",
                    content = {@Content(schema = @Schema(implementation = UnstagedContract.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response completeTest(@PathParam(ID) Long id) {
        return Response.ok(service.completeTest(id)).build();
    }

    @PUT
    @Path("/deploy-staging")
    @Operation(summary = "Deploy the Contract to the staging and sync to UDC")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deploy the Contract to Staging",
                    content = {@Content(schema = @Schema(implementation = UnstagedContract.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response deployStaging(@PathParam(ID) Long id, @RequestBody DeployContractRequest request) {
        validateUpdateRequestId(request, id);
        return Response.ok(service.deployStaging(request)).build();
    }

    @PUT
    @Path("/deploy-production")
    @Operation(summary = "Deploy the Contract to the production and sync to UDC")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deploy the Contract to Production",
                    content = {@Content(schema = @Schema(implementation = UnstagedContract.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response deployProduction(@PathParam(ID) Long id, @RequestBody DeployContractRequest request) {
        validateUpdateRequestId(request, id);
        return Response.ok(service.deployProduction(request)).build();
    }

    @PUT
    @Path("/archive")
    @Operation(summary = "Archive the Contract and delete from UDC")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Archived the Contract",
                    content = {@Content(schema = @Schema(implementation = UnstagedContract.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response archive(@PathParam(ID) Long id) {
        return Response.ok(service.archive(id)).build();
    }
}