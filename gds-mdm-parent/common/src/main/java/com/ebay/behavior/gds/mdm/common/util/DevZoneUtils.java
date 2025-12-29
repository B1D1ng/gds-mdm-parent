package com.ebay.behavior.gds.mdm.common.util;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import lombok.val;

@UtilityClass
public class DevZoneUtils {

    public static final String DEV_ZONE_CLUSTER = "sddz";
    public static final String TESS_CLUSTER_TYPE = "TESS_CLUSTER_TYPE";

    @Setter
    @Getter
    private static EnvProvider envProvider = new SystemEnvProvider();

    /**
     * Check if the current environment is a DevZone cluster.
     */
    public static boolean isDevZone() {
        val clusterType = getEnvProvider().getEnv(TESS_CLUSTER_TYPE);
        return DEV_ZONE_CLUSTER.equals(clusterType);
    }
}