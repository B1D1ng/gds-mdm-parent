package com.ebay.behavior.gds.mdm.signal.service.migration;

import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.VersionedIdWithStatus;
import com.ebay.behavior.gds.mdm.common.util.RandomUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.Plan;
import com.ebay.behavior.gds.mdm.signal.common.model.PlatformLookup;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.LegacySignalRecord;
import com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.datatype.SignalDefinition;
import com.ebay.behavior.gds.mdm.signal.service.DomainLookupService;
import com.ebay.behavior.gds.mdm.signal.service.MetricsService;
import com.ebay.behavior.gds.mdm.signal.service.PlanService;
import com.ebay.behavior.gds.mdm.signal.service.PlatformLookupService;
import com.ebay.behavior.gds.mdm.signal.service.SignalImportService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedSignalService;
import com.ebay.behavior.gds.mdm.signal.usecase.PromoteToProductionUsecase;
import com.ebay.behavior.gds.mdm.signal.usecase.PromoteToStagingUsecase;
import com.ebay.behavior.gds.mdm.signal.usecase.UnstageUsecase;

import io.micrometer.core.instrument.Counter;
import io.vavr.control.Either;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static com.ebay.behavior.gds.mdm.common.model.Environment.PRODUCTION;
import static com.ebay.behavior.gds.mdm.signal.common.model.migration.SignalMigrationStatus.startStep;
import static com.ebay.behavior.gds.mdm.signal.util.ImportUtils.CJS;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedSignal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignalMigrationServiceTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private DomainLookupService domainLookupService;

    @Mock
    private LegacyMapper legacyMapper;

    @Mock
    private PlanService planService;

    @Mock
    private UnstagedSignalService unstagedSignalService;

    @Mock
    private LegacySignalReadService readService;

    @Mock
    private SignalImportService signalImportService;

    @Mock
    private PromoteToStagingUsecase promoteToStagingUsecase;

    @Mock
    private PromoteToStagingUsecase remotePromoteUsecase;

    @Mock
    private UnstageUsecase unstageUsecase;

    @Mock
    private PromoteToProductionUsecase localPromoteUsecase;

    @Spy
    @InjectMocks
    private SignalMigrationService service;

    @Mock
    private MetricsService metricsService;

    @Mock
    private Counter counter;

    @Mock
    private PlatformLookupService platformService;

    @BeforeEach
    void setUp() {
        Mockito.reset(legacyMapper, planService, unstagedSignalService, readService,
                promoteToStagingUsecase, remotePromoteUsecase, localPromoteUsecase, unstageUsecase, metricsService);
    }

    @Test
    void migrate_emptyList() {
        var emptyRecord = new LegacySignalRecord("id", List.of());
        assertThat(service.migrate(emptyRecord, 123L, false, Environment.STAGING).isEmpty()).isTrue();
    }

    @Test
    void remotePromote() {
        var signal = unstagedSignal(RandomUtils.getRandomLong(10_000));
        var signalId = RandomUtils.getRandomLong(10_000);
        signal.setVersion(1);
        signal.setId(signalId);
        signal.setSignalId(VersionedId.of(signalId, 1));

        var statuses = List.of(startStep(signal.getName(), 1));
        when(remotePromoteUsecase.remoteInjectMetadata(any(UnstagedSignal.class)))
                .thenReturn(Either.right(VersionedIdWithStatus.okStatus(signal.getId(), signal.getVersion(), signal.getName())));
        service.remotePromote(signal, 0, statuses, PRODUCTION);

        verify(unstagedSignalService, times(1)).updateEnvironment(any(VersionedId.class), eq(PRODUCTION));
    }

    @Test
    void migrateAll_noLegacySignals_emptyResult() throws Exception {
        when(readService.readAll(eq(CJS))).thenReturn(List.of());

        var migratedStatuses = service.migrateAll(CJS, true, PRODUCTION).get();

        assertThat(migratedStatuses).isEmpty();
    }

    @Test
    void migrateAll_error() {
        when(readService.readAll(eq(CJS))).thenThrow(new RuntimeException("Error"));

        var result = service.migrateAll(CJS, true, PRODUCTION);

        assertThat(result.isCompletedExceptionally()).isTrue();
    }

    @Test
    void migrate_error() throws Exception {
        PlatformLookup platformLookup = new PlatformLookup();
        platformLookup.setId(1L);
        platformLookup.setName(CJS);
        when(platformService.getByName(CJS)).thenReturn(platformLookup);
        var pageable = PageRequest.of(0, 1);
        var plan = new Plan().withId(123L);
        var page = new PageImpl<>(List.of(plan), pageable, 1);
        var signalDefinition = new SignalDefinition();
        signalDefinition.setId("123");
        signalDefinition.setVersion(1);
        when(readService.readAll(eq(CJS))).thenReturn(List.of(signalDefinition));
        when(planService.getAllByName(any(), any())).thenReturn(page);
        when(legacyMapper.mapLegacySignalRecord(any(), any()))
                .thenReturn(List.of(unstagedSignal(1L).toBuilder().id(1L).version(1).build()));
        when(metricsService.getSignalMigrationCounter()).thenReturn(counter);

        var migratedStatuses = service.migrateAll(CJS, false, PRODUCTION).get();

        assertThat(migratedStatuses).hasSize(1);
        assertThat(migratedStatuses.get(0).isFailed()).isTrue();
    }

    @Test
    void migrate_remotePromoteError() throws Exception {
        PlatformLookup platformLookup = new PlatformLookup();
        platformLookup.setId(1L);
        platformLookup.setName(CJS);
        when(platformService.getByName(CJS)).thenReturn(platformLookup);
        var pageable = PageRequest.of(0, 1);
        var plan = new Plan().withId(123L);
        var page = new PageImpl<>(List.of(plan), pageable, 1);
        var signalDefinition = new SignalDefinition();
        var unstagedSignal = unstagedSignal(1L).toBuilder().id(1L).version(1).build();
        Either<VersionedIdWithStatus, VersionedIdWithStatus> left = Either.left(VersionedIdWithStatus.failedStatus(unstagedSignal.getSignalId(), "error"));
        signalDefinition.setId("123");
        signalDefinition.setVersion(1);
        when(readService.readAll(eq(CJS))).thenReturn(List.of(signalDefinition));
        when(planService.getAllByName(any(), any())).thenReturn(page);
        when(legacyMapper.mapLegacySignalRecord(any(), any())).thenReturn(List.of(unstagedSignal));
        when(unstageUsecase.createWithAssociations(any())).thenReturn(unstagedSignal);
        when(unstagedSignalService.getByIdWithAssociationsRecursive(any())).thenReturn(unstagedSignal);
        when(remotePromoteUsecase.remoteInjectMetadata(any())).thenReturn(left);
        when(metricsService.getSignalMigrationCounter()).thenReturn(counter);

        var migratedStatuses = service.migrateAll(CJS, false, PRODUCTION).get();

        assertThat(migratedStatuses).hasSize(1);
        assertThat(migratedStatuses.get(0).isFailed()).isTrue();
    }

    @Test
    void migrate_localPromoteError() throws Exception {
        PlatformLookup platformLookup = new PlatformLookup();
        platformLookup.setId(1L);
        platformLookup.setName(CJS);
        when(platformService.getByName(CJS)).thenReturn(platformLookup);
        var pageable = PageRequest.of(0, 1);
        var plan = new Plan().withId(123L);
        var page = new PageImpl<>(List.of(plan), pageable, 1);
        var signalDefinition = new SignalDefinition();
        var unstagedSignal = unstagedSignal(1L).toBuilder().id(1L).version(1).build();
        Either<VersionedIdWithStatus, VersionedIdWithStatus> left = Either.left(VersionedIdWithStatus.failedStatus(unstagedSignal.getSignalId(), "error"));
        Either<VersionedIdWithStatus, VersionedIdWithStatus> right = Either.right(VersionedIdWithStatus.okStatus(unstagedSignal.getSignalId()));
        signalDefinition.setId("123");
        signalDefinition.setVersion(1);
        when(readService.readAll(eq(CJS))).thenReturn(List.of(signalDefinition));
        when(planService.getAllByName(any(), any())).thenReturn(page);
        when(legacyMapper.mapLegacySignalRecord(any(), any())).thenReturn(List.of(unstagedSignal));
        when(unstageUsecase.createWithAssociations(any())).thenReturn(unstagedSignal);
        when(unstagedSignalService.getByIdWithAssociationsRecursive(any())).thenReturn(unstagedSignal);
        when(remotePromoteUsecase.remoteInjectMetadata(any())).thenReturn(right);
        when(localPromoteUsecase.localInjectMetadata(any())).thenReturn(left);
        when(metricsService.getSignalMigrationCounter()).thenReturn(counter);

        var migratedStatuses = service.migrateAll(CJS, false, PRODUCTION).get();

        assertThat(migratedStatuses).hasSize(1);
        assertThat(migratedStatuses.get(0).isFailed()).isTrue();
    }

    @Test
    void migrateAll() throws Exception {

        PlatformLookup platformLookup = new PlatformLookup();
        platformLookup.setId(1L);
        platformLookup.setName(CJS);
        when(platformService.getByName(CJS)).thenReturn(platformLookup);
        var pageable = PageRequest.of(0, 1);
        var plan = new Plan().withId(123L);
        var page = new PageImpl<>(List.of(plan), pageable, 1);
        var signalDefinition = new SignalDefinition();
        signalDefinition.setId("123");
        signalDefinition.setVersion(1);
        var statuses = List.of(startStep("signal", 1));
        when(readService.readAll(eq(CJS))).thenReturn(List.of(signalDefinition));
        when(planService.getAllByName(any(), any())).thenReturn(page);
        doReturn(statuses).when(service).migrate(any(), anyLong(), anyBoolean(), eq(PRODUCTION));

        var migratedStatuses = service.migrateAll(CJS, true, PRODUCTION).get();

        assertThat(migratedStatuses).hasSize(1);
    }
}
