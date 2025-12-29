package com.ebay.behavior.gds.mdm.signal.service.migration;

import com.ebay.behavior.gds.mdm.signal.service.PlatformLookupService;
import com.ebay.behavior.gds.mdm.signal.service.StagedSignalService;
import com.ebay.behavior.gds.mdm.signal.service.StagingSyncClient;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedSignalService;

import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.ExecutionException;

import static com.ebay.behavior.gds.mdm.common.model.Environment.PRODUCTION;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static com.ebay.behavior.gds.mdm.signal.util.ImportUtils.CJS;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.CJS_PLATFORM_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@Tag(INTEGRATION_TEST)
@ActiveProfiles(IT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SignalMigrationServiceIT {

    @MockitoBean
    private StagingSyncClient stagingInjector;

    @Autowired
    private SignalMigrationService service;

    @Autowired
    private StagedSignalService stagedSignalService;

    @Autowired
    private UnstagedSignalService unstagedSignalService;

    @Mock
    private PlatformLookupService platformService;

    @BeforeAll
    void setUpAll() {
        doReturn("signal:0").when(stagingInjector).post(any(), anyList(), any(), any());
    }

    @Test
    void migrate_byName() {
        when(platformService.getPlatformId(CJS)).thenReturn(CJS_PLATFORM_ID);
        val name = "Action_Navigational_Click_XO_Member_Shipping_Address";
        val statuses = service.migrate(name, CJS, true, PRODUCTION);
        assertThat(statuses.size()).isGreaterThanOrEqualTo(1);

        statuses.forEach(status -> {
            assertThat(status.getSignalName()).isEqualTo(name);
            assertThat(status.isOk()).isTrue();

            val migratedUnstaged = unstagedSignalService.getByIdWithAssociationsRecursive(status.toVersionedId());
            assertThat(migratedUnstaged.getEnvironment()).isEqualTo(PRODUCTION);

            val migratedStaged = stagedSignalService.getById(status.toVersionedId());
            assertThat(migratedStaged.getEnvironment()).isEqualTo(PRODUCTION);
            assertThat(migratedStaged.getCreateBy()).isEqualTo(migratedUnstaged.getCreateBy());
            assertThat(migratedStaged.getUpdateBy()).isEqualTo(migratedUnstaged.getUpdateBy());
        });
    }

    // This is a long-running test, that not required on every build. We can run it manually, if needed.
    @Test
    @Disabled
    void migrateAll() throws ExecutionException, InterruptedException {
        val statuses = service.migrateAll(CJS, false, PRODUCTION).get();
        assertThat(statuses).hasSizeGreaterThanOrEqualTo(100);
    }
}
