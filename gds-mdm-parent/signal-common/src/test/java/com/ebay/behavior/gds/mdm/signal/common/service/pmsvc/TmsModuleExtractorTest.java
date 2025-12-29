package com.ebay.behavior.gds.mdm.signal.common.service.pmsvc;

import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.response.TmsModuleResponse;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TmsModuleExtractorTest {

    @Test
    void getPath() {
        var extractor = new TmsModuleExtractor();
        assertThat(extractor.getPath()).isEqualTo("tms/searchModule");
    }

    @Test
    void getResponseType() {
        var extractor = new TmsModuleExtractor();
        assertThat(extractor.getResponseType()).isEqualTo(TmsModuleResponse.class);
    }
}