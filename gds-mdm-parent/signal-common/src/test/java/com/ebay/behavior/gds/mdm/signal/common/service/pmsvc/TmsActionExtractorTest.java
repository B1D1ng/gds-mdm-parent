package com.ebay.behavior.gds.mdm.signal.common.service.pmsvc;

import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.datatype.ActionV1;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TmsActionExtractorTest {

    private final TmsActionExtractor extractor = new TmsActionExtractor();

    @Test
    void getPath() {
        var extractor = new TmsActionExtractor();
        assertThat(extractor.getPath()).isEqualTo("tms/searchOnboardingEventAction");
    }

    @Test
    void get_withParams_notImplemented() {
        assertThatThrownBy(() -> extractor.get("path", null, ActionV1.class))
                .isInstanceOf(NotImplementedException.class);
    }

    @Test
    void get_notImplemented() {
        assertThatThrownBy(() -> extractor.get("path", ActionV1.class))
                .isInstanceOf(NotImplementedException.class);
    }
}