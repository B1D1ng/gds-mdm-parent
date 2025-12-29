package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.common.model.Plan;
import com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus;
import com.ebay.behavior.gds.mdm.signal.common.model.PlanUserAction;
import com.ebay.behavior.gds.mdm.signal.common.model.audit.PlanHistory;
import com.ebay.behavior.gds.mdm.signal.config.GovernanceConfiguration;
import com.ebay.behavior.gds.mdm.signal.service.PlanService;
import com.ebay.behavior.gds.mdm.signal.util.AuditUtils;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.ebay.behavior.gds.mdm.common.model.audit.AuditLogParams.MODE;
import static com.ebay.behavior.gds.mdm.common.model.audit.AuditMode.FULL;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestAuthFilter.IT_TEST_USER;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.plan;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.DEFINITION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.EXPECTATION_FAILED;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class PlanActionResourceIT extends AbstractResourceTest {

    @Autowired
    private PlanService planService;

    @Autowired
    private GovernanceConfiguration config;

    private Plan plan;
    private long planId;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + DEFINITION + "/plan";
        plan = plan().setStatus(PlanStatus.CREATED);
        plan = planService.create(plan);
        planId = plan.getId();
    }

    @Test
    void complete() {
        plan.setOwners(IT_TEST_USER);
        plan = planService.update(plan);

        requestSpecWithBody(plan)
                .when().put(url + String.format("/%d/action/complete", planId))
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE);

        testAuditLog(PlanUserAction.COMPLETE);
    }

    @Test
    void complete_planNotFound_417() {
        requestSpecWithBody(plan)
                .when().put(url + String.format("/%d/action/complete", getRandomLong()))
                .then().statusCode(EXPECTATION_FAILED.value())
                .and().contentType(APPLICATION_JSON_VALUE);
    }

    @Test
    void submitForReview() {
        plan.setOwners(IT_TEST_USER);
        plan = planService.update(plan);

        requestSpec()
                .when().put(url + String.format("/%d/action/submit-for-review", planId))
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE);

        testAuditLog(PlanUserAction.SUBMIT_FOR_REVIEW);
    }

    @Test
    void approve() {
        plan.setStatus(PlanStatus.SUBMITTED_FOR_REVIEW);
        plan.setOwners(config.getModerators());
        plan = planService.update(plan);

        requestSpec()
                .when().put(url + String.format("/%d/action/approve", planId))
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE);

        testAuditLog(PlanUserAction.APPROVE);
    }

    @Test
    void approve_forbidden_403() {
        requestSpec()
                .when().put(url + String.format("/%d/action/approve", planId))
                .then().statusCode(FORBIDDEN.value())
                .and().contentType(APPLICATION_JSON_VALUE);
    }

    @Test
    void reject() {
        plan.setStatus(PlanStatus.SUBMITTED_FOR_REVIEW);
        plan.setOwners(config.getModerators());
        planService.update(plan);
        requestSpec().body("{\"comment\": \"reject\"}")
                .when().put(url + String.format("/%d/action/reject", planId))
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE);

        testAuditLog(PlanUserAction.REJECT);
    }

    @Test
    void reject_forbidden_403() {
        requestSpec().body("{\"comment\": \"reject\"}")
                .when().put(url + String.format("/%d/action/reject", planId))
                .then().statusCode(FORBIDDEN.value())
                .and().contentType(APPLICATION_JSON_VALUE);
    }

    @Test
    void hide() {
        requestSpec()
                .when().put(url + String.format("/%d/action/hide", planId))
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE);

        testAuditLog(PlanUserAction.HIDE);
    }

    @Test
    void hide_planNotFound_417() {
        requestSpec()
                .when().put(url + String.format("/%d/action/hide", getRandomLong()))
                .then().statusCode(EXPECTATION_FAILED.value())
                .and().contentType(APPLICATION_JSON_VALUE);
    }

    @Test
    void cancel() {
        requestSpec()
                .when().put(url + String.format("/%d/action/cancel", planId))
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE);

        testAuditLog(PlanUserAction.CANCEL);
    }

    @Test
    void cancel_planNotFound_417() {
        requestSpec()
                .when().put(url + String.format("/%d/action/cancel", getRandomLong()))
                .then().statusCode(EXPECTATION_FAILED.value())
                .and().contentType(APPLICATION_JSON_VALUE);
    }

    @SneakyThrows
    private void testAuditLog(PlanUserAction action) {
        var json = requestSpec()
                .queryParam(MODE, FULL)
                .when().get(url + '/' + planId + "/auditLog")
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().asString();

        var auditRecords = AuditUtils.deserializeAuditRecords(json, objectMapper, PlanHistory.class);

        assertThat(auditRecords).isNotEmpty();
        assertThat(auditRecords.stream()
                .filter(record -> action.name().equals(record.getChangeReason()))
                .count())
                .isEqualTo(1);
    }
}