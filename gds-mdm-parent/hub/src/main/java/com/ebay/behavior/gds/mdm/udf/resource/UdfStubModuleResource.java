package com.ebay.behavior.gds.mdm.udf.resource;

import com.ebay.behavior.gds.mdm.udf.common.model.UdfStubModule;
import com.ebay.behavior.gds.mdm.udf.service.UdfStubModuleService;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.created;
import static com.ebay.behavior.gds.mdm.udf.util.UdfUtils.UDFMM;

@Validated
@Path(V1 + UDFMM + "/udf/stub/module")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UdfStubModuleResource {

    @Autowired
    private UdfStubModuleService service;

    @Context
    private UriInfo uriInfo;

    @POST
    @Operation(summary = "Create new UdfStubModule")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created the udf",
                    content = {@Content(schema = @Schema(implementation = UdfStubModule.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response create(UdfStubModule udfStubModule) {
        val persisted = service.create(udfStubModule);
        return created(uriInfo, persisted);
    }
}
