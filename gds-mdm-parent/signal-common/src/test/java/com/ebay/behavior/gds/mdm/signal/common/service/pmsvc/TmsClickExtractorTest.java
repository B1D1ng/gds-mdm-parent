package com.ebay.behavior.gds.mdm.signal.common.service.pmsvc;

import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.response.TmsClickResponse;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TmsClickExtractorTest {

    @Test
    void getPath() {
        var extractor = new TmsClickExtractor();
        assertThat(extractor.getPath()).isEqualTo("tms/searchTrackingClick");
    }

    @Test
    void getResponseType() {
        var extractor = new TmsClickExtractor();
        assertThat(extractor.getResponseType()).isEqualTo(TmsClickResponse.class);
    }
}