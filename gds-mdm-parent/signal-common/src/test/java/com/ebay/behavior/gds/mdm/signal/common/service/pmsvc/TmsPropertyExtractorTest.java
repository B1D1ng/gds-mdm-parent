package com.ebay.behavior.gds.mdm.signal.common.service.pmsvc;

import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.response.TmsPropertyResponse;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TmsPropertyExtractorTest {

    @Test
    void getPath() {
        var extractor = new TmsPropertyExtractor();
        assertThat(extractor.getPath()).isEqualTo("tms/searchTrackingProperty");
    }

    @Test
    void getResponseType() {
        var extractor = new TmsPropertyExtractor();
        assertThat(extractor.getResponseType()).isEqualTo(TmsPropertyResponse.class);
    }
}