package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.service.PlanService;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedSignal;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UnstagedSignalRepositoryIT {

    @Autowired
    private UnstagedSignalRepository repository;

    @Autowired
    private PlanService planService;

    private UnstagedSignal signal;
    private long planId;

    @BeforeEach
    void setUp() {
        var plan = TestModelUtils.plan();
        planId = planService.create(plan).getId();
        signal = unstagedSignal(planId);
    }

    @Test
    void create() {
        var saved = repository.save(signal);

        var persisted = repository.findById(saved.getSignalId()).get();

        assertThat(persisted.getId()).isEqualTo(saved.getId());
        assertThat(persisted.getRevision()).isEqualTo(0);
    }

    @Test
    void create_withId() {
        var id = getRandomLong();
        var signal1 = unstagedSignal(planId).toBuilder().id(id).version(1).build();
        var signal2 = unstagedSignal(planId).toBuilder().id(id).version(2).build();

        var saved1 = repository.save(signal1);
        var saved2 = repository.save(signal2);

        var persisted1 = repository.findById(saved1.getSignalId()).get();
        var persisted2 = repository.findById(saved2.getSignalId()).get();

        assertThat(persisted1.getId()).isEqualTo(saved1.getId());
        assertThat(persisted2.getId()).isEqualTo(saved2.getId());
    }

    @Test
    void update() {
        var created = repository.save(signal);
        var createDate = created.getCreateDate();
        var updateDate = created.getUpdateDate();

        created.setName("updated");
        repository.save(created);

        var updated = repository.findById(created.getSignalId()).get();
        assertThat(updated.getName()).isEqualTo("updated");
        assertThat(updated.getRevision()).isEqualTo(1);
        assertThat(updated.getCreateDate()).isEqualTo(createDate);
        assertThat(updated.getUpdateDate()).isAfterOrEqualTo(updateDate);
    }

    @Test
    void delete() {
        var saved = repository.save(signal);

        repository.deleteById(saved.getSignalId());

        var maybeDeleted = repository.findById(saved.getSignalId());
        assertThat(maybeDeleted).isEmpty();
    }

    @Test
    void findLatestVersion() {
        var saved = repository.save(signal);

        var version = repository.findLatestVersion(saved.getId()).get();

        assertThat(version).isEqualTo(saved.getVersion());
    }

    @Test
    void findByIdAndLatestVersion() {
        var saved = repository.save(signal);

        var signal = repository.findByIdAndLatestVersion(saved.getId()).get();

        assertThat(signal.getSignalId()).isEqualTo(saved.getSignalId());
    }

    @Test
    void findByIdAndVersion() {
        var saved = repository.save(signal);

        var signal = repository.findByIdAndVersion(saved.getId(), saved.getVersion()).get();

        assertThat(signal.getSignalId()).isEqualTo(saved.getSignalId());
    }
}
