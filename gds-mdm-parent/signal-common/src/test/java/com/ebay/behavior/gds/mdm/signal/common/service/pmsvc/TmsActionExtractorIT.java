package com.ebay.behavior.gds.mdm.signal.common.service.pmsvc;

import com.ebay.behavior.gds.mdm.common.model.external.AckValue;
import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.request.SearchIn;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.EXACT_MATCH;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestUtils.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

@Tag(INTEGRATION_TEST)
@ActiveProfiles(IT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class TmsActionExtractorIT {

    @Autowired
    private TmsActionExtractor extractor;

    @Test
    void readAllProjects() {
        // cache miss
        var request = extractor.createRequest("TESTABCA", SearchIn.NAME.getValue(), EXACT_MATCH);
        var res = extractor.post(null, null, request, extractor.getResponseType());

        assertThat(res.getAck()).isEqualTo(AckValue.SUCCESS);
    }
}
