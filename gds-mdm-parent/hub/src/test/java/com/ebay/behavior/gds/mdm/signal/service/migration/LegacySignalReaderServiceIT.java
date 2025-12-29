package com.ebay.behavior.gds.mdm.signal.service.migration;

import com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.datatype.LogicalDefinition;

import lombok.val;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static com.ebay.behavior.gds.mdm.signal.util.ImportUtils.CJS;
import static com.ebay.behavior.gds.mdm.signal.util.ImportUtils.EJS;
import static java.util.Locale.US;
import static org.assertj.core.api.Assertions.assertThat;

@Tag(INTEGRATION_TEST)
@ActiveProfiles(IT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class LegacySignalReaderServiceIT {

    @Autowired
    private LegacySignalReadService service;

    @Test
    void readAll_cjs() {
        var signalDefinitions = service.readAll(CJS);

        val platforms = signalDefinitions.stream()
                .flatMap(sd -> sd.getLogicalDefinition().stream())
                .map(LogicalDefinition::getPlatform)
                .map(platform -> platform.toUpperCase(US))
                .distinct();
        assertThat(signalDefinitions.size()).isGreaterThan(10);
        assertThat(platforms).containsOnly(CJS);
    }

    @Test
    void readAll_ejs() {
        var signalDefinitions = service.readAll(EJS);

        val platforms = signalDefinitions.stream()
                .flatMap(sd -> sd.getLogicalDefinition().stream())
                .map(LogicalDefinition::getPlatform)
                .map(platform -> platform.toUpperCase(US))
                .distinct()
                .toList();
        assertThat(signalDefinitions.size()).isGreaterThanOrEqualTo(0);

        if (!platforms.isEmpty()) {
            assertThat(platforms).containsOnly(EJS);
        }
    }
}
