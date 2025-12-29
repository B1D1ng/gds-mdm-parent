package com.ebay.behavior.gds.mdm.signal.usecase;

import com.ebay.behavior.gds.mdm.common.config.UdcConfiguration;
import com.ebay.behavior.gds.mdm.common.exception.ExternalCallException;
import com.ebay.behavior.gds.mdm.common.exception.PartialSuccessException;
import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.VersionedIdWithStatus;
import com.ebay.behavior.gds.mdm.signal.common.model.Plan;
import com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.service.MetadataWriteService;
import com.ebay.behavior.gds.mdm.signal.service.PlanService;
import com.ebay.behavior.gds.mdm.signal.service.StagedSignalService;
import com.ebay.behavior.gds.mdm.signal.service.StagingSyncClient;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedAttributeService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedEventService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedFieldService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedSignalService;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;
import com.ebay.behavior.gds.mdm.commonTestUtil.TestRequestContextUtils;
import com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils;

import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.CompletionStatus.COMPLETED;
import static com.ebay.behavior.gds.mdm.common.model.Environment.PRODUCTION;
import static com.ebay.behavior.gds.mdm.common.model.Environment.STAGING;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.SIGNAL;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestAuthFilter.IT_TEST_USER;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.stagedSignal;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedAttribute;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedEvent;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedField;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedSignal;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PromoteToProductionUsecaseIT {

    @Autowired
    private PromoteToProductionUsecase promoteUsecase;

    @Autowired
    private PlanService planService;

    @Autowired
    private StagedSignalService stagedSignalService;

    @Autowired
    private UnstagedSignalService unstagedSignalService;

    @Autowired
    private UnstagedEventService unstagedEventService;

    @Autowired
    private UnstagedFieldService unstagedFieldService;

    @Autowired
    private UnstagedAttributeService unstagedAttributeService;

    @Autowired
    private UdcConfiguration config;

    @MockitoBean
    private MetadataWriteService metadataWriteService;

    @MockitoBean
    private StagingSyncClient stagingInjector;

    private StagedSignal stagedSignal;
    private UnstagedSignal unstagedSignal;
    private VersionedId signalId;
    private Plan plan;
    private long planId;

    @BeforeAll
    void setUpAll() {
        TestRequestContextUtils.setUser(IT_TEST_USER);
    }

    @BeforeEach
    void setUp() {
        reset(metadataWriteService);
        plan = planService.create(TestModelUtils.plan());
        planId = plan.getId();
        plan.setStatus(PlanStatus.STAGING);
        plan = planService.update(plan);

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
                .domain("CART")
                .completionStatus(COMPLETED)
                .environment(STAGING)
                .build();
        signalId = unstagedSignalService.create(unstagedSignal).getSignalId();
        unstagedSignal = unstagedSignalService.getById(signalId);

        var field = unstagedField(signalId).toBuilder()
                .name("testName")
                .description("testDescription")
                .tag("testTag")
                .build();
        unstagedFieldService.create(field, Set.of(attributeId));

        stagedSignal = stagedSignal(planId).toBuilder()
                .id(signalId.getId())
                .version(signalId.getVersion())
                .name(unstagedSignal.getName())
                .description(unstagedSignal.getDescription())
                .completionStatus(COMPLETED)
                .environment(STAGING)
                .build();
        stagedSignalService.create(stagedSignal);
    }

    @Test
    void localPromote() {
        var expectedEntityId = Metadata.toEntityId(SIGNAL, signalId.getId());
        when(metadataWriteService.upsert(any(UnstagedSignal.class), eq(config.getDataSource()))).thenReturn(expectedEntityId);
        doReturn(signalId).when(stagingInjector).patch(Mockito.isNull(), Mockito.isNull(), eq(signalId), eq(VersionedId.class));

        var result = promoteUsecase.localPromote(planId);
        TestUtils.sleep(5);

        var plan = planService.getById(planId);
        assertThat(plan.getStatus()).isEqualTo(PlanStatus.PRODUCTION);
        assertThat(result).hasSize(1);

        val idWithStatus = result.get(0);
        assertThat(idWithStatus.getId()).isEqualTo(signalId.getId());
        assertThat(idWithStatus.getHttpStatusCode()).isEqualTo(200);
        assertThat(result.get(0).isOk()).isTrue();
        assertThat(result.get(0).getId()).isEqualTo(signalId.getId());
        assertThat(result.get(0).getVersion()).isEqualTo(signalId.getVersion());

        verify(metadataWriteService).upsert(
                argThat(signal -> signal.getEnvironment() == PRODUCTION),
                eq(config.getDataSource())
        );
    }

    @Test
    void finalizePromotedSignals_onePromotedNoFailed_planUpdated() {
        var idWithStatus = VersionedIdWithStatus.okStatus(signalId.getId(), signalId.getVersion(), "test");

        var statuses = promoteUsecase.finalizePromotedSignals(planId, PRODUCTION, List.of(), List.of(idWithStatus));

        assertThat(statuses).hasSize(1);
        assertThat(statuses.get(0).getId()).isEqualTo(idWithStatus.getId());

        plan = planService.getById(planId);
        assertThat(plan.getStatus()).isEqualTo(PlanStatus.PRODUCTION);
    }

    @Test
    void finalizePromotedSignals_noPromotedOneFailed_planUpdated() {
        var idWithStatus = VersionedIdWithStatus.okStatus(signalId.getId(), signalId.getVersion(), "test");

        assertThatThrownBy(() -> promoteUsecase.finalizePromotedSignals(planId, PRODUCTION, List.of(idWithStatus), List.of()))
                .isInstanceOf(ExternalCallException.class)
                .hasMessageContaining("failed");
    }

    @Test
    void finalizePromotedSignals_onePromotedOneFailed_partialSuccess() {
        var idWithStatus = VersionedIdWithStatus.okStatus(signalId.getId(), signalId.getVersion(), "test");

        assertThatThrownBy(() -> promoteUsecase.finalizePromotedSignals(planId, PRODUCTION, List.of(idWithStatus), List.of(idWithStatus)))
                .isInstanceOf(PartialSuccessException.class);
    }

    @Test
    void updatePromotedUnstagedSignals_statusUpdated() {
        var idWithStatus = VersionedIdWithStatus.okStatus(signalId.getId(), signalId.getVersion(), "test");

        var statuses = promoteUsecase.finalizePromotedSignals(planId, PRODUCTION, List.of(), List.of(idWithStatus));

        assertThat(statuses).hasSize(1);
        assertThat(statuses.get(0).getId()).isEqualTo(idWithStatus.getId());

        plan = planService.getById(planId);
        unstagedSignal = unstagedSignalService.getById(signalId);
        assertThat(plan.getStatus()).isEqualTo(PlanStatus.PRODUCTION);
        assertThat(unstagedSignal.getEnvironment()).isEqualTo(PRODUCTION);
    }

    @Test
    void updatePromotedStagedSignals_stagedSignalCreated() {
        var idWithStatus = VersionedIdWithStatus.okStatus(signalId.getId(), signalId.getVersion(), "test");

        var statuses = promoteUsecase.finalizePromotedSignals(planId, PRODUCTION, List.of(), List.of(idWithStatus));

        assertThat(statuses).hasSize(1);
        assertThat(statuses.get(0).getId()).isEqualTo(idWithStatus.getId());

        plan = planService.getById(planId);
        unstagedSignal = unstagedSignalService.getById(signalId);
        stagedSignal = stagedSignalService.getById(signalId);
        assertThat(unstagedSignal.getEnvironment()).isEqualTo(PRODUCTION);
        assertThat(stagedSignal.getSignalId()).isEqualTo(signalId);
        assertThat(stagedSignal.getEnvironment()).isEqualTo(PRODUCTION);
    }
}