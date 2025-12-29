package com.ebay.behavior.gds.mdm.signal.mockResource;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.VersionedIdWithStatus;
import com.ebay.behavior.gds.mdm.signal.common.model.Plan;
import com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.service.PlanService;
import com.ebay.behavior.gds.mdm.signal.service.StagedSignalService;
import com.ebay.behavior.gds.mdm.signal.service.StagingSyncClient;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedAttributeService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedEventService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedFieldService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedSignalService;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.CompletionStatus.COMPLETED;
import static com.ebay.behavior.gds.mdm.common.model.Environment.STAGING;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.plan;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.stagedSignal;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedAttribute;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedEvent;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedField;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedSignal;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.DEFINITION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.springframework.http.HttpStatus.EXPECTATION_FAILED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class PromoteToProductionResourceIT extends AbstractResourceTest {

    @Autowired
    private PlanService planService;

    @Autowired
    private UnstagedEventService unstagedEventService;

    @Autowired
    private UnstagedFieldService unstagedFieldService;

    @Autowired
    private UnstagedAttributeService unstagedAttributeService;

    @Autowired
    private UnstagedSignalService unstagedSignalService;

    @Autowired
    private StagedSignalService stagedSignalService;

    @MockitoBean
    private StagingSyncClient stagingInjector;

    private Plan plan;
    private long planId;
    private UnstagedSignal unstagedSignal;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + DEFINITION + "/plan";
        plan = plan().setStatus(PlanStatus.CREATED);
        plan = planService.create(plan);
        planId = plan.getId();

        var event = unstagedEvent().toBuilder()
                .name("testName")
                .description("testDescription")
                .build();
        event = unstagedEventService.create(event);
        var eventId = event.getId();

        var attribute = unstagedAttribute(eventId).toBuilder().tag("testTag").build();
        attribute = unstagedAttributeService.create(attribute);
        var attributeId = attribute.getId();

        unstagedSignal = unstagedSignal(planId).toBuilder()
                .name("testName")
                .description("testDescription")
                .completionStatus(COMPLETED)
                .environment(STAGING)
                .build();
        var signalId = unstagedSignalService.create(unstagedSignal).getSignalId();
        unstagedSignal = unstagedSignalService.getById(signalId);

        var field = unstagedField(signalId).toBuilder()
                .name("testName")
                .description("testDescription")
                .tag("testTag")
                .build();
        unstagedFieldService.create(field, Set.of(attributeId));
    }

    @Test
    void promote_planNotFound_417() {
        requestSpec()
                .when().put(url + '/' + getRandomLong() + "/action/production/promote")
                .then().statusCode(EXPECTATION_FAILED.value())
                .and().contentType(APPLICATION_JSON_VALUE);
    }

    @Test
    void promote() {
        plan.setStatus(PlanStatus.STAGING);
        planService.update(plan);
        val signalId = unstagedSignal.getSignalId();
        doReturn(signalId).when(stagingInjector).patch(Mockito.isNull(), Mockito.isNull(), eq(signalId), eq(VersionedId.class));

        var stagedSignal = stagedSignal(planId).toBuilder()
                .id(signalId.getId())
                .version(signalId.getVersion())
                .name(unstagedSignal.getName())
                .description(unstagedSignal.getDescription())
                .completionStatus(COMPLETED)
                .environment(STAGING)
                .build();
        stagedSignalService.create(stagedSignal);

        val statuses = requestSpec()
                .when().put(url + '/' + planId + "/action/production/promote")
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", VersionedIdWithStatus.class);

        assertThat(statuses).hasSize(1);
        assertThat(statuses.get(0).getId()).isEqualTo(signalId.getId());
        assertThat(statuses.get(0).getVersion()).isEqualTo(signalId.getVersion());
    }
}