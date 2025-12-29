package com.ebay.behavior.gds.mdm.udf.service;

import com.ebay.behavior.gds.mdm.udf.common.model.UdfStubModule;
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
class UdfStubModuleServiceIT {

    @Autowired
    private UdfStubModuleService udfStubModuleService;

    private UdfStubModule udfStubModule;

    @BeforeEach
    void setUp() {
        udfStubModule = TestModelUtils.udfStubModule();
    }

    @Test
    void create() {
        UdfStubModule saved = udfStubModuleService.create(udfStubModule);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUdfStubModuleName()).isEqualTo("tracking-lib-spark");
    }
}
