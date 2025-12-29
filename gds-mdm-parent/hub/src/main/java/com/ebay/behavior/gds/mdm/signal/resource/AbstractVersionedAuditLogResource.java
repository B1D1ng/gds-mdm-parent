package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.model.audit.AuditLogParams;
import com.ebay.behavior.gds.mdm.common.model.audit.AuditMode;
import com.ebay.behavior.gds.mdm.common.model.audit.VersionedHistoryAuditable;
import com.ebay.behavior.gds.mdm.commonSvc.service.AuditService;
import com.ebay.behavior.gds.mdm.signal.util.AuditUtils;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.model.VersionedModel.VERSION;
import static com.ebay.behavior.gds.mdm.common.model.audit.AuditLogParams.MODE;

public abstract class AbstractVersionedAuditLogResource<H extends VersionedHistoryAuditable> {

    @Autowired
    private ObjectMapper objectMapper;

    protected abstract AuditService<H> getService();

    @GET
    @Path("/auditLog")
    @Operation(summary = "Get the audit log of an entity version")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get audit log", content = {@Content(schema = @Schema(implementation = List.class))}),
            @ApiResponse(responseCode = "417", description = "Id/ver not found", content = {@Content(schema = @Schema(implementation = ErrorMessageV3.class))})
    })
    public Response getVersionedAuditLog(@PathParam(ID) Long id,
                                         @PathParam(VERSION) Integer version,
                                         @QueryParam(MODE) @DefaultValue("BASIC") AuditMode mode) {
        val auditParams = AuditLogParams.ofVersioned(id, version, mode);
        val log = getService().getAuditLog(auditParams);
        val json = AuditUtils.serializeAuditRecords(objectMapper, log);
        return Response.ok(json).build();
    }
}
