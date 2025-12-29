package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalPhysicalStorage;
import com.ebay.behavior.gds.mdm.signal.service.PlanService;
import com.ebay.behavior.gds.mdm.signal.service.SignalPhysicalStorageService;
import com.ebay.behavior.gds.mdm.signal.service.SignalTypeLookupService;
import com.ebay.behavior.gds.mdm.signal.service.StagedSignalService;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import static com.ebay.behavior.gds.mdm.common.model.Environment.STAGING;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.ENVIRONMENT;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.PAGE_IMPRESSION_SIGNAL;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.physicalStorage;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.stagedSignal;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.LOOKUP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class SignalPhysicalStorageResourceIT extends AbstractResourceTest {

    @Autowired
    private SignalPhysicalStorageService service;

    @Autowired
    private StagedSignalService signalService;

    @Autowired
    private SignalTypeLookupService lookupService;

    @Autowired
    private PlanService planService;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + LOOKUP + "/physical-storage/";
    }

    @Test
    void create() {
        var storage = physicalStorage().toBuilder().build();

        var created = requestSpecWithBody(storage)
                .when().post(url)
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getObject(".", SignalPhysicalStorage.class);

        assertThat(created.getId()).isNotNull();
    }

    @Test
    void update() {
        var storage = physicalStorage().toBuilder().build();
        storage = service.create(storage);
        var storageId = storage.getId();
        storage.setDoneFilePath("/new/path/for/testing");

        var updated = requestSpecWithBody(storage)
                .when().put(url + storageId)
                .then().statusCode(HttpStatus.OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", SignalPhysicalStorage.class);

        assertThat(updated.getId()).isEqualTo(storageId);
        assertThat(updated.getDoneFilePath()).isEqualTo("/new/path/for/testing");
    }

    @Test
    void getById() {
        var storage = physicalStorage().toBuilder().build();
        storage = service.create(storage);
        var storageId = storage.getId();

        var persisted = requestSpec()
                .when().get(url + storageId)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", SignalPhysicalStorage.class);

        assertThat(persisted).isNotNull();
        assertThat(persisted.getId()).isEqualTo(storageId);
    }

    @Test
    void getAll() {
        var storage = physicalStorage().toBuilder().environment(STAGING).build();
        service.create(storage);

        var storages = requestSpec()
                .when().get(url)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", SignalPhysicalStorage.class);

        assertThat(storages).isNotEmpty();
    }

    @Test
    void search_byTopicAndEnvironment() {
        var topic = "test-topic";
        var storage = physicalStorage().toBuilder().kafkaTopic(topic).environment(STAGING).build();
        service.create(storage);

        var persisted = requestSpec()
                .queryParam(ENVIRONMENT, STAGING.name())
                .queryParam("kafkaTopic", topic)
                .when().get(url + "search")
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", SignalPhysicalStorage.class);

        assertThat(persisted.getKafkaTopic()).isEqualTo(topic);
        assertThat(persisted.getEnvironment()).isEqualTo(STAGING);
    }

    @Test
    void search_bySignalIdAndVersion() {
        var storage = physicalStorage().toBuilder().environment(STAGING).build();
        service.create(storage);

        var plan = TestModelUtils.plan();
        var planId = planService.create(plan).getId();

        var signal = stagedSignal(planId).toBuilder()
                .id(getRandomLong())
                .version(1)
                .type(PAGE_IMPRESSION_SIGNAL)
                .environment(storage.getEnvironment())
                .build();
        var signalId = signalService.create(signal).getSignalId();

        val lookup = lookupService.getByName(PAGE_IMPRESSION_SIGNAL);

        lookupService.createPhysicalStorageMapping(lookup.getId(), storage.getId());

        var persisted = requestSpec()
                .queryParam("signalId", signalId.getId())
                .queryParam("signalVersion", signalId.getVersion())
                .when().get(url + "search")
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", SignalPhysicalStorage.class);

        assertThat(persisted.getEnvironment()).isEqualTo(signal.getEnvironment());
    }
}
