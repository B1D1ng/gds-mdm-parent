package com.ebay.behavior.gds.mdm.dec.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.dec.model.Pipeline;
import com.ebay.behavior.gds.mdm.dec.service.PhysicalStorageService;
import com.ebay.behavior.gds.mdm.dec.service.PipelineService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.physicalStorage;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.pipeline;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.PIPELINE_METADATA_API;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.EXPECTATION_FAILED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class PipelineResourceIT extends AbstractResourceTest {

    @Autowired
    private PipelineService service;

    @Autowired
    private PhysicalStorageService storageService;

    private Pipeline pipeline;
    private long pipelineId;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + PIPELINE_METADATA_API;

        pipeline = pipeline();
        pipeline = service.create(pipeline);
        pipelineId = pipeline.getId();
    }

    @Test
    void getById() {
        var persisted = requestSpec().when().get(url + '/' + pipelineId)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", Pipeline.class);

        assertThat(persisted.getId()).isEqualTo(pipelineId);
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
        var pipeline = pipeline();
        var created = requestSpecWithBody(pipeline)
                .when().post(url)
                .then().statusCode(CREATED.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", Pipeline.class);

        assertThat(created.getId()).isNotNull();
    }

    @Test
    void update() {
        pipeline.setName("NewName");

        var updated = requestSpecWithBody(pipeline)
                .when().put(url + '/' + pipelineId)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", Pipeline.class);

        assertThat(updated.getId()).isEqualTo(pipelineId);
    }

    @Test
    void delete() {
        var pipeline1 = pipeline();
        pipeline1 = service.create(pipeline1);

        requestSpec().when().delete(url + '/' + pipeline1.getId())
                .then().statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void getAll() {
        var persisted = requestSpec()
                .when().get(url)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", Pipeline.class);

        assertThat(persisted).isNotEmpty();
    }

    @Test
    void getAll_PipelineId() {
        var persisted = requestSpec()
                .when().get(url + "?pipelineId=" + pipeline.getPipelineId())
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", Pipeline.class);

        assertThat(persisted).isNotEmpty();
    }

    @Test
    void getAll_StorageId() {
        var pipeline1 = pipeline();

        var storage = physicalStorage();
        storage = storageService.create(storage);
        storageService.savePipelineMappings(storage.getId(), Set.of(pipeline1), null);

        var persisted = requestSpec()
                .when().get(url + "?storageId=" + storage.getId())
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", Pipeline.class);

        assertThat(persisted).isNotEmpty();
    }
}