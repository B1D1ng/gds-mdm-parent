package com.ebay.behavior.gds.mdm.dec.util;

import com.ebay.behavior.gds.mdm.dec.model.dto.StorageDetail;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@UtilityClass
public class TextUtils {

    @SneakyThrows
    public static String writeJson(ObjectMapper objectMapper, Object obj) {
        if (obj == null) {
            return null;
        }
        return objectMapper.writeValueAsString(obj);
    }

    @SneakyThrows
    public static <T> T readJson(ObjectMapper objectMapper, String json, Class<T> clazz) {
        if (json == null) {
            return null;
        }
        return objectMapper.readValue(json, clazz);
    }

    @SneakyThrows
    public static <T> T readJson(ObjectMapper objectMapper, String json, TypeReference<T> typeReference) {
        if (json == null) {
            return null;
        }
        return objectMapper.readValue(json, typeReference);
    }

    @SneakyThrows
    public static List<StorageDetail> readStorageDetailsFromFile(ObjectMapper objectMapper, String filePath) {
        if (filePath == null) {
            return new ArrayList<>();
        }
        try (InputStream inputStream = TextUtils.class.getResourceAsStream(filePath)) {
            return objectMapper.readValue(inputStream, new TypeReference<List<StorageDetail>>() {
            });
        } catch (Exception ex) {
            log.error("Error reading storage details from file: {}", filePath, ex);
            throw new IllegalStateException("Failed to read storage details", ex);
        }
    }

    public static String getStr(Object obj) {
        return obj == null ? null : obj.toString();
    }
}