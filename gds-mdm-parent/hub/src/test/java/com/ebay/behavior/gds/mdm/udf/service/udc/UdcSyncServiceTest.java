package com.ebay.behavior.gds.mdm.udf.service.udc;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.exception.UdcException;
import com.ebay.behavior.gds.mdm.udf.common.model.Udf;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfStub;
import com.ebay.behavior.gds.mdm.udf.service.UdfService;
import com.ebay.behavior.gds.mdm.udf.testUtil.TestModelUtils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.reset;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class UdcSyncServiceTest {
    private final String udfUdcEntityId = "1";
    private final long udfId = 1L;

    @Mock
    private UdfMetadataWriteService metadataWriteService;
    @Mock
    private UdfService udfService;
    @Spy
    @InjectMocks
    private UdfUdcSyncService service;

    @BeforeEach
    void setUp() {
        Udf udf = TestModelUtils.udf();
        udf.setId(udfId);
        udf.setRevision(0);

        UdfStub udfStub = TestModelUtils.udfStub(1L);
        udfStub.setId(1L);
        udfStub.setRevision(0);

        udf.setUdfStubs(new HashSet<>(List.of(udfStub)));

        reset(metadataWriteService);
        reset(udfService);
        reset(service);

        lenient().when(metadataWriteService.upsertUdf(any())).thenReturn(udfUdcEntityId);
        lenient().when(udfService.getById(udfId, true)).thenReturn(udf);
    }

    @Test
    void testUdcSyncUdf() {
        var result = service.udcSyncUdf(udfId);
        assertThat(result.getId()).isEqualTo(udfId);
        assertThat(result.getHttpStatusCode()).isEqualTo(200);
        assertThat(result.isOk()).isEqualTo(true);
    }

    @Test
    void testDataNotFoundException() {
        lenient().when(metadataWriteService.upsertUdf(any())).thenThrow(new DataNotFoundException("not found"));
        var result = service.udcSyncUdf(udfId);
        assertThat(result.getId()).isEqualTo(udfId);
        assertThat(result.getHttpStatusCode()).isEqualTo(500);
        assertThat(result.isFailed()).isEqualTo(true);
    }

    @Test
    void testUdcException() {
        lenient().when(metadataWriteService.upsertUdf(any())).thenThrow(new UdcException("request", "error"));
        var result = service.udcSyncUdf(udfId);
        assertThat(result.getId()).isEqualTo(udfId);
        assertThat(result.getHttpStatusCode()).isEqualTo(500);
        assertThat(result.isFailed()).isEqualTo(true);
    }
}
