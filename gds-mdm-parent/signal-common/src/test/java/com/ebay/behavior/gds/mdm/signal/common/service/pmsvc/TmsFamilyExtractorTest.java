package com.ebay.behavior.gds.mdm.signal.common.service.pmsvc;

import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.response.TmsFamilyResponse;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TmsFamilyExtractorTest {

    @Test
    void getPath() {
        var extractor = new TmsFamilyExtractor();
        assertThat(extractor.getPath()).isEqualTo("tms/searchOnboardingEventFamily");
    }

    @Test
    void getResponseType() {
        var extractor = new TmsFamilyExtractor();
        assertThat(extractor.getResponseType()).isEqualTo(TmsFamilyResponse.class);
    }
}