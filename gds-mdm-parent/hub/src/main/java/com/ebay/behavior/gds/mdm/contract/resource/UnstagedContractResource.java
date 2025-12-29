package com.ebay.behavior.gds.mdm.contract.resource;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.search.RelationalSearchRequest;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.util.DbUtils;
import com.ebay.behavior.gds.mdm.contract.config.ContractGovernanceConfiguration;
import com.ebay.behavior.gds.mdm.contract.model.ContractConfigView;
import com.ebay.behavior.gds.mdm.contract.model.ContractPipeline;
import com.ebay.behavior.gds.mdm.contract.model.Routing;
import com.ebay.behavior.gds.mdm.contract.model.SyncUdcRequest;
import com.ebay.behavior.gds.mdm.contract.model.UnstagedContract;
import com.ebay.behavior.gds.mdm.contract.model.UpdateContractRequest;
import com.ebay.behavior.gds.mdm.contract.model.search.ContractSearchBy;
import com.ebay.behavior.gds.mdm.contract.service.ContractOwnershipService;
import com.ebay.behavior.gds.mdm.contract.service.ContractSyncUdcService;
import com.ebay.behavior.gds.mdm.contract.service.UnstagedContractService;
import com.ebay.behavior.gds.mdm.contract.util.ValidationUtils;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
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
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.PAGE_NUMBER;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.PAGE_SIZE;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_BY;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_TERM;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.created;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.getRequestUser;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.validateUpdateRequestId;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.CMM;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.DEFINITION;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.RECURSIVE;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.WITH_ASSOCIATIONS;
import static com.ebay.behavior.gds.mdm.contract.util.ValidationUtils.validServerCall;
import static com.ebay.behavior.gds.mdm.contract.util.ValidationUtils.validateModerator;

