package com.ebay.behavior.gds.mdm.signal.mockResource;

import com.ebay.behavior.gds.mdm.common.config.UdcConfiguration;
import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.VersionedIdWithStatus;
import com.ebay.behavior.gds.mdm.signal.common.model.Plan;
import com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.service.MetadataWriteService;
import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.service.PlanService;
import com.ebay.behavior.gds.mdm.signal.service.StagingSyncClient;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedAttributeService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedEventService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedFieldService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedSignalService;
import com.ebay.behavior.gds.mdm.signal.usecase.PromoteToStagingUsecase;
import com.ebay.raptorio.env.PlatformEnvProperties;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.CompletionStatus.COMPLETED;
import static com.ebay.behavior.gds.mdm.common.model.PlatformEnvironment.PRE_PRODUCTION;
import static com.ebay.behavior.gds.mdm.common.model.PlatformEnvironment.QA;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.SIGNAL;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.plan;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedAttribute;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedEvent;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedField;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedSignal;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.DEFINITION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.EXPECTATION_FAILED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * This test relocated under usecaseResource in order not to run as part of ResourceItSuite.
 * the reason is that it uses @MockitoSpyBean annotation which cannot run with ResourceItSuite,
 * where all beans loaded once per suite and cannot be replaced with spy beans.
 * <p/>
 * We must use fixed port 8080 as DEFINED_PORT under the server application-IT.yaml configuration,
 * so udc.metadata.endpointUri will stay valid.
 **/
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class PromoteToStagingResourceIT extends AbstractResourceTest {

    @Autowired
    private UdcConfiguration config;

    @Autowired
    private PlanService planService;

    @Autowired
    private UnstagedEventService eventService;

    @Autowired
    private UnstagedFieldService fieldService;

    @Autowired
    private UnstagedAttributeService attributeService;

    @Autowired
    private UnstagedSignalService signalService;

    @MockitoBean
    private MetadataWriteService metadataWriteService;

    @MockitoSpyBean
    private PromoteToStagingUsecase stagingUsecase;

    @MockitoBean
    private StagingSyncClient stagingInjector;

    @MockitoSpyBean
    private PlatformEnvProperties platformEnvProperties;

    private Plan plan;
    private long planId;
    private VersionedId signalId;
    private UdcDataSourceType dataSource;

    @BeforeEach
    void setUp() {
        dataSource = config.getDataSource();
        url = getBaseUrl() + V1 + DEFINITION + "/plan";
        plan = plan().setStatus(PlanStatus.CREATED);
        plan = planService.create(plan);
        planId = plan.getId();

        var event = unstagedEvent().toBuilder()
                .name("testName")
                .description("testDescription")
                .build();
        event = eventService.create(event);
        var eventId = event.getId();

        var attribute = unstagedAttribute(eventId).toBuilder().tag("testTag").build();
        attribute = attributeService.create(attribute);
        var attributeId = attribute.getId();

        var signal = unstagedSignal(planId).toBuilder()
                .name("testName")
                .description("testDescription")
                .completionStatus(COMPLETED)
                .build();
        signalId = signalService.create(signal).getSignalId();
        signalService.getById(signalId);

        var field = unstagedField(signalId).toBuilder()
                .name("testName")
                .description("testDescription")
                .tag("testTag")
                .build();
        fieldService.create(field, Set.of(attributeId));
    }

    @Test
    void promote_planNotFound_417() {
        requestSpec()
                .when().put(url + '/' + getRandomLong() + "/action/staging/promote")
                .then().statusCode(EXPECTATION_FAILED.value())
                .and().contentType(APPLICATION_JSON_VALUE);
    }

    @Test
    void promote_local() {
        // This ensures the test "sees" it runs on the staging environment
        doReturn(QA.getValue()).when(platformEnvProperties).getPlatformEnvironment();

        var expectedEntityId = Metadata.toEntityId(SIGNAL, signalId.getId());
        when(metadataWriteService.upsert(any(UnstagedSignal.class), eq(dataSource))).thenReturn(expectedEntityId);
        plan.setStatus(PlanStatus.APPROVED_BY_GOVERNANCE);
        planService.update(plan);

        val statuses = requestSpec()
                .when().put(url + '/' + planId + "/action/staging/promote")
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", VersionedIdWithStatus.class);

        assertThat(statuses).hasSize(1);
        assertThat(statuses.get(0).getId()).isEqualTo(signalId.getId());
        assertThat(statuses.get(0).getMessage()).isEqualTo(expectedEntityId);

        verify(stagingUsecase, times(1)).localPromote(planId);
        verify(stagingUsecase, never()).remotePromote(planId);
    }

    @Test
    void promote_remote() {
        // This ensures the test "sees" it runs on the pre-production environment
        doReturn(PRE_PRODUCTION.getValue()).when(platformEnvProperties).getPlatformEnvironment();
        var expectedEntityId = Metadata.toEntityId(SIGNAL, signalId.getId());
        doReturn(expectedEntityId).when(stagingInjector).post(any(), anyList(), any(), any());
        when(metadataWriteService.upsert(any(UnstagedSignal.class), eq(dataSource))).thenReturn(expectedEntityId);
        plan.setStatus(PlanStatus.APPROVED_BY_GOVERNANCE);
        planService.update(plan);

        val statuses = requestSpec()
                .when().put(url + '/' + planId + "/action/staging/promote")
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", VersionedIdWithStatus.class);

        assertThat(statuses).hasSize(1);
        assertThat(statuses.get(0).getId()).isEqualTo(signalId.getId());
        assertThat(statuses.get(0).getMessage()).isEqualTo(expectedEntityId);

        verify(stagingUsecase, times(1)).remotePromote(planId);
        verify(stagingUsecase, never()).localPromote(planId);
    }
}