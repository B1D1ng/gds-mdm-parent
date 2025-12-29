package com.ebay.behavior.gds.mdm.dec.service.udc;

import com.ebay.behavior.gds.mdm.common.exception.UdcException;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.external.udc.Entity;
import com.ebay.behavior.gds.mdm.common.service.udc.MetadataReadService;
import com.ebay.behavior.gds.mdm.common.service.udc.UdcEntityConverter;
import com.ebay.behavior.gds.mdm.common.service.udc.UdcIngestionService;
import com.ebay.behavior.gds.mdm.dec.model.Dataset;
import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalStorage;
import com.ebay.behavior.gds.mdm.dec.model.Pipeline;
import com.ebay.behavior.gds.mdm.dec.model.enums.AccessType;
import com.ebay.behavior.gds.mdm.dec.model.enums.ViewType;
import com.ebay.behavior.gds.mdm.dec.model.udc.ConsumableDataset;
import com.ebay.behavior.gds.mdm.dec.model.udc.DataStoreTableDetail;
import com.ebay.behavior.gds.mdm.dec.model.udc.LogicalDataModel;
import com.ebay.behavior.gds.mdm.dec.model.udc.LogicalField;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils;
import com.ebay.behavior.gds.mdm.dec.util.EntityUtils;
import com.ebay.datagov.pushingestion.EntityVersionData;
import com.ebay.datagov.pushingestion.PushIngestionResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.LDM;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.TRANSFORMATION;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.ldmBaseEntity;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.ldmField;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.MIN_VERSION;
import static com.ebay.datagov.pushingestion.PushIngestionStatus.ACCEPTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DecUdcWriteServiceImplTest {

    private final UdcDataSourceType dataSource = UdcDataSourceType.TEST;
    private final String requestId = "testRequestId";
    private final String entityId = "testEntityId";
    private final PushIngestionResponse okResponse = new PushIngestionResponse(requestId, ACCEPTED, null, entityId);
    private final UdcException failedResponse = new UdcException(requestId, "Failed to ingested entity");// Simulating a failed response
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private UdcIngestionService ingestService;
    @Mock
    private MetadataReadService readService;
    @Spy
    @InjectMocks
    private DecUdcWriteServiceImpl service;
    @Mock
    private EntityVersionData udcLdmEntity;
    @Mock
    private Entity udcEntity;
    @Mock
    private EntityVersionData udcFieldEntity;
    @Mock
    private EntityVersionData lineageEntity;
    @Mock
    private EntityVersionData udcDatasetEntity;
    @Mock
    private EntityVersionData udcDatasetEntityDeleted;
    @Mock
    private UdcEntityConverter entityConverter;
    private LdmBaseEntity baseLdm;
    private LdmBaseEntity derivedLdm;
    private Dataset dataset;
    private Namespace domainNamespace;
    private LogicalDataModel udcBaseLdm;
    private LogicalDataModel udcDerivedLdm;
    private ConsumableDataset udcDataset;
    private PhysicalStorage storage;

    @BeforeEach
    void setUp() {
        // set up base ldm
        Namespace baseNamespace = TestModelUtils.namespace();
        baseNamespace.setId(1L);

        baseLdm = ldmBaseEntity(baseNamespace.getId());
        baseLdm.setId(1L);

        var field = ldmField(1L, MIN_VERSION);
        var baseLdmRaw = LdmEntity.builder().id(1L).viewType(ViewType.RAW).version(MIN_VERSION).fields(Set.of(field)).build();
        var baseLdmSnapshot = LdmEntity.builder().id(2L).viewType(ViewType.SNAPSHOT).version(1).fields(Set.of(field)).build();
        baseLdm.setViews(new ArrayList<>(List.of(baseLdmRaw, baseLdmSnapshot)));
        udcBaseLdm = EntityUtils.mapToLogicalDataModel(baseLdm, baseNamespace);
        LogicalField udcField = udcBaseLdm.getFields().get(0);

        // set up derived ldm
        domainNamespace = TestModelUtils.namespace();
        domainNamespace.setId(2L);

        derivedLdm = ldmBaseEntity(domainNamespace.getId());
        derivedLdm.setId(2L);

        var derivedLdmView = LdmEntity.builder().id(3L).viewType(ViewType.NONE).version(MIN_VERSION).fields(Set.of(field)).build();
        derivedLdm.setViews(new ArrayList<>(List.of(derivedLdmView)));
        udcDerivedLdm = EntityUtils.mapToLogicalDataModel(derivedLdm, domainNamespace);

        Pipeline pipeline = Pipeline.builder()
                .name("IT_test_pipeline_name")
                .pipelineId("IT_test_pipeline_id").build();
        storage = PhysicalStorage.builder().accessType(AccessType.HIVE)
                .storageDetails("{\"table_name\":\"gds_db.gds_table_name\"}")
                .pipelines(Set.of(pipeline))
                .build();

        val pipelineIds = storage.getPipelines().stream().map(Pipeline::getPipelineId).toList();
        String pipelineId = pipelineIds.get(0);

        // set up dataset
        dataset = TestModelUtils.dataset(derivedLdmView.getId(), derivedLdmView.getVersion(), domainNamespace.getId());
        dataset.setId(1L);
        udcDataset = EntityUtils.mapToConsumableDataset(objectMapper, dataset, domainNamespace, derivedLdm.getId(), derivedLdm.getName(),
                storage.getAccessType(), EntityUtils.getStorageDetail(objectMapper, storage), pipelineId);

        ReflectionTestUtils.setField(service, "dataSource", dataSource);

        reset(service);
        reset(ingestService);
        ReflectionTestUtils.setField(service, "ingestService", ingestService);

        lenient().doReturn(udcLdmEntity).when(entityConverter).toEntity(udcBaseLdm, dataSource);
        lenient().doReturn(udcLdmEntity).when(entityConverter).toEntity(udcDerivedLdm, dataSource);
        lenient().doReturn(udcDatasetEntity).when(entityConverter).toEntity(udcDataset, dataSource);
        lenient().doReturn(udcFieldEntity).when(entityConverter).toEntity(any(), any(), any());
        lenient().doReturn(lineageEntity).when(entityConverter).toEntity(any(), any(), any(), any());
        lenient().doReturn(okResponse.getPk()).when(ingestService).ingest(udcLdmEntity, udcBaseLdm.getId());
        lenient().doReturn(okResponse.getPk()).when(ingestService).ingest(udcLdmEntity, udcDerivedLdm.getId());
        lenient().doReturn(okResponse.getPk()).when(ingestService).ingest(udcDatasetEntity, udcDataset.getId());
        lenient().doReturn(okResponse.getPk()).when(ingestService).ingest(udcFieldEntity, udcField.getCompositeId());
        lenient().doReturn(okResponse.getPk()).when(ingestService).ingest(lineageEntity, baseLdm.getId());
    }

    @Test
    void upsertLogicalDataModel_withoutUpstreamLdm() {
        var idMap = service.upsertLogicalDataModel(udcBaseLdm, Set.of());

        assertThat(idMap).hasSize(1);
        assertThat(idMap.get(udcBaseLdm.getEntityType())).isEqualTo(entityId);
        verify(ingestService, times(1)).ingest(any(), anyLong());
    }

    @Test
    void upsertLogicalDataModel_NullUpstreamLdm() {
        var idMap = service.upsertLogicalDataModel(udcBaseLdm, null);

        assertThat(idMap).hasSize(1);
        assertThat(idMap.get(udcBaseLdm.getEntityType())).isEqualTo(entityId);
        verify(ingestService, times(1)).ingest(any(), anyLong());
    }

    @Test
    void upsertLogicalDataModel_withLineage() {
        doReturn(okResponse.getPk()).when(ingestService).ingest(lineageEntity, udcDerivedLdm.getId());

        var idMap = service.upsertLogicalDataModel(udcDerivedLdm, Set.of(baseLdm.getId()));

        assertThat(idMap).hasSize(2);
        assertThat(idMap.get(udcDerivedLdm.getEntityType())).isEqualTo(entityId);
        assertThat(idMap.get(TRANSFORMATION)).isEqualTo(entityId);
        verify(ingestService, times(2)).ingest(any(), anyLong());
    }

    @Test
    void upsertLogicalDataModel_withRollback() {
        doThrow(failedResponse).when(ingestService).ingest(lineageEntity, udcDerivedLdm.getId());
        doReturn(entityId).when(service).deleteLogicalDataModel(udcDerivedLdm.getId());

        assertThatThrownBy(() -> service.upsertLogicalDataModel(udcDerivedLdm, Set.of(baseLdm.getId())))
                .isInstanceOf(UdcException.class);

        verify(ingestService, times(2)).ingest(any(), anyLong());
        verify(service, times(1)).deleteLogicalDataModel(udcDerivedLdm.getId());
    }

    @Test
    void upsertLogicalDataModel_withLineageUpdate() {
        doReturn(okResponse.getPk()).when(ingestService).ingest(lineageEntity, udcDerivedLdm.getId());
        doReturn(udcEntity).when(readService).getEntityById(LDM, udcDerivedLdm.getId());

        service.upsertLogicalDataModel(udcDerivedLdm, Set.of(baseLdm.getId()));

        var derivedLdm2 = derivedLdm;
        derivedLdm2.setViews(new ArrayList<>());
        var udcDerivedLdm2 = EntityUtils.mapToLogicalDataModel(derivedLdm2, domainNamespace);

        lenient().doReturn(udcLdmEntity).when(entityConverter).toEntity(udcDerivedLdm2, dataSource);
        var resultId = service.upsertLogicalDataModel(derivedLdm2, domainNamespace, Set.of(100L), Set.of(baseLdm.getId()));

        assertThat(resultId).isEqualTo(entityId);
        verify(ingestService, times(5)).ingest(any(), anyLong());
    }

    @Test
    void upsertLogicalDataModel_emptyUpstreamIds() {
        doReturn(okResponse.getPk()).when(ingestService).ingest(lineageEntity, udcDerivedLdm.getId());
        doReturn(udcEntity).when(readService).getEntityById(LDM, udcDerivedLdm.getId());

        service.upsertLogicalDataModel(udcDerivedLdm, Set.of(baseLdm.getId()));

        var derivedLdm2 = derivedLdm;
        derivedLdm2.setViews(new ArrayList<>());
        var udcDerivedLdm2 = EntityUtils.mapToLogicalDataModel(derivedLdm2, domainNamespace);

        lenient().doReturn(udcLdmEntity).when(entityConverter).toEntity(udcDerivedLdm2, dataSource);
        var resultId = service.upsertLogicalDataModel(derivedLdm2, domainNamespace, Set.of(), Set.of(baseLdm.getId()));

        assertThat(resultId).isEqualTo(entityId);
        verify(ingestService, times(4)).ingest(any(), anyLong());
    }

    @Test
    void upsertDatasetPhysicalRelation_withLineageUpdate() {
        doReturn(okResponse.getPk()).when(ingestService).ingest(lineageEntity, udcDataset.getId());

        var idMap = service.upsertDatasetPhysicalRelation(udcDataset);

        assertThat(idMap).hasSize(1);
        assertThat(idMap.get(TRANSFORMATION)).isEqualTo(entityId);
        verify(ingestService, times(1)).ingest(any(), anyLong());
    }

    @Test
    void upsertDatasetPhysicalRelation_withUnsupportedAccessType() {
        ConsumableDataset unsupportedDataset = mock(ConsumableDataset.class);
        doReturn(null).when(unsupportedDataset).getAccessType();
        DataStoreTableDetail detail = new  DataStoreTableDetail();
        detail.setDataStoreName("iceberg_store");
        detail.setDataStoreType("iceberg");
        detail.setDataTableName("iceberg_table");
        doReturn(detail).when(unsupportedDataset).getStoreTableDetail();
        assertThatThrownBy(() -> service.upsertDatasetPhysicalRelation(unsupportedDataset))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unsupported access type");
    }

    @Test
    void upsertDatasetPhysicalRelation_withHiveAccessType() {
        ConsumableDataset hiveDataset = mock(ConsumableDataset.class);
        doReturn(AccessType.HIVE).when(hiveDataset).getAccessType();
        doReturn(okResponse.getPk()).when(ingestService).ingest(any(), anyLong());
        DataStoreTableDetail detail = new  DataStoreTableDetail();
        detail.setDataStoreName("hive_store");
        detail.setDataStoreType("hive");
        detail.setDataTableName("hive_table");
        doReturn(detail).when(hiveDataset).getStoreTableDetail();

        var idMap = service.upsertDatasetPhysicalRelation(hiveDataset);

        assertThat(idMap).hasSize(1);
        assertThat(idMap.get(TRANSFORMATION)).isEqualTo(entityId);
        verify(ingestService, times(1)).ingest(any(), anyLong());
    }

    @Test
    void upsertDatasetPhysicalRelation_withIcebergAccessType() {
        ConsumableDataset icebergDataset = mock(ConsumableDataset.class);
        doReturn(AccessType.ICEBERG).when(icebergDataset).getAccessType();
        doReturn(okResponse.getPk()).when(ingestService).ingest(any(), anyLong());
        DataStoreTableDetail detail = new  DataStoreTableDetail();
        detail.setDataStoreName("iceberg_store");
        detail.setDataStoreType("iceberg");
        detail.setDataTableName("iceberg_table");
        doReturn(detail).when(icebergDataset).getStoreTableDetail();

        var idMap = service.upsertDatasetPhysicalRelation(icebergDataset);

        assertThat(idMap).hasSize(1);
        assertThat(idMap.get(TRANSFORMATION)).isEqualTo(entityId);
        verify(ingestService, times(1)).ingest(any(), anyLong());
    }

    @Test
    void upsertDatasetPhysicalRelation_withStreamAccessType() {
        ConsumableDataset streamDataset = mock(ConsumableDataset.class);
        doReturn(AccessType.STREAM).when(streamDataset).getAccessType();
        DataStoreTableDetail detail = new  DataStoreTableDetail();
        detail.setDataStoreName("stream_store");
        detail.setDataStoreType("stream");
        detail.setDataTableName("stream_table");
        doReturn(detail).when(streamDataset).getStoreTableDetail();
        doReturn(okResponse.getPk()).when(ingestService).ingest(any(), anyLong());

        var idMap = service.upsertDatasetPhysicalRelation(streamDataset);

        assertThat(idMap).hasSize(1);
        assertThat(idMap.get(TRANSFORMATION)).isEqualTo(entityId);
        verify(ingestService, times(1)).ingest(any(), anyLong());
    }

    @Test
    void upsertConsumableDataset_withLineageUpdate() {
        doReturn(okResponse.getPk()).when(ingestService).ingest(lineageEntity, udcDataset.getId());

        service.upsertConsumableDataset(udcDataset);

        // update lineage and upsert again
        dataset.setLdmEntityId(baseLdm.getId());
        val pipelineIds = storage.getPipelines().stream().map(Pipeline::getPipelineId).toList();
        String pipelineId = pipelineIds.get(0);
        var udcDataset2 = EntityUtils.mapToConsumableDataset(objectMapper, dataset, domainNamespace, baseLdm.getId(), baseLdm.getName(),
                storage.getAccessType(), EntityUtils.getStorageDetail(objectMapper, storage), pipelineId);
        lenient().doReturn(udcDatasetEntity).when(entityConverter).toEntity(udcDataset2, dataSource);

        var idMap = service.upsertConsumableDataset(dataset, domainNamespace, baseLdm.getId(), baseLdm.getName(),
                storage, derivedLdm.getId());

        assertThat(idMap).hasSize(2);
        assertThat(idMap.get(udcDataset.getEntityType())).isEqualTo(entityId);
        verify(ingestService, times(7)).ingest(any(), anyLong());
    }

    @Test
    void upsertConsumableDataset_withException() {
        lenient().doThrow(failedResponse).when(ingestService).ingest(lineageEntity, udcDataset.getId());
        lenient().doReturn(Map.of()).when(service).upsertDatasetPhysicalRelation(udcDataset);

        assertThatThrownBy(() -> service.upsertConsumableDataset(dataset, domainNamespace, derivedLdm.getId(), derivedLdm.getName(),
                storage, null)).isInstanceOf(Exception.class);

        verify(ingestService, times(3)).ingest(any(), anyLong());
    }

    @Test
    void upsertConsumableDataset() {
        doReturn(okResponse.getPk()).when(ingestService).ingest(lineageEntity, udcDataset.getId());

        var idMap = service.upsertConsumableDataset(udcDataset);

        assertThat(idMap).hasSize(2);
        assertThat(idMap.get(udcDataset.getEntityType())).isEqualTo(entityId);
        verify(ingestService, times(3)).ingest(any(), anyLong());
    }

    @Test
    void upsertConsumableDataset_withRollback() {
        lenient().doThrow(failedResponse).when(ingestService).ingest(lineageEntity, udcDataset.getId());
        lenient().doReturn(Map.of()).when(service).upsertDatasetPhysicalRelation(udcDataset);
        lenient().doReturn(entityId).when(service).deleteConsumableDataset(udcDataset.getId());

        assertThatThrownBy(() -> service.upsertConsumableDataset(udcDataset))
                .isInstanceOf(UdcException.class);

        verify(ingestService, times(2)).ingest(any(), anyLong());
        verify(service, times(1)).deleteConsumableDataset(udcDataset.getId());
    }

    @Test
    void upsertConsumableDataset_successfulUpsert() {
        doReturn(okResponse.getPk()).when(ingestService).ingest(any(), anyLong());

        var idMap = service.upsertConsumableDataset(udcDataset);

        assertThat(idMap).hasSize(2);
        assertThat(idMap.get(udcDataset.getEntityType())).isEqualTo(entityId);
        verify(ingestService, times(3)).ingest(any(), anyLong());
    }

    @Test
    void upsertConsumableDataset_exceptionHandling() {
        doThrow(failedResponse).when(ingestService).ingest(any(), anyLong());

        assertThatThrownBy(() -> service.upsertConsumableDataset(udcDataset))
                .isInstanceOf(UdcException.class);

        verify(ingestService, times(1)).ingest(any(), anyLong());
    }

    @Test
    void upsertConsumableDataset_lineageUpdate() {
        doReturn(okResponse.getPk()).when(ingestService).ingest(any(), anyLong());

        service.upsertConsumableDataset(udcDataset);

        dataset.setLdmEntityId(baseLdm.getId());
        val pipelineIds = storage.getPipelines().stream().map(Pipeline::getPipelineId).toList();
        String pipelineId = pipelineIds.get(0);
        var udcDataset2 = EntityUtils.mapToConsumableDataset(objectMapper, dataset, domainNamespace, baseLdm.getId(), baseLdm.getName(),
                storage.getAccessType(), EntityUtils.getStorageDetail(objectMapper, storage), pipelineId);
        lenient().doReturn(udcDatasetEntity).when(entityConverter).toEntity(udcDataset2, dataSource);

        var idMap = service.upsertConsumableDataset(dataset, domainNamespace, baseLdm.getId(), baseLdm.getName(),
                storage, derivedLdm.getId());

        assertThat(idMap).hasSize(2);
        assertThat(idMap.get(udcDataset.getEntityType())).isEqualTo(entityId);
        verify(ingestService, times(7)).ingest(any(), anyLong());
    }

    @Test
    void upsertSignalToLdmLineage() {
        var idMap = service.upsertSignalToLdmLineage(baseLdm.getId(), Set.of(1L));

        assertThat(idMap).hasSize(1);
        assertThat(idMap.get(TRANSFORMATION)).isEqualTo(entityId);
        verify(ingestService, times(1)).ingest(any(), anyLong());
    }

    @Test
    void deleteConsumableDataset() {
        val udcDatasetDeleted = new ConsumableDataset();
        udcDatasetDeleted.setConsumableDatasetId(udcDataset.getId());
        lenient().doReturn(udcDatasetEntityDeleted).when(entityConverter).toDeleteEntity(udcDatasetDeleted, dataSource);
        doReturn(okResponse.getPk()).when(ingestService).ingest(udcDatasetEntityDeleted, udcDataset.getId());

        var id = service.deleteConsumableDataset(udcDataset.getId());

        assertThat(id).isEqualTo(entityId);
        verify(ingestService, times(1)).ingest(any(), anyLong());
    }

    @Test
    void deleteLineage() {
        var deletedId = "transformation:123";
        lenient().doReturn(lineageEntity).when(entityConverter)
                .toEntity(TRANSFORMATION, dataSource, Map.of(TRANSFORMATION.getIdName(), deletedId), Map.of());

        var lineageEntityDeleted = lineageEntity;
        lineageEntityDeleted.setDeleted(true);
        doReturn(okResponse.getPk()).when(ingestService).ingest(lineageEntityDeleted, deletedId);

        var id = service.deleteLineage(TRANSFORMATION, deletedId);

        assertThat(id).isEqualTo(entityId);
        verify(ingestService, times(1)).ingest(any(), anyString());
    }
}
