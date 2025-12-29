package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class OpenApiSchemaResourceIT extends AbstractResourceTest {

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1;
    }

    @Test
    void get_asJson() {
        var json = givenAuthorized()
                .when().get(url + "/openapi.json")
                .then().statusCode(HttpStatus.OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().asPrettyString();

        write(json, "schema.json");
    }

    @Test
    void get_asYaml() {
        var yaml = givenAuthorized()
                .when().get(url + "/openapi.yaml")
                .then().statusCode(HttpStatus.OK.value())
                .and().contentType("application/yaml")
                .extract().asPrettyString();

        write(yaml, "schema.yaml");
    }

    @SneakyThrows
    private static void write(String text, String fileName) {
        var path = Paths.get(System.getProperty("user.dir")).resolve("openApi").resolve(fileName);
        Files.writeString(path, text, StandardCharsets.UTF_8);
    }

    @Test
    void get_badType_error() {
        givenAuthorized()
                .when().get(url + "/openapi.abc")
                .then().statusCode(HttpStatus.NOT_FOUND.value());
    }
}