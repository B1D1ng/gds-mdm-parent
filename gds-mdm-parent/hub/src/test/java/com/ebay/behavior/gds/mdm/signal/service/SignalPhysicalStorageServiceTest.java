package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedSignal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.Environment.STAGING;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.PAGE_IMPRESSION_SIGNAL;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.physicalStorage;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.signalType;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.stagedSignal;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class SignalPhysicalStorageServiceTest {

    @Mock
    private StagedSignalService signalService;

    @Mock
    private SignalTypeLookupService signalTypeLookupService;

    @Spy
    @InjectMocks
    private SignalPhysicalStorageService service;

    private final VersionedId signalId = VersionedId.of(1L, 1);

    private final StagedSignal signal = stagedSignal(1L).toBuilder()
            .id(signalId.getId())
            .version(signalId.getVersion())
            .environment(STAGING)
            .type(PAGE_IMPRESSION_SIGNAL)
            .build();

    @Test
    void getBySignalId_multipleStorages_error() {
        var storage1 = physicalStorage().toBuilder().environment(STAGING).build();
        var storage2 = physicalStorage().toBuilder().environment(STAGING).build();
        var lookup = signalType().toBuilder().physicalStorages(Set.of(storage1, storage2)).build();
        doReturn(signal).when(signalService).getById(signalId);
        doReturn(lookup).when(signalTypeLookupService).getByName(signal.getType());

        assertThatThrownBy(() -> service.getBySignalId(signalId))
                .isInstanceOf(IllegalStateException.class);
    }
}
