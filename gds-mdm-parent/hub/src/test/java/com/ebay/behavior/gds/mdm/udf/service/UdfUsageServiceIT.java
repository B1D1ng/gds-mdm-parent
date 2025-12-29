package com.ebay.behavior.gds.mdm.udf.service;

import com.ebay.behavior.gds.mdm.udf.common.model.Udf;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfUsage;
import com.ebay.behavior.gds.mdm.udf.testUtil.TestModelUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UdfUsageServiceIT {

    @Autowired
    private UdfService udfService;

    @Autowired
    private UdfUsageService udfUsageService;

    private UdfUsage udfUsage;

    @BeforeEach
    void setUp() {
        Udf udf = TestModelUtils.udf();
        udf = udfService.create(udf);
        udf = udfService.getById(udf.getId());
        long udfId = udf.getId();

        udfUsage = TestModelUtils.udfUsage(udfId);
        udfUsage = udfUsageService.create(udfUsage);
    }

    @Test
    void create() {
        assertThat(udfUsage.getId()).isNotNull();
        assertThat(udfUsage.getUsageType()).isNotNull();
    }

    @Test
    void getByIdWithAssociations() {
        var persisted = udfUsageService.getByIdWithAssociations(udfUsage.getId());
        assertThat(persisted.getUdfId()).isNotNull();
    }
}
