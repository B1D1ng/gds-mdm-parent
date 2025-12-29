package com.ebay.behavior.gds.mdm.udf.util;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class UdfUtilsTest {

    @Test
    void testToIdSet() {
        var result = UdfUtils.toIdSet("1,2");
        assertThat(result).isNotEmpty();
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.stream().toList().get(0)).isEqualTo(1L);
        assertThat(result.stream().toList().get(1)).isEqualTo(2L);
    }

    @Test
    void testIncrementStringNumber() {
        var result = UdfUtils.incrementStringNumber("1");
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo("2");
    }
}
