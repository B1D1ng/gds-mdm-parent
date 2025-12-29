package com.ebay.behavior.gds.mdm.udf.util;

import com.ebay.behavior.gds.mdm.udf.common.model.UdcUdf;
import com.ebay.behavior.gds.mdm.udf.common.model.Udf;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfStub;
import com.ebay.behavior.gds.mdm.udf.testUtil.TestModelUtils;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.List;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UdfMetadataUtilsIT {

    @Test
    void test() {
        Udf udf = TestModelUtils.udf();
        udf.setId(1L);
        udf.setRevision(0);

        UdfStub stub = TestModelUtils.udfStub(1L);
        stub.setId(1L);
        stub.setRevision(0);

        udf.setUdfStubs(new HashSet<>(List.of(stub)));

        UdcUdf udcUdf = UdfMetadataUtils.convert(udf);

        assertThat(udcUdf).isNotNull();
        assertThat(udcUdf.getUdfId()).isEqualTo(1L);
        assertThat(udcUdf.getStubs()).isNotEmpty();
        assertThat(udcUdf.getStubs().size()).isEqualTo(1);
    }
}
