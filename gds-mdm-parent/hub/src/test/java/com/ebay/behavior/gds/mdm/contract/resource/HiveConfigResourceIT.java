package com.ebay.behavior.gds.mdm.contract.resource;

import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.contract.model.HiveConfig;
import com.ebay.behavior.gds.mdm.contract.model.HiveSource;
import com.ebay.behavior.gds.mdm.contract.model.HiveStorage;
import com.ebay.behavior.gds.mdm.contract.service.HiveConfigService;
import com.ebay.behavior.gds.mdm.contract.service.HiveSourceService;
import com.ebay.behavior.gds.mdm.contract.service.HiveStorageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.*;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.hiveStorage;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.hiveSource;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.hiveConfig;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.CMM;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.DEFINITION;
import static org.assertj.core.api.Assertions.assertThat;

class HiveConfigResourceIT extends AbstractResourceTest {

    private HiveConfig hiveConfig;

    private HiveSource hiveSource;

    private HiveStorage hiveStorage;

    @Autowired
    private HiveConfigService hiveConfigService;

    @Autowired
    private HiveSourceService hiveSourceService;

    @Autowired
    private HiveStorageService hiveStorageService;

    @BeforeEach
    void setup() {
        url = getBaseUrl() + V1 + CMM + DEFINITION + "/hive-config";
        hiveConfig = hiveConfig(getRandomLong());
        hiveSource = hiveSource(getRandomString());
        hiveStorage = hiveStorage(getRandomString());
    }

    @Test
    void create() {
        var sourceCreated = hiveSourceService.create(hiveSource);

        hiveConfig.setComponentId(sourceCreated.getId());
        var created = requestSpecWithBody(hiveConfig)
                .when().post(url)
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getObject(".", HiveConfig.class);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getComponentId()).isEqualTo(hiveConfig.getComponentId());
        assertThat(created.getEnv()).isEqualTo(hiveConfig.getEnv());
    }

    @Test
    void getById() {
        var sourceCreated = hiveSourceService.create(hiveSource);
        hiveConfig.setComponentId(sourceCreated.getId());
        var created = hiveConfigService.create(hiveConfig);
        var hiveConfigId = created.getId();

        var retrieved = requestSpec()
                .when().get(url + '/' + hiveConfigId)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", HiveConfig.class);

        assertThat(retrieved.getId()).isEqualTo(hiveConfigId);
    }

    @Test
    void update() {
        var sourceCreated = hiveSourceService.create(hiveSource);
        hiveConfig.setComponentId(sourceCreated.getId());
        var created = hiveConfigService.create(hiveConfig);
        var hiveConfigId = created.getId();
        var updateRequest = created.toBuilder().env(Environment.STAGING).build();

        var updated = requestSpecWithBody(updateRequest)
                .when().patch(url + '/' + hiveConfigId)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", HiveConfig.class);

        assertThat(updated.getId()).isEqualTo(hiveConfigId);
        assertThat(updated.getEnv()).isEqualTo(updateRequest.getEnv());
    }

    @Test
    void delete() {
        var hiveSourceCreated = hiveSourceService.create(hiveSource);

        hiveConfig.setComponentId(hiveSourceCreated.getId());
        var created = hiveConfigService.create(hiveConfig);
        var hiveConfigId = created.getId();

        requestSpec()
                .when().delete(url + '/' + hiveConfigId)
                .then().statusCode(HttpStatus.NO_CONTENT.value());

        requestSpec()
                .when().get(url + '/' + hiveConfigId)
                .then().statusCode(HttpStatus.EXPECTATION_FAILED.value());
    }

    @Test
    void updateStorage() {
        var sourceCreated = hiveSourceService.create(hiveSource);

        hiveConfig.setComponentId(sourceCreated.getId());
        var created = hiveConfigService.create(hiveConfig);
        var hiveConfigId = created.getId();

        var storageCreated = hiveStorageService.create(hiveStorage);
        Long storageId = storageCreated.getId(); // Example storage ID

        var updated = requestSpecWithBody(storageId)
                .when().put(url + '/' + hiveConfigId + "/storage")
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", HiveConfig.class);

        assertThat(updated.getId()).isEqualTo(hiveConfigId);
        assertThat(updated.getHiveStorage().getId()).isEqualTo(storageId);
    }
}
