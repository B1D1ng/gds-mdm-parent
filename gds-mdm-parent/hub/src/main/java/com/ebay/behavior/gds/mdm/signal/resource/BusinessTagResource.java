package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.signal.common.model.BusinessTagNotification;
import com.ebay.behavior.gds.mdm.signal.service.BusinessTagService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.BUSINESS_TAGS;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

@Slf4j
@Validated
@Path(V1 + BUSINESS_TAGS)
@RestController
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ConditionalOnExpression("${controller.production-enabled}")
public class BusinessTagResource {

    @Autowired
    private BusinessTagService service;

    @POST
    @Operation(summary = "Notify the service of the business tags daily job status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Zeta notification is successfully delivered, starting to load data"),
            @ApiResponse(responseCode = "400", description = "Bad request")
    })
    public Response businessTags(BusinessTagNotification notification) {
        val date = LocalDate.parse(notification.getRunDate()).minusDays(2).toString();
        log.info(String.format("Received notification for business tags job: %s, run date: %s, data date: %s, table: %s, status: %s", notification.getJobName(),
                notification.getRunDate(), date, notification.getTable(), notification.getStatus()));

        if (HttpStatus.OK.getReasonPhrase().equals(notification.getStatus())) {
            service.loadBusinessTags(date);
            return Response.accepted().build();
        }

        // No notification would be sent for failed jobs
        log.warn("Invalid business tag notification. Status: {}", notification.getStatus());
        return Response.status(BAD_REQUEST).build();
    }
}
