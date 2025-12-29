package com.ebay.behavior.gds.mdm.common.service.udc;


import com.ebay.behavior.gds.mdm.common.exception.UdcException;
import com.ebay.datagov.pushingestion.EntityVersionData;
import com.ebay.datagov.pushingestion.PushIngestionResponse;
import com.ebay.datagov.pushingestion.PushIngestionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.ebay.datagov.pushingestion.PushIngestionStatus.ACCEPTED;
import static com.ebay.datagov.pushingestion.PushIngestionStatus.FAILED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class UdcIngestionServiceTest {

    @Mock
    private PushIngestionService ingestionService;

    @Mock
    private EntityVersionData entity;

    @Mock
    private PushIngestionResponse response;

    @InjectMocks
    private UdcIngestionService service;

    private static final String TEST_PK = "testPk";

    @BeforeEach
    void setUp() {
        setField(service, "ingestionService", ingestionService);
        lenient().when(response.getPk()).thenReturn(TEST_PK);
        lenient().when(entity.isDeleted()).thenReturn(false);
        when(ingestionService.ingest(entity)).thenReturn(response);
    }

    @Test
    void ingest_ok() {
        when(response.getStatus()).thenReturn(ACCEPTED);

        var result = service.ingest(entity, 1L);

        assertThat(result).isEqualTo(TEST_PK);
    }

    @Test
    void ingest_deleted_ok() {
        when(response.getStatus()).thenReturn(ACCEPTED);
        when(entity.isDeleted()).thenReturn(true);

        var result = service.ingest(entity, "1");

        assertThat(result).isEqualTo(TEST_PK);
    }

    @Test
    void ingest_failed_error() {
        when(response.getStatus()).thenReturn(FAILED);
        when(response.getRequestId()).thenReturn("requestId");
        when(response.getErrorMsg()).thenReturn("errorMsg");

        assertThatThrownBy(() -> service.ingest(entity, "1"))
                .isInstanceOf(UdcException.class)
                .hasMessageContaining("errorMsg");
    }
}