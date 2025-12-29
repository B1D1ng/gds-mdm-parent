package com.ebay.behavior.gds.mdm.dec.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.service.NamespaceService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.List;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.namespace;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.NAMESPACE_METADATA_API;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.EXPECTATION_FAILED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class NamespaceResourceIT extends AbstractResourceTest {

    @Autowired
    private NamespaceService namespaceService;

    private Namespace namespace;
    private long namespaceId;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + NAMESPACE_METADATA_API;
        namespace = namespace();
        namespace = namespaceService.create(namespace);
        namespaceId = namespace.getId();
    }

    @Test
    void getById() {
        var persisted = requestSpec().when().get(url + '/' + namespaceId)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", Namespace.class);

        assertThat(persisted.getId()).isEqualTo(namespaceId);
    }

    @Test
    void getById_notFound_417() {
        requestSpec()
                .when().get(url + '/' + getRandomLong())
                .then().statusCode(EXPECTATION_FAILED.value())
                .and().contentType(APPLICATION_JSON_VALUE);
    }

    @Test
    void create() {
        var namespace = namespace();
        var created = requestSpecWithBody(namespace)
                .when().post(url)
                .then().statusCode(CREATED.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", Namespace.class);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getRevision()).isEqualTo(0);
        assertThat(created.getCreateBy()).isEqualTo(created.getUpdateBy());
        assertThat(created.getUpdateDate()).isEqualTo(created.getCreateDate());
    }

    @Test
    void update() {
        namespace.setName("New Name");

        var updated = requestSpecWithBody(namespace)
                .when().put(url + String.format("/%d", namespaceId))
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", Namespace.class);

        assertThat(updated.getId()).isEqualTo(namespaceId);
        assertThat(updated.getRevision()).isEqualTo(namespace.getRevision() + 1);
    }

    @Test
    void update_notFound_417() {
        namespace = namespace().withId(getRandomLong()).withRevision(1);

        requestSpecWithBody(namespace)
                .when().put(url + String.format("/%d", namespace.getId()))
                .then().statusCode(EXPECTATION_FAILED.value())
                .and().contentType(APPLICATION_JSON_VALUE);
    }

    @Test
    void delete() {
        var namespace2 = namespace();
        namespace2 = namespaceService.create(namespace2);

        requestSpec().when().delete(url + '/' + namespace2.getId())
                .then().statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void getAll() throws JsonProcessingException {
        var json = requestSpec().when().get(url)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().asString();

        List<Namespace> namespaces = objectMapper.readValue(json, new TypeReference<List<Namespace>>() {
        });

        assertThat(namespaces.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void getAll_ByName() throws JsonProcessingException {
        var name = namespace.getName();
        var json = requestSpec().when().get(url + "?name=" + name)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().asString();

        List<Namespace> namespaces = objectMapper.readValue(json, new TypeReference<List<Namespace>>() {
        });

        assertThat(namespaces.size()).isGreaterThanOrEqualTo(1);
    }
}