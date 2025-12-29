package com.ebay.behavior.gds.mdm.commonTestUtil;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang.RandomStringUtils;

import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;

@UtilityClass
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class TestUtils {

    public static final String INTEGRATION_TEST = "integrationTest";
    public static final String SLOW_TEST = "slow";
    public static final String RESOURCE_TEST = "resource";
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

    public static <T> RequestSpecification requestSpecWithBody(T json) {
        return given().contentType(ContentType.JSON).body(json);
    }

    public static RequestSpecification requestSpecWithBody(String json) {
        return given().contentType(ContentType.JSON).body(json);
    }

    public static RequestSpecification requestSpec() {
        return given().contentType(ContentType.JSON);
    }
}
