package com.ebay.behavior.gds.mdm.common.util;

@FunctionalInterface
public interface EnvProvider {
    String getEnv(String name);
}
