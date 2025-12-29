package com.ebay.behavior.gds.mdm.common.testUtil;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang.RandomStringUtils;

import java.util.concurrent.TimeUnit;

@UtilityClass
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class TestUtils {

    public static final String INTEGRATION_TEST = "integrationTest";
    public static final int TEN_MILLION = 10_000_000;

    public static Long getRandomLong() {
        return (long) (Math.random() * (TEN_MILLION - 1) + 1);
    }

    public static Long getRandomLong(int max) {
        return (long) (Math.random() * (max - 1) + 1);
    }

    public static String getRandomSmallString() {
        return RandomStringUtils.randomAlphabetic(10);
    }

    public static String getRandomString() {
        return RandomStringUtils.randomAlphabetic(30);
    }

    public static String getRandomString(int count) {
        return RandomStringUtils.randomAlphabetic(count);
    }

    public static String getRandomEmail() {
        return String.format("testDL%d@ebay.com", getRandomLong(TEN_MILLION));
    }

    public static void sleep(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException ignored) {
        }
    }
}
