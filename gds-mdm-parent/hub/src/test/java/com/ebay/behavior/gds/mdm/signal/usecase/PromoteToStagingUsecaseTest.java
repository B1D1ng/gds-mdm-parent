package com.ebay.behavior.gds.mdm.signal.usecase;

import com.ebay.behavior.gds.mdm.common.config.UdcConfiguration;
import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.exception.ExternalCallException;
import com.ebay.behavior.gds.mdm.common.exception.PartialSuccessException;
import com.ebay.behavior.gds.mdm.common.exception.UdcException;
import com.ebay.behavior.gds.mdm.common.model.CompletionStatus;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.VersionedIdWithStatus;
import com.ebay.behavior.gds.mdm.signal.common.model.Plan;
import com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.service.MetadataWriteService;
import com.ebay.behavior.gds.mdm.signal.repository.UnstagedSignalRepository;
import com.ebay.behavior.gds.mdm.signal.service.MetricsService;
import com.ebay.behavior.gds.mdm.signal.service.PlanActionService;
import com.ebay.behavior.gds.mdm.signal.service.PlanService;
import com.ebay.behavior.gds.mdm.signal.service.SignalImportService;
import com.ebay.behavior.gds.mdm.signal.service.StagingSyncClient;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedSignalService;

import io.micrometer.core.instrument.Counter;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.CompletionStatus.COMPLETED;
import static com.ebay.behavior.gds.mdm.common.model.Environment.STAGING;
import static com.ebay.behavior.gds.mdm.common.model.VersionedId.MIN_VERSION;
import static com.ebay.behavior.gds.mdm.signal.common.model.PlanUserAction.PROMOTE_TO_STAGING;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.plan;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedAttribute;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedEvent;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedField;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedSignal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromoteToStagingUsecaseTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private PlanService planService;

    @Mock
    private UnstagedSignalService signalService;

    @Mock
    private SignalImportService signalImportService;

    @Mock
    private MetadataWriteService metadataWriteService;

    @Mock
    private UnstagedSignalRepository signalRepository;

    @Mock
    private PlanActionService planActionService;

    @Mock
    private StagingSyncClient stagingInjector;

    @Mock
    private UdcConfiguration config;

    @Spy
    @InjectMocks
    private PromoteToStagingUsecase usecase;

    @Mock
    private MetricsService metricsService;

    @Mock
    private Counter counter;

    private final long planId = 1L;
    private final VersionedId signalId = VersionedId.of(2L, MIN_VERSION);

    private Plan plan;
    private UnstagedSignal signal;
    private UnstagedEvent event;
    private UnstagedField field;
    private final UdcDataSourceType dataSource = UdcDataSourceType.TEST;

    @BeforeEach
    void setUp() {
        plan = plan().withId(planId).setStatus(PlanStatus.APPROVED_BY_GOVERNANCE);
        var eventId = 3L;
        var fieldId = 6L;

        event = unstagedEvent().toBuilder()
                .id(eventId)
                .name("good")
                .build();
        var attribute = unstagedAttribute(eventId).toBuilder().id(4L).build();
        event.setAttributes(Set.of(attribute));
        signal = unstagedSignal(planId).setSignalId(signalId).toBuilder()
                .domain(null)
                .owners(null)
                .completionStatus(COMPLETED)
                .events(Set.of(event))
                .build();

        field = unstagedField(signalId).toBuilder()
                .id(fieldId)
                .attributes(Set.of(attribute))
                .build();
        signal.setFields(Set.of(field));

        reset(config);
        reset(planService);
        reset(signalService);
        reset(signalRepository);
        reset(planActionService);
        reset(metadataWriteService);

        lenient().when(config.getDataSource()).thenReturn(dataSource);
        lenient().when(planService.getById(planId)).thenReturn(plan);
        lenient().when(planService.getSignals(planId)).thenReturn(Set.of(signal));
        lenient().when(signalService.getByIdWithAssociationsRecursive(signalId)).thenReturn(signal);

        ReflectionTestUtils.invokeMethod(usecase, "init");
    }

    @Test
    void handleResponse_withEmptyLists_error() {
        assertThatThrownBy(() -> usecase.finalizePromotedSignals(planId, STAGING, List.of(), List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("lists are empty");
    }

    @Test
    void remotePromote_planNotFound_error() {
        when(planService.getById(planId)).thenThrow(new DataNotFoundException("not found"));

        assertThatThrownBy(() -> usecase.remotePromote(planId))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void remotePromote_invalidPlanStatus() {
        doThrow(new ForbiddenException("")).when(planActionService).validateAction(plan, PROMOTE_TO_STAGING);
        plan.setStatus(PlanStatus.CANCELED); // Invalid status

        assertThatThrownBy(() -> usecase.remotePromote(planId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void remotePromote_signalNotCompleted_error() {
        signal.setCompletionStatus(CompletionStatus.DRAFT); // Signal is not completed

        assertThatThrownBy(() -> usecase.remotePromote(planId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A Signal must be COMPLETED");
    }

    @Test
    void remotePromote_withoutSignals_error() {
        when(planService.getSignals(planId)).thenReturn(Set.of());

        assertThatThrownBy(() -> usecase.remotePromote(planId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No signals found");
    }

    @Test
    void localPromote_planNotFound_error() {
        when(planService.getById(planId)).thenThrow(new DataNotFoundException("not found"));

        assertThatThrownBy(() -> usecase.localPromote(planId))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void localPromote_invalidPlanStatus() {
        doThrow(new ForbiddenException("")).when(planActionService).validateAction(plan, PROMOTE_TO_STAGING);
        plan.setStatus(PlanStatus.CANCELED); // Invalid status

        assertThatThrownBy(() -> usecase.localPromote(planId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void localPromote_signalNotCompleted_error() {
        signal.setCompletionStatus(CompletionStatus.DRAFT); // Signal is not completed

        assertThatThrownBy(() -> usecase.localPromote(planId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A Signal must be COMPLETED");
    }

    @Test
    void localPromote_withoutSignals_error() {
        when(planService.getSignals(planId)).thenReturn(Set.of());

        assertThatThrownBy(() -> usecase.localPromote(planId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No signals found");
    }

    @Test
    void localPromote_oneDraftOneStaging_stagingFilteredOut() {
        var signalId1 = VersionedId.of(9L, MIN_VERSION);
        var signalId2 = VersionedId.of(10L, MIN_VERSION);

        var signal1 = unstagedSignal(planId).setSignalId(signalId1).toBuilder()
                .completionStatus(COMPLETED)
                .events(Set.of())
                .fields(Set.of())
                .build();

        var signal2 = unstagedSignal(planId).setSignalId(signalId2).toBuilder()
                .completionStatus(COMPLETED)
                .environment(STAGING)
                .events(Set.of())
                .fields(Set.of())
                .build();

        when(planService.getSignals(planId)).thenReturn(Set.of(signal1, signal2));
        when(signalService.getByIdWithAssociationsRecursive(signalId1)).thenReturn(signal1);
        when(signalRepository.findAllById(List.of(signalId1))).thenReturn(List.of(signal1));
        when(metricsService.getPromoteToStagingCounter()).thenReturn(counter);

        usecase.localPromote(planId);

        verify(metadataWriteService, times(1)).upsert(signal1, dataSource);
        verify(metadataWriteService, never()).upsert(signal2, dataSource);
        verify(planService, times(1)).update(plan, PROMOTE_TO_STAGING);
        verify(signalRepository, times(1)).findAllById(List.of(signalId1));
        verify(signalRepository, never()).findAllById(List.of(signalId2));
        verify(signalRepository, times(1)).saveAll(List.of(signal1));
        verify(signalRepository, never()).saveAll(List.of(signal2));

        // Capture and verify handleResponse parameters
        final ArgumentCaptor<List<VersionedIdWithStatus>> failedCaptor = ArgumentCaptor.forClass((Class) List.class);
        final ArgumentCaptor<List<VersionedIdWithStatus>> promotedCaptor = ArgumentCaptor.forClass((Class) List.class);
        verify(usecase).finalizePromotedSignals(eq(planId), eq(STAGING), failedCaptor.capture(), promotedCaptor.capture());

        assertThat(failedCaptor.getValue()).isEmpty();
        assertThat(promotedCaptor.getValue().size()).isEqualTo(1);
    }

    @Test
    void remotePromote_oneDraftOneStaging_stagingFilteredOut() {
        var signalId1 = VersionedId.of(9L, MIN_VERSION);
        var signalId2 = VersionedId.of(10L, MIN_VERSION);

        var signal1 = unstagedSignal(planId).setSignalId(signalId1).toBuilder()
                .completionStatus(COMPLETED)
                .events(Set.of())
                .fields(Set.of())
                .build();

        var signal2 = unstagedSignal(planId).setSignalId(signalId2).toBuilder()
                .completionStatus(COMPLETED)
                .environment(STAGING)
                .events(Set.of())
                .fields(Set.of())
                .build();

        when(stagingInjector.post(eq(null), anyList(), eq(signal1), eq(String.class))).thenReturn("entityId");
        when(planService.getSignals(planId)).thenReturn(Set.of(signal1, signal2));
        when(signalService.getByIdWithAssociationsRecursive(signalId1)).thenReturn(signal1);
        when(signalRepository.findAllById(List.of(signalId1))).thenReturn(List.of(signal1));
        when(metricsService.getPromoteToStagingCounter()).thenReturn(counter);

        usecase.remotePromote(planId);

        verify(stagingInjector, times(1)).post(eq(null), anyList(), eq(signal1), eq(String.class));
        verify(stagingInjector, never()).post(eq(null), anyList(), eq(signal2), eq(String.class));
        verify(planService, times(1)).update(plan, PROMOTE_TO_STAGING);
        verify(signalRepository, times(1)).findAllById(List.of(signalId1));
        verify(signalRepository, never()).findAllById(List.of(signalId2));
        verify(signalRepository, times(1)).saveAll(List.of(signal1));
        verify(signalRepository, never()).saveAll(List.of(signal2));

        // Capture and verify handleResponse parameters
        final ArgumentCaptor<List<VersionedIdWithStatus>> failedCaptor = ArgumentCaptor.forClass((Class) List.class);
        final ArgumentCaptor<List<VersionedIdWithStatus>> promotedCaptor = ArgumentCaptor.forClass((Class) List.class);
        verify(usecase).finalizePromotedSignals(eq(planId), eq(STAGING), failedCaptor.capture(), promotedCaptor.capture());

        assertThat(failedCaptor.getValue()).isEmpty();
        assertThat(promotedCaptor.getValue().size()).isEqualTo(1);
    }

    @Test
    void localPromote_allPromoted() {
        when(metadataWriteService.upsert(signal, dataSource)).thenReturn("entityId");
        when(signalRepository.findAllById(List.of(signalId))).thenReturn(List.of(signal));
        when(metricsService.getPromoteToStagingCounter()).thenReturn(counter);

        usecase.localPromote(planId);

        verify(planService, times(1)).update(plan, PROMOTE_TO_STAGING);
        verify(signalRepository, times(1)).findAllById(List.of(signalId));
        verify(signalRepository, times(1)).saveAll(List.of(signal));

        // Capture and verify handleResponse parameters
        final ArgumentCaptor<List<VersionedIdWithStatus>> failedCaptor = ArgumentCaptor.forClass((Class) List.class);
        final ArgumentCaptor<List<VersionedIdWithStatus>> promotedCaptor = ArgumentCaptor.forClass((Class) List.class);
        verify(usecase).finalizePromotedSignals(eq(planId), eq(STAGING), failedCaptor.capture(), promotedCaptor.capture());

        assertThat(failedCaptor.getValue()).isEmpty();
        assertThat(promotedCaptor.getValue().size()).isEqualTo(1);

        assertThat(signal.getDomain()).isEqualTo(plan.getDomain());
        assertThat(signal.getOwners()).isEqualTo(plan.getOwners());
    }

    @Test
    void remotePromote_allPromoted() {
        when(stagingInjector.post(eq(null), anyList(), eq(signal), eq(String.class))).thenReturn("entityId");
        when(signalRepository.findAllById(List.of(signalId))).thenReturn(List.of(signal));
        when(metricsService.getPromoteToStagingCounter()).thenReturn(counter);

        usecase.remotePromote(planId);

        verify(planService, times(1)).update(plan, PROMOTE_TO_STAGING);
        verify(signalRepository, times(1)).findAllById(List.of(signalId));
        verify(signalRepository, times(1)).saveAll(List.of(signal));

        // Capture and verify handleResponse parameters
        final ArgumentCaptor<List<VersionedIdWithStatus>> failedCaptor = ArgumentCaptor.forClass((Class) List.class);
        final ArgumentCaptor<List<VersionedIdWithStatus>> promotedCaptor = ArgumentCaptor.forClass((Class) List.class);
        verify(usecase).finalizePromotedSignals(eq(planId), eq(STAGING), failedCaptor.capture(), promotedCaptor.capture());

        assertThat(failedCaptor.getValue()).isEmpty();
        assertThat(promotedCaptor.getValue().size()).isEqualTo(1);

        assertThat(signal.getDomain()).isEqualTo(plan.getDomain());
        assertThat(signal.getOwners()).isEqualTo(plan.getOwners());
    }

    @Test
    void localPromote_allFailed() {
        when(metadataWriteService.upsert(signal, dataSource)).thenThrow(new UdcException("123", "failed"));
        when(metricsService.getPromoteToStagingCounter()).thenReturn(counter);

        assertThatThrownBy(() -> usecase.localPromote(planId))
                .isInstanceOf(ExternalCallException.class)
                .hasMessageContaining("failed");

        verify(planService, never()).update(plan, PROMOTE_TO_STAGING);
        verify(signalRepository, never()).saveAll(List.of(signal));
    }

    @Test
    void remotePromote_allFailed() {
        when(stagingInjector.post(eq(null), anyList(), eq(signal), eq(String.class))).thenThrow(new ExternalCallException("failed"));
        when(metricsService.getPromoteToStagingCounter()).thenReturn(counter);

        assertThatThrownBy(() -> usecase.remotePromote(planId))
                .isInstanceOf(ExternalCallException.class)
                .hasMessageContaining("failed");

        verify(planService, never()).update(plan, PROMOTE_TO_STAGING);
        verify(signalRepository, never()).saveAll(List.of(signal));
    }

    @Test
    void localPromote_partialSuccess() {
        var failedEvent = unstagedEvent().toBuilder()
                .id(1L)
                .name("failed")
                .build();
        var failedSignalId = VersionedId.of(9L, MIN_VERSION);
        var failedSignal = unstagedSignal(planId).setSignalId(failedSignalId).toBuilder()
                .completionStatus(COMPLETED)
                .events(Set.of(failedEvent))
                .fields(Set.of(field))
                .build();

        var successSignalId = VersionedId.of(10L, MIN_VERSION);
        var successSignal = unstagedSignal(planId).setSignalId(successSignalId).toBuilder()
                .completionStatus(COMPLETED)
                .events(Set.of(event))
                .fields(Set.of(field))
                .build();
        when(planService.getSignals(planId)).thenReturn(Set.of(failedSignal, successSignal));
        when(signalService.getByIdWithAssociationsRecursive(failedSignalId)).thenReturn(failedSignal);
        when(signalService.getByIdWithAssociationsRecursive(successSignalId)).thenReturn(successSignal);
        when(signalRepository.findAllById(List.of(successSignalId))).thenReturn(List.of(successSignal));
        when(metadataWriteService.upsert(failedSignal, dataSource)).thenThrow(new UdcException("123", "failed"));
        when(metadataWriteService.upsert(successSignal, dataSource)).thenReturn("signalEntityId");
        when(metricsService.getPromoteToStagingCounter()).thenReturn(counter);

        assertThatThrownBy(() -> usecase.localPromote(planId))
                .isInstanceOf(PartialSuccessException.class)
                .hasMessageContaining("partially failed");

        verify(planService, times(1)).update(plan, PROMOTE_TO_STAGING);
        verify(signalRepository, times(1)).findAllById(List.of(successSignalId));
        verify(signalRepository, never()).findAllById(List.of(failedSignalId));
        verify(signalRepository, times(1)).saveAll(List.of(successSignal));
        verify(signalRepository, never()).saveAll(List.of(failedSignal));

        // Capture and verify handleResponse parameters
        final ArgumentCaptor<List<VersionedIdWithStatus>> failedCaptor = ArgumentCaptor.forClass((Class) List.class);
        final ArgumentCaptor<List<VersionedIdWithStatus>> promotedCaptor = ArgumentCaptor.forClass((Class) List.class);
        verify(usecase).finalizePromotedSignals(eq(planId), eq(STAGING), failedCaptor.capture(), promotedCaptor.capture());

        assertThat(failedCaptor.getValue().size()).isEqualTo(1);
        assertThat(promotedCaptor.getValue().size()).isEqualTo(1);

        assertThat(successSignal.getDomain()).isEqualTo(plan.getDomain());
        assertThat(successSignal.getOwners()).isEqualTo(plan.getOwners());
    }

    @Test
    void remotePromote_partialSuccess() {
        var failedEvent = unstagedEvent().toBuilder()
                .id(1L)
                .name("failed")
                .build();
        var failedSignalId = VersionedId.of(9L, MIN_VERSION);
        var failedSignal = unstagedSignal(planId).setSignalId(failedSignalId).toBuilder()
                .completionStatus(COMPLETED)
                .events(Set.of(failedEvent))
                .fields(Set.of(field))
                .build();

        var successSignalId = VersionedId.of(10L, MIN_VERSION);
        var successSignal = unstagedSignal(planId).setSignalId(successSignalId).toBuilder()
                .completionStatus(COMPLETED)
                .events(Set.of(event))
                .fields(Set.of(field))
                .build();

        when(stagingInjector.post(eq(null), anyList(), eq(successSignal), eq(String.class))).thenReturn("entityId");
        when(stagingInjector.post(eq(null), anyList(), eq(failedSignal), eq(String.class))).thenThrow(new ExternalCallException("failed"));
        when(planService.getSignals(planId)).thenReturn(Set.of(failedSignal, successSignal));
        when(signalService.getByIdWithAssociationsRecursive(failedSignalId)).thenReturn(failedSignal);
        when(signalService.getByIdWithAssociationsRecursive(successSignalId)).thenReturn(successSignal);
        when(signalRepository.findAllById(List.of(successSignalId))).thenReturn(List.of(successSignal));
        when(metricsService.getPromoteToStagingCounter()).thenReturn(counter);

        assertThatThrownBy(() -> usecase.remotePromote(planId))
                .isInstanceOf(PartialSuccessException.class)
                .hasMessageContaining("partially failed");

        verify(planService, times(1)).update(plan, PROMOTE_TO_STAGING);
        verify(signalRepository, times(1)).findAllById(List.of(successSignalId));
        verify(signalRepository, never()).findAllById(List.of(failedSignalId));
        verify(signalRepository, times(1)).saveAll(List.of(successSignal));
        verify(signalRepository, never()).saveAll(List.of(failedSignal));

        // Capture and verify handleResponse parameters
        final ArgumentCaptor<List<VersionedIdWithStatus>> failedCaptor = ArgumentCaptor.forClass((Class) List.class);
        final ArgumentCaptor<List<VersionedIdWithStatus>> promotedCaptor = ArgumentCaptor.forClass((Class) List.class);
        verify(usecase).finalizePromotedSignals(eq(planId), eq(STAGING), failedCaptor.capture(), promotedCaptor.capture());

        assertThat(failedCaptor.getValue().size()).isEqualTo(1);
        assertThat(promotedCaptor.getValue().size()).isEqualTo(1);

        assertThat(successSignal.getDomain()).isEqualTo(plan.getDomain());
        assertThat(successSignal.getOwners()).isEqualTo(plan.getOwners());
    }
}
