package com.ebay.behavior.gds.mdm.udf.service.udc;

import com.ebay.behavior.gds.mdm.udf.common.model.Udf;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfStub;
import com.ebay.behavior.gds.mdm.udf.service.UdfService;
import com.ebay.behavior.gds.mdm.udf.testUtil.TestModelUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.List;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.reset;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UdcSyncServiceIT {
    private final long udfId = 1L;

    @Mock
    private UdfService udfService;
    @Autowired
    private UdfUdcSyncService service;

    @BeforeEach
    void setUp() {
        Udf udf = TestModelUtils.udf();
        udf.setId(udfId);
        udf.setRevision(0);

        UdfStub udfStub = TestModelUtils.udfStub(udfId);
        udfStub.setId(1L);
        udfStub.setRevision(0);

        udf.setUdfStubs(new HashSet<>(List.of(udfStub)));

        reset(udfService);

        lenient().when(udfService.getById(udfId, true)).thenReturn(udf);
    }

    @Test
    void test() {
        service.udcSyncUdf(udfId);
    }
}
