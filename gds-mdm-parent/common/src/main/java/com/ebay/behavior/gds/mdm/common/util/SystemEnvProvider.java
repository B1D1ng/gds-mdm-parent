package com.ebay.behavior.gds.mdm.common.util;

public class SystemEnvProvider implements EnvProvider {

    @Override
    public String getEnv(String name) {
        return System.getenv(name);
    }
}
