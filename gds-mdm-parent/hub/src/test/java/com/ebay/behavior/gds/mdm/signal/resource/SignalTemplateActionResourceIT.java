package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalTemplate;

import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.CLIENT_PAGE_VIEW;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.PAGE_IMPRESSION_SIGNAL;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.PAGE_IMPRESSION_SIGNAL_NAME;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.PAGE_SERVE;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.PAGE_VIEW_ENTRY;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.PAGE_VIEW_EXIT;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.TEMPLATE;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SignalTemplateActionResourceIT extends AbstractResourceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Environment env;

    @BeforeAll
    void setUpAll() {
        url = getBaseUrl() + V1 + TEMPLATE + "/signal";

        if (env.matchesProfiles(IT)) { // We must not run this script if we run this class with dev/staging DB
            jdbcTemplate.execute("RUNSCRIPT FROM 'classpath:/dml/lookup.sql'");
        }
    }

    @Test
    void recreate() {
        var signal = requestSpec()
                .when().put(url + "/" + PAGE_IMPRESSION_SIGNAL + "/action/recreate")
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", SignalTemplate.class);

        assertThat(signal.getId()).isNotNull();
        assertThat(signal.getName()).isEqualTo(PAGE_IMPRESSION_SIGNAL_NAME);
        assertThat(signal.getType()).isEqualTo(PAGE_IMPRESSION_SIGNAL);
        assertThat(signal.getEvents().size()).isEqualTo(4);

        val events = signal.getEvents();
        assertThat(events).extracting("type").containsExactlyInAnyOrder(PAGE_VIEW_ENTRY, PAGE_VIEW_EXIT, PAGE_SERVE, CLIENT_PAGE_VIEW);
    }
}
