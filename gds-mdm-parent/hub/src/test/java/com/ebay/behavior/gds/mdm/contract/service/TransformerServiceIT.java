package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.contract.model.Transformer;
import com.ebay.behavior.gds.mdm.contract.model.UdfAlias;
import com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class TransformerServiceIT {
    @Autowired
    private TransformerService transformerService;

    private Transformer transformer;

    @BeforeAll
    void setUpAll() {
        transformer = TestModelUtils.transformer("test");
        transformer.setUdfAliases(Sets.newHashSet(
                UdfAlias.builder().name("aa").alias("a").func("afunc").build(),
                UdfAlias.builder().name("bb").alias("b").func("bfunc").build()
        ));
        transformer = transformerService.create(transformer);
    }

    @Test
    void getByIdWithAssociations_initializesAssociations() {
        var persisted = transformerService.getByIdWithAssociations(transformer.getId());
        System.out.println(persisted);
    }

    @Test
    void create() {
        System.out.println(transformer);
    }
}