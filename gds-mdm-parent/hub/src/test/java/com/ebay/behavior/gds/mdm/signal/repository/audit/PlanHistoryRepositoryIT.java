package com.ebay.behavior.gds.mdm.signal.repository.audit;

import com.ebay.behavior.gds.mdm.common.model.Model;
import com.ebay.behavior.gds.mdm.common.util.RandomUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.audit.PlanHistory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.CREATED;
import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.UPDATED;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.planHistory;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PlanHistoryRepositoryIT {

    @Autowired
    private PlanHistoryRepository repository;

    private PlanHistory model;

    @BeforeEach
    void setUp() {
        model = planHistory();
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
    void deleteById() {
        repository.save(model);

        repository.deleteById(model.getId());

        var maybePersisted = repository.findById(model.getId());
        assertThat(maybePersisted).isEmpty();
    }

    @Test
    void findByOriginalId() {
        var originalId = RandomUtils.getRandomLong(1000);
        var model1 = planHistory().withOriginalId(originalId);
        var model2 = planHistory().withOriginalId(originalId).withOriginalRevision(1).withType(UPDATED);
        repository.saveAllAndFlush(List.of(model1, model2));

        var found = repository.findByOriginalId(originalId);
        var ids = found.stream().map(Model::getId).toList();

        assertThat(ids).contains(model1.getId(), model2.getId());
        assertThat(found.get(0).getOriginalId()).isEqualTo(model1.getOriginalId());
        assertThat(found.get(0).getOriginalRevision()).isEqualTo(0);
        assertThat(found.get(0).getChangeType()).isEqualTo(CREATED);
        assertThat(found.get(1).getOriginalId()).isEqualTo(model2.getOriginalId());
        assertThat(found.get(1).getOriginalRevision()).isEqualTo(1);
        assertThat(found.get(1).getChangeType()).isEqualTo(UPDATED);
    }
}
