package com.ebay.behavior.gds.mdm.contract.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.contract.model.HiveStorage;
import com.ebay.behavior.gds.mdm.contract.service.HiveStorageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomString;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.hiveStorage;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.CMM;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.DEFINITION;
import static org.assertj.core.api.Assertions.assertThat;

class HiveStorageResourceIT extends AbstractResourceTest {

    private HiveStorage hiveStorage;

    @Autowired
    private HiveStorageService hiveStorageService;

    @BeforeEach
    void setup() {
        url = getBaseUrl() + V1 + CMM + DEFINITION + "/hive-storage";
        hiveStorage = hiveStorage(getRandomString());
    }

    @Test
    void create() {
        HiveStorage created = requestSpecWithBody(hiveStorage)
                .when().post(url)
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getObject(".", HiveStorage.class);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getTableName()).isEqualTo(hiveStorage.getTableName());
        assertThat(created.getDbName()).isEqualTo(hiveStorage.getDbName());
    }

    @Test
    void getById() {
        HiveStorage created = hiveStorageService.create(hiveStorage);
        Long hiveStorageId = created.getId();

        HiveStorage retrieved = requestSpec()
                .when().get(url + '/' + hiveStorageId)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", HiveStorage.class);

        assertThat(retrieved.getId()).isEqualTo(created.getId());
        assertThat(retrieved.getDbName()).isEqualTo(created.getDbName());
        assertThat(retrieved.getTableName()).isEqualTo(created.getTableName());
    }

    @Test
    void update() {
        HiveStorage created = hiveStorageService.create(hiveStorage);
        Long hiveStorageId = created.getId();

        HiveStorage updateRequest = created.toBuilder().tableName("Updated Storage tableName").build();

        HiveStorage updated = requestSpecWithBody(updateRequest)
                .when().patch(url + "/" + hiveStorageId)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", HiveStorage.class);

        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getTableName()).isEqualTo(updateRequest.getTableName());
    }

    @Test
    void delete() {
        HiveStorage created = hiveStorageService.create(hiveStorage);
        Long hiveStorageId = created.getId();

        requestSpec().when().delete(url + "/" + hiveStorageId)
                .then().statusCode(HttpStatus.NO_CONTENT.value());
    }
}