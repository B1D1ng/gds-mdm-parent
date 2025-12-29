package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.signal.common.model.SignalPhysicalStorage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.model.Environment.PRODUCTION;
import static com.ebay.behavior.gds.mdm.common.model.Environment.STAGING;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.physicalStorage;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SignalPhysicalStorageRepositoryIT {

    @Autowired
    private SignalPhysicalStorageRepository repository;

    private SignalPhysicalStorage storage;

    @BeforeAll
    void setUp() {
        storage = physicalStorage().toBuilder().environment(STAGING).build();
        storage = repository.save(storage);
    }

    @Test
    void create() {
        var persisted = repository.findById(storage.getId()).get();

        assertThat(persisted.getId()).isEqualTo(storage.getId());
        assertThat(persisted.getRevision()).isEqualTo(0);
    }

    @Test
    void delete() {
        var storage1 = physicalStorage().toBuilder().environment(PRODUCTION).build();
        storage1 = repository.save(storage1);

        repository.deleteById(storage1.getId());

        var maybeDeleted = repository.findById(storage1.getId());
        assertThat(maybeDeleted).isEmpty();
    }

    @Test
    void findByKafkaTopicAndEnvironment() {
        var topic = storage.getKafkaTopic();
        var result = repository.findByKafkaTopicAndEnvironment(topic, STAGING);

        assertThat(result).isPresent();
        assertThat(result.get().getKafkaTopic()).isEqualTo(topic);
        assertThat(result.get().getEnvironment()).isEqualTo(STAGING);
    }
}
