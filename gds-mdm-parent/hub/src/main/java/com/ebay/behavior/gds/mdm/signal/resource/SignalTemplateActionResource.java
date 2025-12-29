package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.signal.common.model.SignalTemplate;
import com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService;
import com.ebay.behavior.gds.mdm.signal.common.model.TemplateFieldDefinition;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

import static com.ebay.behavior.gds.mdm.common.model.search.Search.TYPE;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.TEMPLATE;

@Validated
@Path(V1 + TEMPLATE + "/signal")
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SignalTemplateActionResource {

    @Autowired
    private SignalTemplateActionService service;

    @PUT
    @Path("/{type}/action/recreate")
    @Operation(summary = "Recreate a signal template and all associations, optionally with custom field configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rebuild Signal Template",
            content = {@Content(schema = @Schema(implementation = SignalTemplate.class))}),
        @ApiResponse(responseCode = "400", description = "Bad request",
            content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})})
    public Response recreate(@PathParam(TYPE) String type, @RequestBody(required = false) @Valid Optional<List<TemplateFieldDefinition>> maybeFieldsConfig) {
        val signal = service.recreate(type, maybeFieldsConfig);
        return Response.ok(signal).build();
    }
}
