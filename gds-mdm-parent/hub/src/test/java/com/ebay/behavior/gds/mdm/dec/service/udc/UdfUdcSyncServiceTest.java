package com.ebay.behavior.gds.mdm.dec.service.udc;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.exception.UdcException;
import com.ebay.behavior.gds.mdm.dec.model.Dataset;
import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalStorage;
import com.ebay.behavior.gds.mdm.dec.model.Pipeline;
import com.ebay.behavior.gds.mdm.dec.model.enums.AccessType;
import com.ebay.behavior.gds.mdm.dec.model.enums.ViewType;
import com.ebay.behavior.gds.mdm.dec.service.DatasetService;
import com.ebay.behavior.gds.mdm.dec.service.LdmBaseEntityService;
import com.ebay.behavior.gds.mdm.dec.service.LdmEntityService;
import com.ebay.behavior.gds.mdm.dec.service.NamespaceService;
import com.ebay.behavior.gds.mdm.dec.service.PhysicalStorageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.LDM;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.dataset;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.ldmBaseEntity;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.ldmField;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.ldmFieldSignalMappingEmpty;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.namespace;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.MIN_VERSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UdfUdcSyncServiceTest {

    private final long baseLdmId = 1L;
    private final long derivedLdmId = 2L;
    private final long signalId = 1L;
    @Mock
    private LdmEntityService entityService;
    @Mock
    private LdmBaseEntityService baseEntityService;
    @Mock
    private DatasetService datasetService;
    @Mock
    private PhysicalStorageService physicalStorageService;
    @Mock
    private MetadataWriteService udcWriteService;
    @Mock
    private NamespaceService namespaceService;
    @Spy
    @InjectMocks
    private UdcSyncService service;
    private LdmBaseEntity baseLdm;
    private LdmBaseEntity derivedLdm;
    private Dataset dataset;
    private Namespace baseNamespace;
    private Namespace domainNamespace;
    private PhysicalStorage storage;

    @BeforeEach
    void setUp() {
        // set up base ldm
        baseNamespace = namespace();
        baseNamespace.setId(1L);

        baseLdm = ldmBaseEntity(baseNamespace.getId());
        baseLdm.setId(baseLdmId);

        var field = ldmField(1L, MIN_VERSION);
        var signalMapping = ldmFieldSignalMappingEmpty();
        signalMapping.setId(signalId);
        field.setSignalMapping(Set.of(signalMapping));

        var baseLdmRaw = LdmEntity.builder().id(1L).viewType(ViewType.RAW).version(MIN_VERSION).baseEntityId(baseLdmId).fields(Set.of(field)).build();
        var baseLdmSnapshot = LdmEntity.builder().id(2L).viewType(ViewType.SNAPSHOT).version(MIN_VERSION).baseEntityId(baseLdmId).fields(Set.of(field)).build();
        baseLdm.setViews(List.of(baseLdmRaw, baseLdmSnapshot));

        // set up derived ldm
        domainNamespace = namespace();
        domainNamespace.setId(2L);

        derivedLdm = ldmBaseEntity(domainNamespace.getId());
        derivedLdm.setId(derivedLdmId);

        var derivedLdmView = LdmEntity.builder().id(3L).viewType(ViewType.NONE).version(MIN_VERSION).baseEntityId(derivedLdmId).fields(Set.of(field)).build();
        derivedLdm.setViews(List.of(derivedLdmView));

        Pipeline pipeline = Pipeline.builder()
                .name("IT_test_pipeline_name")
                .pipelineId("IT_test_pipeline_id").build();
        storage = PhysicalStorage.builder().accessType(AccessType.HIVE)
                .storageDetails("{\"table_name\":\"gds_db.gds_table_name\"}")
                .pipelines(Set.of(pipeline))
                .build();

        // set up dataset
        dataset = dataset(derivedLdmView.getId(), derivedLdmView.getVersion(), domainNamespace.getId());
        dataset.setId(1L);
        dataset.setVersion(MIN_VERSION);

        reset(entityService);
        reset(baseEntityService);
        reset(datasetService);
        reset(physicalStorageService);
        reset(namespaceService);
        reset(udcWriteService);

        lenient().when(baseEntityService.getByIdWithAssociations(baseLdmId)).thenReturn(baseLdm);
        lenient().when(entityService.getByIdCurrentVersion(derivedLdmView.getId())).thenReturn(derivedLdmView);
        lenient().when(baseEntityService.getById(derivedLdmId)).thenReturn(derivedLdm);
        lenient().when(namespaceService.getById(baseNamespace.getId())).thenReturn(baseNamespace);
        lenient().when(datasetService.getByIdCurrentVersion(dataset.getId())).thenReturn(dataset);
        lenient().when(namespaceService.getById(domainNamespace.getId())).thenReturn(domainNamespace);
        lenient().when(physicalStorageService.getAllByDatasetId(dataset.getId(), false, null)).thenReturn(List.of(storage));
    }

    @Test
    void syncLdm_NotFound() {
        when(baseEntityService.getByIdWithAssociations(baseLdmId)).thenThrow(new DataNotFoundException("not found"));

        assertThatThrownBy(() -> service.syncLdm(baseLdmId))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void syncLdm_Failed() {
        when(udcWriteService.upsertLogicalDataModel(baseLdm, baseNamespace, Set.of(), Set.of())).thenThrow(new UdcException("123", "failed"));

        var idWithStatus = service.syncLdm(baseLdmId);
        assertThat(idWithStatus.getId()).isEqualTo(baseLdmId);
        assertThat(idWithStatus.isOk()).isFalse();
    }

    @Test
    void syncDataset_NotFound() {
        when(datasetService.getByIdCurrentVersion(dataset.getId())).thenThrow(new DataNotFoundException("not found"));

        assertThatThrownBy(() -> service.syncDataset(dataset.getId()))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void syncDataset_Failed() {
        when(udcWriteService.upsertConsumableDataset(dataset, domainNamespace, derivedLdmId, derivedLdm.getName(), storage, null))
                .thenThrow(new UdcException("123", "failed"));

        var idWithStatus = service.syncDataset(dataset.getId());
        assertThat(idWithStatus.getId()).isEqualTo(dataset.getId());
        assertThat(idWithStatus.isOk()).isFalse();
    }

    @Test
    void syncSignalLineage_NotFound() {
        when(baseEntityService.getByIdWithAssociations(baseLdmId)).thenThrow(new DataNotFoundException("not found"));

        assertThatThrownBy(() -> service.syncSignalLineage(baseLdmId, null))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void convertToLong_validInputs() {
        Set<String> input = Set.of("123", "456", "789");
        Set<Long> expected = Set.of(123L, 456L, 789L);

        Set<Long> result = service.convertToLong(input);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void convertToLong_invalidInputs() {
        Set<String> input = Set.of("abc", "123", "def");
        Set<Long> expected = Set.of(123L);

        Set<Long> result = service.convertToLong(input);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void isValidLong_validInput() {
        assertThat(service.isValidLong("123")).isTrue();
    }

    @Test
    void isValidLong_invalidInput() {
        assertThat(service.isValidLong("abc")).isFalse();
    }

    @Test
    void syncSignalLineage_Failed() {
        when(udcWriteService.upsertSignalToLdmLineage(baseLdmId, Set.of(signalId))).thenThrow(new UdcException("123", "failed"));

        var idWithStatus = service.syncSignalLineage(baseLdmId, null);
        assertThat(idWithStatus.isOk()).isFalse();
    }

    @Test
    void delete_Failed() {
        when(udcWriteService.deleteLogicalDataModel(derivedLdmId)).thenThrow(new UdcException("123", "failed"));

        var result = service.delete(LDM, String.valueOf(derivedLdmId));
        assertThat(result).isEqualTo(String.valueOf(derivedLdmId));
    }
}