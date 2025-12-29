package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.audit.AuditLogParams;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedSignal;
import com.ebay.behavior.gds.mdm.signal.repository.StagedSignalRepository;

import jakarta.persistence.EntityManager;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.VersionedId.MIN_VERSION;
import static com.ebay.behavior.gds.mdm.common.model.audit.AuditMode.BASIC;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.stagedSignal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StagedSignalServiceTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private StagedSignalRepository signalRepository;

    @Mock
    private PlanService planService;

    @Spy
    @InjectMocks
    private StagedSignalService service;

    private StagedSignal stagedSignal;
    private final VersionedId signalId = VersionedId.of(1L, MIN_VERSION);

    @BeforeEach
    void setUp() {
        Mockito.reset(signalRepository, service, planService);
        stagedSignal = stagedSignal(1L).toBuilder()
                .revision(5)
                .environment(Environment.STAGING)
                .dataSource(UdcDataSourceType.TEST)
                .events(Set.of())
                .fields(Set.of())
                .build();
        stagedSignal.setSignalId(signalId);

        var stagedSignal = new StagedSignal();
        stagedSignal.setId(signalId.getId());
        stagedSignal.setVersion(signalId.getVersion());
    }

    @Test
    void getAuditLog_noSignals_error() {
        when(signalRepository.findAllById(signalId.getId())).thenReturn(List.of());
        var params = AuditLogParams.ofNonVersioned(signalId.getId(), BASIC);

        assertThatThrownBy(() -> service.getAuditLog(UdcDataSourceType.TEST, params))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void getAuditLog_noProductionSignals_error() {
        stagedSignal.setEnvironment(Environment.STAGING);
        when(signalRepository.findAllById(signalId.getId())).thenReturn(List.of(stagedSignal));
        var params = AuditLogParams.ofNonVersioned(signalId.getId(), BASIC);

        assertThatThrownBy(() -> service.getAuditLog(UdcDataSourceType.TEST, params))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void getAuditLog_versionedParams_error() {
        var params = AuditLogParams.ofVersioned(signalId.getId(), signalId.getVersion(), BASIC);

        assertThatThrownBy(() -> service.getAuditLog(UdcDataSourceType.TEST, params))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getAuditLog_singleSignal() {
        stagedSignal.setEnvironment(Environment.PRODUCTION);
        when(signalRepository.findAllById(signalId.getId())).thenReturn(List.of(stagedSignal));
        doReturn(stagedSignal).when(service).getByIdWithAssociationsRecursive(signalId);

        var params = AuditLogParams.ofNonVersioned(signalId.getId(), BASIC);

        val records = service.getAuditLog(UdcDataSourceType.TEST, params);

        assertThat(records).hasSize(1);
    }
}