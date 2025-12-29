package com.ebay.behavior.gds.mdm.udf.service.udc;

import com.ebay.behavior.gds.mdm.common.exception.UdcException;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;
import com.ebay.behavior.gds.mdm.common.service.udc.MetadataReadService;
import com.ebay.behavior.gds.mdm.common.service.udc.UdcEntityConverter;
import com.ebay.behavior.gds.mdm.common.service.udc.UdcIngestionService;
import com.ebay.behavior.gds.mdm.udf.common.model.UdcUdf;
import com.ebay.behavior.gds.mdm.udf.common.model.Udf;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfStub;
import com.ebay.behavior.gds.mdm.udf.testUtil.TestModelUtils;
import com.ebay.behavior.gds.mdm.udf.util.UdfMetadataUtils;
import com.ebay.datagov.pushingestion.EntityVersionData;
import com.ebay.datagov.pushingestion.PushIngestionResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.ebay.datagov.pushingestion.PushIngestionStatus.ACCEPTED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.reset;


@ExtendWith(MockitoExtension.class)
class UdfUdcWriteServiceImplTest {
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
    @Mock
    private UdcEntityConverter entityConverter;
    @Spy
    @InjectMocks
    private UdfUdcWriteServiceImpl service;
    @Mock
    private EntityVersionData udcUdfEntity;
    private UdcUdf udcUdf;

    @BeforeEach
    void setUp() {
        Udf udf = TestModelUtils.udf();
        udf.setId(1L);
        udf.setRevision(0);

        UdfStub udfStub = TestModelUtils.udfStub(1L);
        udfStub.setId(1L);
        udfStub.setRevision(0);

        udf.setUdfStubs(new HashSet<>(List.of(udfStub)));

        udcUdf = UdfMetadataUtils.convert(udf);

        ReflectionTestUtils.setField(service, "dataSource", dataSource);
        reset(service);
        reset(ingestService);
        ReflectionTestUtils.setField(service, "ingestService", ingestService);

        lenient().doReturn(udcUdfEntity).when(entityConverter).toEntity(any(), any(), any());
        lenient().doReturn(udcUdfEntity).when(entityConverter).toEntity(any(), any(), any(), any());

        lenient().doReturn(okResponse.getPk()).when(ingestService).ingest(udcUdfEntity, udf.getId());
    }

    @Test
    void testUpsertMetadata() {
        String deletedId = "1";
        lenient().doReturn(udcUdfEntity).when(entityConverter).toEntity(UdcEntityType.UDF, dataSource, Map.of(UdcEntityType.UDF.getIdName(), deletedId), Map.of());
        var udfEntityDeleted = udcUdfEntity;
        udfEntityDeleted.setDeleted(true);

        service.upsertUdf(udcUdf);
    }
}
