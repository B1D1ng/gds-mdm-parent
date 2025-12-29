package com.ebay.behavior.gds.mdm.contract.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.contract.model.ConfigStorageMapping;
import com.ebay.behavior.gds.mdm.contract.model.HiveConfig;
import com.ebay.behavior.gds.mdm.contract.model.HiveSource;
import com.ebay.behavior.gds.mdm.contract.model.HiveStorage;
import com.ebay.behavior.gds.mdm.contract.service.ConfigStorageMappingService;
import com.ebay.behavior.gds.mdm.contract.service.HiveConfigService;
import com.ebay.behavior.gds.mdm.contract.service.HiveSourceService;
import com.ebay.behavior.gds.mdm.contract.service.HiveStorageService;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomString;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.hiveConfig;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.hiveStorage;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.hiveSource;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.CMM;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.DEFINITION;
import static org.assertj.core.api.Assertions.assertThat;

class ConfigStorageMappingResourceIT extends AbstractResourceTest {

    private ConfigStorageMapping configStorageMapping;

    @Autowired
    private ConfigStorageMappingService service;
    @Autowired
    private HiveSourceService hiveSourceService;
    @Autowired
    private HiveConfigService hiveConfigService;
    @Autowired
    private HiveStorageService hiveStorageService;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + CMM + DEFINITION + "/config-storage-mapping";
        val hiveSource = hiveSource(getRandomString());
        val hiveStorage = hiveStorage(getRandomString());
        HiveSource createdHiveSource = hiveSourceService.create(hiveSource);
        val hiveConfig = hiveConfig(createdHiveSource.getId());

        HiveConfig createdHiveConfig = hiveConfigService.create(hiveConfig);
        HiveStorage createdHiveStorage = hiveStorageService.create(hiveStorage);

        configStorageMapping = ConfigStorageMapping.builder()
                .storageId(createdHiveStorage.getId())
                .configId(createdHiveConfig.getId())
                .build();
    }

    @Test
    void create() {
        val created = requestSpecWithBody(configStorageMapping)
                .when().post(url)
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getObject(".", ConfigStorageMapping.class);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getConfigId()).isEqualTo(configStorageMapping.getConfigId());
        assertThat(created.getStorageId()).isEqualTo(configStorageMapping.getStorageId());
    }

    @Test
    void update() {
        val createdMapping = service.create(configStorageMapping);

        val newStorage = hiveStorage(getRandomString());
        val createdNewStorage = hiveStorageService.create(newStorage);

        val updatedMapping = createdMapping.toBuilder().storageId(createdNewStorage.getId()).build();
        val updated = requestSpecWithBody(updatedMapping)
                .when().patch(url + '/' + createdMapping.getId())
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", ConfigStorageMapping.class);

        assertThat(updated.getId()).isEqualTo(createdMapping.getId());
        assertThat(updated.getStorageId()).isEqualTo(createdNewStorage.getId());
    }

    @Test
    void delete() {
        val createdMapping = service.create(configStorageMapping);

        requestSpec()
                .when().delete(url + '/' + createdMapping.getId())
                .then().statusCode(HttpStatus.NO_CONTENT.value());
    }
}
