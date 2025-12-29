package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalPhysicalStorage;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;

import lombok.val;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.model.Environment.PRODUCTION;
import static com.ebay.behavior.gds.mdm.common.model.Environment.UNSTAGED;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.CONTAINS;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.PAGE_IMPRESSION_SIGNAL;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.CJS_PLATFORM_ID;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.EJS_PLATFORM_ID;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.physicalStorage;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.stagedSignal;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SignalPhysicalStorageServiceIT {

    @Autowired
    private SignalPhysicalStorageService service;

    @Autowired
    private StagedSignalService signalService;

    @Autowired
    private PlanService planService;

    private long planId;
    private SignalPhysicalStorage storage;
    private VersionedId signalId;

    @BeforeAll
    void setupAll() {
        storage = physicalStorage();
        storage = service.create(storage);

        var plan = TestModelUtils.plan();
        planId = planService.create(plan).getId();

        var signal = stagedSignal(planId).toBuilder()
                .id(getRandomLong())
                .version(1)
                .platformId(CJS_PLATFORM_ID)
                .environment(UNSTAGED)
                .type(PAGE_IMPRESSION_SIGNAL)
                .build();
        signalId = signalService.create(signal).getSignalId();
    }

    @Test
    void create() {
        assertThat(storage.getId()).isNotNull();
    }

    @Test
    void delete() {
        var storage1 = physicalStorage().toBuilder().build();
        storage1 = service.create(storage1);
        var storageId = storage1.getId();
        assertThat(storageId).isNotNull();
        assertThat(service.getById(storageId)).isNotNull();

        service.delete(storageId);

        assertThatThrownBy(() -> service.getById(storageId))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void create_shouldThrow_whenIdIsPresent() {
        storage = physicalStorage().toBuilder().id(123L).build();

        assertThatThrownBy(() -> service.create(storage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be null");
    }

    @Test
    void getById() {
        var result = service.getById(storage.getId());

        assertThat(result.getId()).isEqualTo(storage.getId());
    }

    @Test
    void getByIdWithAssociations_notImplemented_error() {
        assertThatThrownBy(() -> service.getByIdWithAssociations(storage.getId()))
                .isInstanceOf(NotImplementedException.class);
    }

    @Test
    void getById_nonExistentId_error() {
        assertThatThrownBy(() -> service.getById(9999L))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void getByKafkaTopicAndEnvironment() {
        val topic = storage.getKafkaTopic();
        var result = service.getByKafkaTopicAndEnvironment(topic, PRODUCTION);

        assertThat(result.getKafkaTopic()).isEqualTo(topic);
        assertThat(result.getEnvironment()).isEqualTo(PRODUCTION);
    }

    @Test
    void getByKafkaTopicAndEnvironment_nonFound_error() {
        assertThatThrownBy(() -> service.getByKafkaTopicAndEnvironment("noSuchTopic", PRODUCTION))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void getBySignalId() {
        var result = service.getBySignalId(signalId);

        assertThat(result.getEnvironment()).isEqualTo(UNSTAGED); // matches the data.sql record
    }

    @Test
    void getBySignalId_noStorage_error() {
        var signal1 = stagedSignal(planId).toBuilder()
                .id(getRandomLong())
                .version(1)
                .platformId(EJS_PLATFORM_ID)
                .environment(PRODUCTION)
                .type(PAGE_IMPRESSION_SIGNAL)
                .build();
        var signalId1 = signalService.create(signal1).getSignalId();

        assertThatThrownBy(() -> service.getBySignalId(signalId1))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void getAll_nonImplemented_error() {
        var search = new Search("by", "term", CONTAINS, PageRequest.of(0, 1));
        assertThatThrownBy(() -> service.getAll(search))
                .isInstanceOf(NotImplementedException.class);
    }

    @Test
    void getAll() {
        var storages = service.getAll();

        assertThat(storages).isNotEmpty();
    }

    @Test
    void getRepository() {
        assertThat(service.getRepository()).isNotNull();
        assertThat(service.getModelType()).isEqualTo(SignalPhysicalStorage.class);
    }
}
