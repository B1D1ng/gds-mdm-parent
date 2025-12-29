package com.ebay.behavior.gds.mdm.signal.service;

import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.*;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SignalTemplateActionServiceIT {

    @Autowired
    private SignalTemplateActionService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Environment env;

    @BeforeAll
    void setUpAll() {
        if (env.matchesProfiles(IT)) { // We must not run this script if we run this class with dev/staging DB
            jdbcTemplate.execute("RUNSCRIPT FROM 'classpath:/dml/lookup.sql'");
        }
    }

    @Test
    void rebuild_page_impression() {
        val signal = service.recreate(PAGE_IMPRESSION_SIGNAL);

        assertThat(signal.getName()).isEqualTo(PAGE_IMPRESSION_SIGNAL_NAME);
        assertThat(signal.getType()).isEqualTo(PAGE_IMPRESSION_SIGNAL);
        assertThat(signal.getEvents().size()).isEqualTo(4);

        val events = signal.getEvents();
        assertThat(events).extracting("type").containsExactlyInAnyOrder(PAGE_VIEW_ENTRY, PAGE_VIEW_EXIT, PAGE_SERVE, CLIENT_PAGE_VIEW);
    }

    @Test
    void rebuild_page_impression_unsupportedType_error() {
        assertThatThrownBy(() -> service.recreate("UNSUPPORTED_SIGNAL_TYPE")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rebuild_onsite_click() {
        val signal = service.recreate(ONSITE_CLICK_SIGNAL);

        assertThat(signal.getName()).isEqualTo(ONSITE_CLICK_SIGNAL_NAME);
        assertThat(signal.getType()).isEqualTo(ONSITE_CLICK_SIGNAL);
        assertThat(signal.getEvents().size()).isEqualTo(3);

        val events = signal.getEvents();
        assertThat(events).extracting("type").containsExactlyInAnyOrder(SOJ_CLICK, MODULE_CLICK, SERVICE_CALL);
    }

    @Test
    void rebuild_offsite_click() {
        val signal = service.recreate(OFFSITE_CLICK_SIGNAL);

        assertThat(signal.getName()).isEqualTo(OFFSITE_CLICK_SIGNAL_NAME);
        assertThat(signal.getType()).isEqualTo(OFFSITE_CLICK_SIGNAL);
        assertThat(signal.getEvents().size()).isEqualTo(1);

        val events = signal.getEvents();
        assertThat(events).extracting("type").containsOnly(OFFSITE_EVENT);
    }

    @Test
    void rebuild_module_impression() {
        val signal = service.recreate(MODULE_IMPRESSION_SIGNAL);

        assertThat(signal.getName()).isEqualTo(MODULE_IMPRESSION_SIGNAL_NAME);
        assertThat(signal.getType()).isEqualTo(MODULE_IMPRESSION_SIGNAL);
        assertThat(signal.getEvents().size()).isEqualTo(1);

        val events = signal.getEvents();
        assertThat(events).extracting("type").containsOnly(MODULE_VIEW);
    }

    @Test
    void rebuild_business_outcome() {
        val signal = service.recreate(BUSINESS_OUTCOME_SIGNAL);

        assertThat(signal.getName()).isEqualTo(BUSINESS_OUTCOME_SIGNAL_NAME);
        assertThat(signal.getType()).isEqualTo(BUSINESS_OUTCOME_SIGNAL);
        assertThat(signal.getEvents().size()).isEqualTo(1);

        val events = signal.getEvents();
        assertThat(events).extracting("type").containsOnly(ROI_EVENT);
    }

    @Test
    void rebuild_ejs_signal() {
        val signal = service.recreate(EJS_SIGNAL);

        assertThat(signal.getName()).isEqualTo(EJS_SIGNAL_NAME);
        assertThat(signal.getType()).isEqualTo(EJS_SIGNAL);
        assertThat(signal.getEvents().size()).isEqualTo(1);

        val events = signal.getEvents();
        assertThat(events).extracting("type").containsOnly(CSEVENT);
    }

    @Test
    void rebuild_item_signal() {
        val signal = service.recreate(ITEM);

        assertThat(signal.getName()).isEqualTo(ITEM_SIGNAL_NAME);
        assertThat(signal.getType()).isEqualTo(ITEM);
        assertThat(signal.getEvents().size()).isEqualTo(1);

        val events = signal.getEvents();
        assertThat(events).extracting("type").containsOnly(ITEM_SERVE);
    }
}
