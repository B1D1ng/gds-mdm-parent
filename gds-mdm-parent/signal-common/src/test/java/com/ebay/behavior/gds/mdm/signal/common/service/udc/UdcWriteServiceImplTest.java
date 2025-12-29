package com.ebay.behavior.gds.mdm.signal.common.service.udc;

import com.ebay.behavior.gds.mdm.common.exception.UdcException;
import com.ebay.behavior.gds.mdm.common.model.LineageParameters;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.service.udc.MetadataReadService;
import com.ebay.behavior.gds.mdm.common.service.udc.UdcEntityConverter;
import com.ebay.behavior.gds.mdm.common.service.udc.UdcIngestionService;
import com.ebay.behavior.gds.mdm.common.service.udc.UdcLineageHelper;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.service.UdcWriteServiceImpl;
import com.ebay.datagov.pushingestion.EntityVersionData;
import com.ebay.datagov.pushingestion.PushIngestionResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.VersionedId.MIN_VERSION;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.COLUMN_TRANSFORMATION;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.SIGNAL;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.TRANSFORMATION;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestModelUtils.unstagedEvent;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestModelUtils.unstagedField;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestModelUtils.unstagedSignal;
import static com.ebay.datagov.pushingestion.PushIngestionStatus.ACCEPTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UdcWriteServiceImplTest {

    @Mock
    private UdcIngestionService ingestService;

    @Spy
    @InjectMocks
    private UdcWriteServiceImpl service;

    @Mock
    private EntityVersionData signalEntity;

    @Mock
    private EntityVersionData eventEntity;

    @Mock
    private EntityVersionData fieldEntity;

    @Mock
    private EntityVersionData lineageEntity;

    @Mock
    private UdcEntityConverter entityConverter;

    @Mock
    private UdcLineageHelper lineageHelper;

    @Mock
    private MetadataReadService readService;

    private UnstagedSignal signal;
    private UnstagedField field;
    private UnstagedEvent event;
    private final UdcDataSourceType dataSource = UdcDataSourceType.TEST;

    private final String requestId = "testRequestId";
    private final String entityId = "testEntityId";

    private final PushIngestionResponse okResponse = new PushIngestionResponse(requestId, ACCEPTED, null, entityId);
    private final UdcException failedResponse = new UdcException(requestId, "Failed to ingested entity");// Simulating a failed response

    @BeforeEach
    void setUp() {
        var signalId = VersionedId.of(1L, MIN_VERSION);
        signal = unstagedSignal(1L);
        signal.setSignalId(signalId);
        event = unstagedEvent().toBuilder().id(2L).build();
        field = unstagedField(signalId).toBuilder().id(3L).build();

        ReflectionTestUtils.setField(service, "dataSource", dataSource);

        reset(service, ingestService, readService);
        ReflectionTestUtils.setField(service, "ingestService", ingestService);

        lenient().doReturn(signalEntity).when(entityConverter).toEntity(signal, dataSource);
        lenient().doReturn(eventEntity).when(entityConverter).toEntity(event, dataSource);
        lenient().doReturn(fieldEntity).when(entityConverter).toEntity(any(), any(), any());
        lenient().doReturn(lineageEntity).when(lineageHelper).toLineageEntity(any(LineageParameters.class), eq(dataSource));
        lenient().doReturn(okResponse.getPk()).when(ingestService).ingest(signalEntity, signal.getId());
        lenient().doReturn(okResponse.getPk()).when(ingestService).ingest(eventEntity, event.getId());
        lenient().doReturn(okResponse.getPk()).when(ingestService).ingest(fieldEntity, field.getId());
    }

    @Test
    void upsertSignal_withoutEvents() {
        var idMap = service.upsertSignal(Set.of(), signal);

        assertThat(idMap).hasSize(1);
        assertThat(idMap.get(signal.getEntityType())).isEqualTo(entityId);
        verify(ingestService, times(1)).ingest(any(), anyLong());
    }

    @Test
    void upsertSignal_nullEvents() {
        var idMap = service.upsertSignal(null, signal);

        assertThat(idMap).hasSize(1);
        assertThat(idMap.get(signal.getEntityType())).isEqualTo(entityId);
        verify(ingestService, times(1)).ingest(any(), anyLong());
    }

    @Test
    void upsertSignal_withLineage() {
        doReturn(okResponse.getPk()).when(ingestService).ingest(lineageEntity, 1L);

        var idMap = service.upsertSignal(Set.of(1L), signal);

        assertThat(idMap).hasSize(2);
        assertThat(idMap.get(signal.getEntityType())).isEqualTo(entityId);
        assertThat(idMap.get(TRANSFORMATION)).isEqualTo(entityId);
        verify(ingestService, times(2)).ingest(any(), anyLong());
    }

    @Test
    void upsertSignal_withRollback() {
        doThrow(failedResponse).when(ingestService).ingest(lineageEntity, 1L);
        doReturn(entityId).when(service).deleteSignalWithAssociations(eq(signal.getId()), eq(dataSource));

        assertThatThrownBy(() -> service.upsertSignal(Set.of(1L), signal))
                .isInstanceOf(UdcException.class);

        verify(ingestService, times(2)).ingest(any(), anyLong());
        verify(service, times(1)).deleteSignalWithAssociations(eq(signal.getId()), eq(dataSource));
    }

    @Test
    void upsertEvent() {
        var id = service.upsertEvent(event);

        assertThat(id).isEqualTo(entityId);
    }

    @Test
    void upsertField_withoutAttributes() {
        var idMap = service.upsertField(1L, Set.of(), field);

        assertThat(idMap).hasSize(1);
        assertThat(idMap.get(field.getEntityType())).isEqualTo(entityId);
        verify(ingestService, times(1)).ingest(any(), anyLong());
    }

    @Test
    void upsertField_nullAttributes() {
        var idMap = service.upsertField(1L, null, field);

        assertThat(idMap).hasSize(1);
        assertThat(idMap.get(field.getEntityType())).isEqualTo(entityId);
        verify(ingestService, times(1)).ingest(any(), anyLong());
    }

    @Test
    void upsertField_withLineage() {
        doReturn(okResponse.getPk()).when(ingestService).ingest(lineageEntity, 3L);

        var idMap = service.upsertField(1L, Set.of(1L), field);

        assertThat(idMap).hasSize(2);
        assertThat(idMap.get(field.getEntityType())).isEqualTo(entityId);
        assertThat(idMap.get(COLUMN_TRANSFORMATION)).isEqualTo(entityId);
        verify(ingestService, times(2)).ingest(any(), anyLong());
    }

    @Test
    void upsertField_withRollback() {
        doThrow(failedResponse).when(ingestService).ingest(lineageEntity, 3L);
        doReturn(entityId).when(service).deleteField(field.getId());

        assertThatThrownBy(() -> service.upsertField(1L, Set.of(1L), field))
                .isInstanceOf(UdcException.class);

        verify(ingestService, times(2)).ingest(any(), anyLong());
        verify(service, times(1)).deleteField(field.getId());
    }

    @Test
    void deleteSignalWithAssociations_error() {
        doThrow(new IllegalArgumentException("")).when(readService).getById(eq(SIGNAL), eq(signal.getId()), eq(UnstagedSignal.class));

        assertThatThrownBy(() -> service.deleteSignalWithAssociations(signal.getId(), UdcDataSourceType.TEST))
                .isInstanceOf(UdcException.class);
    }

    @Test
    void delete() {
        doReturn(signalEntity).when(entityConverter).toEntity(any(), any(), any(), any());
        doReturn(okResponse.getPk()).when(ingestService).ingest(signalEntity, entityId);

        var deletedEntityId = service.delete(SIGNAL, entityId);

        assertThat(deletedEntityId).isEqualTo(entityId);
    }

    @Test
    void upsertSignal_error() {
        doThrow(new UdcException("req", "msg")).when(ingestService).ingest(lineageEntity, signal.getId());

        assertThatThrownBy(() -> service.upsertSignal(Set.of(1L), signal))
                .isInstanceOf(UdcException.class);
    }

    @Test
    void upsertField_error() {
        doThrow(new UdcException("req", "msg")).when(ingestService).ingest(lineageEntity, field.getId());

        assertThatThrownBy(() -> service.upsertField(signal.getId(), Set.of(1L), field))
                .isInstanceOf(UdcException.class);
    }
}