package com.ebay.behavior.gds.mdm.common.util;

import org.junit.jupiter.api.Test;

import static com.ebay.behavior.gds.mdm.common.util.DevZoneUtils.DEV_ZONE_CLUSTER;
import static org.assertj.core.api.Assertions.assertThat;

class DevZoneUtilsTest {

    @Test
    void isDevZone_returnsTrue() {
        DevZoneUtils.setEnvProvider(name -> DEV_ZONE_CLUSTER);
        assertThat(DevZoneUtils.isDevZone()).isTrue();
    }

    @Test
    void isDevZone_returnsFalse() {
        DevZoneUtils.setEnvProvider(name -> "abc");
        assertThat(DevZoneUtils.isDevZone()).isFalse();
    }

    @Test
    void isDevZone_returnsFalse_whenClusterIsNull() {
        DevZoneUtils.setEnvProvider(name -> null);
        assertThat(DevZoneUtils.isDevZone()).isFalse();
    }
}