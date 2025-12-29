package com.ebay.behavior.gds.mdm.commonTestUtil;

import lombok.experimental.UtilityClass;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@UtilityClass
public class JsonUtils {

    public static String loadJsonFile(String filePath) throws IOException {
        try (val inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath)) {
            Validate.isTrue(Objects.nonNull(inputStream), "inputStream is null for file: " + filePath);
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }
    }
}
