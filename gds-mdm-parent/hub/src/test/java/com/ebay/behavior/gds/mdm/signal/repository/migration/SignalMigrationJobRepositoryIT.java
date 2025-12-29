package com.ebay.behavior.gds.mdm.signal.repository.migration;

import com.ebay.behavior.gds.mdm.signal.model.migration.SignalMigrationJob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.signalMigrationJob;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SignalMigrationJobRepositoryIT {

    @Autowired
    private SignalMigrationJobRepository repository;

    private SignalMigrationJob model;

    @BeforeEach
    void setUp() {
        model = signalMigrationJob();
    }

    @Test
    void save() {
        var saved = repository.save(model);

        var id = repository.getReferenceById(saved.getId()).getId();
        assertThat(id).isEqualTo(saved.getId());
        assertThat(saved.getRevision()).isEqualTo(0);
        assertThat(saved.getCreateDate()).isNotNull();
        assertThat(saved.getUpdateDate()).isNotNull();
    }

    @Test
    void findByJobId() {
        var saved = repository.save(model);

        var result = repository.findByJobId(saved.getJobId());
        assertThat(result).isPresent();
        assertThat(result.get().getJobId()).isEqualTo(saved.getJobId());
    }
}