@Slf4j
@Validated
@Path(V1 + CMM + DEFINITION + "/contract")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UnstagedContractResource {

    @Getter
    @Autowired
    private UnstagedContractService service;

    @Autowired
    private ContractOwnershipService contractOwnershipService;

    @Autowired
    private ContractSyncUdcService contractSyncUdcService;

    @Autowired
    private ContractGovernanceConfiguration configuration;

    @Context
    private UriInfo uriInfo;

    @POST
    @Operation(summary = "Create new Contract Template")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Create new Contract",
                    content = {@Content(schema = @Schema(implementation = UnstagedContract.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})})
    public Response create(UnstagedContract unstagedContract) {
        validateModerator(getRequestUser(), configuration);
        val persisted = service.create(unstagedContract);
        return created(uriInfo, persisted);
    }

    @PATCH
    @Path("/{id}")
    @Operation(summary = "Update a latest version of an Contract by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Update a latest version of an Contract",
                    content = {@Content(schema = @Schema(implementation = UnstagedContract.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))}),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response updateLatestVersion(@PathParam(ID) Long id, UpdateContractRequest request,
                                        @Context HttpServletRequest httpRequest) {
        if (!validServerCall(httpRequest)) {
            validateModerator(getRequestUser(), configuration);
        }
        validateUpdateRequestId(request, id);
        val updated = service.updateLatestVersion(request);
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete a latest version of an Contract by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deleted the latest version of an Contract"),
            @ApiResponse(responseCode = "417", description = "Id not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response deleteLatestVersion(@PathParam(ID) Long id) {
        validateModerator(getRequestUser(), configuration);
        service.deleteLatestVersion(id);
        return Response.noContent().build();
    }

    @GET
    @Operation(summary = "Search contract")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search contract",
                    content = {@Content(schema = @Schema(implementation = UnstagedContract.class))}),
            @ApiResponse(responseCode = "400", description = "Bad contract", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getAll(@QueryParam(Search.OWNED_BY_ME) boolean ownedByMe,
                           @QueryParam(SEARCH_TERM) String searchTerm,
                           @QueryParam(SEARCH_BY) ContractSearchBy searchBy,
                           @Min(0) @QueryParam(PAGE_NUMBER) @DefaultValue("0") int pageNumber,
                           @Min(1) @Max(2000) @QueryParam(PAGE_SIZE) @DefaultValue("20") int pageSize) {
        if (searchTerm != null || searchBy != null) {
            ValidationUtils.validateContractSearchParams(searchBy, searchTerm, ownedByMe);
        }

        val user = getRequestUser();
        val pageable = DbUtils.getAuditablePageable(pageNumber, pageSize);
        Optional<String> maybeUser = Optional.empty();

        if (BooleanUtils.isTrue(ownedByMe)) {
            maybeUser = Optional.ofNullable(user);
        }

        val page = service.getAll(maybeUser, searchBy, searchTerm, pageable);
        page.get().forEach(contract -> contractOwnershipService.setUserPermissions(contract, user));
        return Response.ok(page).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get a latest version of an Contract by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get an Contract by id",
                    content = {@Content(schema = @Schema(implementation = UnstagedContract.class))}),
            @ApiResponse(responseCode = "404", description = "Contract not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getByIdAndLatestVersion(@PathParam(ID) Long id,
                                            @QueryParam(WITH_ASSOCIATIONS) @DefaultValue("false") Boolean withAssociations,
                                            @QueryParam(RECURSIVE) @DefaultValue("false") Boolean recursive) {
        UnstagedContract unstagedContract;
        val contractId = ofLatestVersion(id);

        if (withAssociations) {
            unstagedContract = service.getByIdWithAssociations(contractId, recursive);
        } else {
            unstagedContract = service.getById(contractId);
        }
        contractOwnershipService.setUserPermissions(unstagedContract, getRequestUser());
        return Response.ok(unstagedContract).build();
    }

    @GET
    @Path("/{id}/yaml")
    @Operation(summary = "Get a latest version of an Contract by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get an Contract by id",
                    content = {@Content(schema = @Schema(implementation = UnstagedContract.class))}),
            @ApiResponse(responseCode = "404", description = "Contract not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    @Produces(MediaType.TEXT_PLAIN)
    public Response getReadableContract(@PathParam(ID) Long id) {
        return getByIdAndLatestVersion(id, true, true);
    }

    @GET
    @Path("/{id}/routings")
    @Operation(summary = "Get associated routings of a latest version of an Contract")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get associated routings by Contract id",
                    content = {@Content(array = @ArraySchema(schema = @Schema(implementation = Routing.class)))}),
            @ApiResponse(responseCode = "404", description = "Contract not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getRoutings(@PathParam(ID) Long id) {
        val contractId = ofLatestVersion(id);
        val routings = service.getRoutings(contractId);
        return Response.ok(routings).build();
    }

    @GET
    @Path("/{id}/pipelines")
    @Operation(summary = "Get associated pipelines of a latest version of an Contract")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get associated pipelines by Contract id",
                    content = {@Content(array = @ArraySchema(schema = @Schema(implementation = ContractPipeline.class)))}),
            @ApiResponse(responseCode = "404", description = "Contract not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getPipelines(@PathParam(ID) Long id) {
        val contractId = ofLatestVersion(id);
        val pipelines = service.getPipelines(contractId);
        return Response.ok(pipelines).build();
    }

    @POST
    @Path("/{id}/sync-udc")
    @Operation(summary = "Sync Data Contract to UDC")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get associated pipelines by Contract id",
                    content = {@Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))}),
            @ApiResponse(responseCode = "404", description = "Contract not found",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response syncUdc(@PathParam(ID) Long id, SyncUdcRequest syncUdcRequest) {
        validateModerator(getRequestUser(), configuration);
        val udcYaml = contractSyncUdcService.syncContractToUdc(id, syncUdcRequest.getEnv());
        return Response.ok(udcYaml).build();
    }

    @PUT
    @Operation(summary = "Search for contract source based on search query specification")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search for contract based on search query specification",
                    content = {@Content(schema = @Schema(implementation = ContractConfigView.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response search(@QueryParam(WITH_ASSOCIATIONS) @DefaultValue("false") Boolean withAssociations,
                           @NotNull @Valid RelationalSearchRequest request) {
        return Response.ok(service.search(request)).build();
    }

    private VersionedId ofLatestVersion(long id) {
        return VersionedId.of(id, service.getLatestVersion(id));
    }
}