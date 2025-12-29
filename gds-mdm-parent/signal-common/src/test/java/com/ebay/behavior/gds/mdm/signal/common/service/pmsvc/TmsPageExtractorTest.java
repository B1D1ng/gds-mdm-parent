package com.ebay.behavior.gds.mdm.signal.common.service.pmsvc;

import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.response.TmsPageResponse;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TmsPageExtractorTest {

    @Test
    void getPath() {
        var extractor = new TmsPageExtractor();
        assertThat(extractor.getPath()).isEqualTo("tms/searchPage");
    }

    @Test
    void getResponseType() {
        var extractor = new TmsPageExtractor();
        assertThat(extractor.getResponseType()).isEqualTo(TmsPageResponse.class);
    }
}