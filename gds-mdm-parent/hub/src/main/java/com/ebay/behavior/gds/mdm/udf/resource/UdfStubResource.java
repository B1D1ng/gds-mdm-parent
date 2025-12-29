package com.ebay.behavior.gds.mdm.udf.resource;

import com.ebay.behavior.gds.mdm.udf.common.model.UdfStub;
import com.ebay.behavior.gds.mdm.udf.service.UdfStubService;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
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
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.csvStringToSet;
import static com.ebay.behavior.gds.mdm.udf.util.UdfUtils.UDFMM;

@Validated
@Path(V1 + UDFMM + "/udf-stub")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UdfStubResource {

    @Context
    private UriInfo uriInfo;

    @Autowired
    private UdfStubService service;

    @POST
    @Operation(summary = "Create new Udf Stub")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created the udf stub",
                    content = {@Content(schema = @Schema(implementation = UdfStub.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response create(UdfStub udfStub) {
        val names = csvStringToSet(udfStub.getStubName());
        val persisted = service.create(udfStub, names);
        return created(uriInfo, persisted);
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update Udf Stub by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Update the udf stub by id",
                    content = {@Content(schema = @Schema(implementation = UdfStub.class))}),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response update(@PathParam(ID) Long id, UdfStub udfStub) {
        val persisted = service.update(udfStub, id);
        return Response.ok(persisted).build();
    }
}
