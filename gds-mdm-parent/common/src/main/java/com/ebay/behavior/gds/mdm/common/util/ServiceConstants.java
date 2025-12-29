package com.ebay.behavior.gds.mdm.common.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ServiceConstants {
    public static final int DEFAULT_RETRY_BACKOFF = 3_000;
    public static final int LARGE_RETRY_BACKOFF = 10_000;
    public static final int SMALL_RETRY_MAX_ATTEMPTS = 2;
    public static final int MEDIUM_RETRY_MAX_ATTEMPTS = 5;
    public static final int LARGE_RETRY_MAX_ATTEMPTS = 8;
    public static final String UNDERSCORE = "_";
    public static final String LIMIT = "limit";
    public static final String OFFSET = "offset";
}
