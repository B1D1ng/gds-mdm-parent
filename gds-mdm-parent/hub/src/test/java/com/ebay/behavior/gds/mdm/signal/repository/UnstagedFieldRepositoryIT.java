package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.commonSvc.util.DbUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.service.PlanService;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedField;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedSignal;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UnstagedFieldRepositoryIT {

    private final Pageable pageable = DbUtils.getAuditablePageable(0, 10);

    @Autowired
    private UnstagedSignalRepository signalRepository;

    @Autowired
    private UnstagedFieldRepository repository;

    @Autowired
    private PlanService planService;

    private VersionedId signalId;
    private String term;
    private UnstagedField field;

    @BeforeEach
    void setUp() {
        var plan = TestModelUtils.plan();
        var planId = planService.create(plan).getId();

        term = getRandomSmallString();
        var signal = unstagedSignal(planId);
        signalId = signalRepository.save(signal).getSignalId();
        field = unstagedField(signalId);
    }

    @Test
    void create() {
        var saved = repository.save(field);

        var persisted = repository.findById(saved.getId()).get();

        assertThat(persisted.getId()).isEqualTo(saved.getId());
        assertThat(persisted.getRevision()).isEqualTo(0);
        assertThat(persisted.getCreateDate()).isNotNull();
        assertThat(persisted.getUpdateDate()).isNotNull();
    }

    @Test
    void update() {
        var created = repository.save(field);
        var createDate = created.getCreateDate();
        var updateDate = created.getUpdateDate();

        created.setName("updated");
        repository.save(created);

        var updated = repository.findById(created.getId()).get();
        assertThat(updated.getName()).isEqualTo("updated");
        assertThat(updated.getRevision()).isEqualTo(1);
        assertThat(updated.getCreateDate()).isEqualTo(createDate);
        assertThat(updated.getUpdateDate()).isAfterOrEqualTo(updateDate);
    }

    @Test
    void delete() {
        var saved = repository.save(field);

        repository.deleteById(saved.getId());

        var maybeDeleted = repository.findById(saved.getId());
        assertThat(maybeDeleted).isEmpty();
    }

    @Test
    void findBySignalIdAndSignalVersion() {
        var saved = repository.save(field);

        var fields = repository.findBySignalIdAndSignalVersion(signalId.getId(), signalId.getVersion());

        assertThat(fields.size()).isGreaterThanOrEqualTo(1);
        assertThat(fields).extracting(ID).contains(saved.getId());
    }

    @Test
    void findByName() {
        var field1 = unstagedField(signalId).toBuilder().name(term).build();
        var field2 = unstagedField(signalId).toBuilder().name(term).build();
        repository.save(field1);
        repository.save(field2);

        var result = repository.findByName(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo(term);
        assertThat(result.getContent().get(1).getName()).isEqualTo(term);
    }

    @Test
    void findByNameContaining() {
        var field1 = unstagedField(signalId).toBuilder().name("prefix_" + term).build();
        var field2 = unstagedField(signalId).toBuilder().name(term + "_suffix").build();
        repository.save(field1);
        repository.save(field2);

        var result = repository.findByNameContaining(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).contains(term);
        assertThat(result.getContent().get(1).getName()).contains(term);
    }

    @Test
    void findByNameStartingWith() {
        var field1 = unstagedField(signalId).toBuilder().name(term + "Signal1").build();
        var field2 = unstagedField(signalId).toBuilder().name(term + "Signal2").build();
        repository.save(field1);
        repository.save(field2);

        var result = repository.findByNameStartingWith(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).startsWith(term);
        assertThat(result.getContent().get(1).getName()).startsWith(term);
    }

    @Test
    void findByTag() {
        var field1 = unstagedField(signalId).toBuilder().tag(term).build();
        var field2 = unstagedField(signalId).toBuilder().tag(term).build();
        repository.save(field1);
        repository.save(field2);

        var result = repository.findByTag(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTag()).isEqualTo(term);
        assertThat(result.getContent().get(1).getTag()).isEqualTo(term);
    }

    @Test
    void findByTagStartingWith() {
        var field1 = unstagedField(signalId).toBuilder().tag(term + "Signal1").build();
        var field2 = unstagedField(signalId).toBuilder().tag(term + "Signal2").build();
        repository.save(field1);
        repository.save(field2);

        var result = repository.findByTagStartingWith(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTag()).startsWith(term);
        assertThat(result.getContent().get(1).getTag()).startsWith(term);
    }

    @Test
    void findByTagContaining() {
        var field1 = unstagedField(signalId).toBuilder().tag("prefix_" + term).build();
        var field2 = unstagedField(signalId).toBuilder().tag(term + "_suffix").build();
        repository.save(field1);
        repository.save(field2);

        var result = repository.findByTagContaining(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTag()).contains(term);
        assertThat(result.getContent().get(1).getTag()).contains(term);
    }

    @Test
    void findByDescription() {
        var field1 = unstagedField(signalId).toBuilder().description(term).build();
        var field2 = unstagedField(signalId).toBuilder().description(term).build();
        repository.save(field1);
        repository.save(field2);

        var result = repository.findByDescription(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getDescription()).isEqualTo(term);
        assertThat(result.getContent().get(1).getDescription()).isEqualTo(term);
    }

    @Test
    void findByDescriptionStartingWith() {
        var field1 = unstagedField(signalId).toBuilder().description(term + "_suffix1").build();
        var field2 = unstagedField(signalId).toBuilder().description(term + "_suffix2").build();
        repository.save(field1);
        repository.save(field2);

        var result = repository.findByDescriptionStartingWith(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getDescription()).startsWith(term);
        assertThat(result.getContent().get(1).getDescription()).startsWith(term);
    }

    @Test
    void findByDescriptionContaining() {
        var field1 = unstagedField(signalId).toBuilder().description("prefix_" + term).build();
        var field2 = unstagedField(signalId).toBuilder().description(term + "_suffix").build();
        repository.save(field1);
        repository.save(field2);

        var result = repository.findByDescriptionContaining(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getDescription()).contains(term);
        assertThat(result.getContent().get(1).getDescription()).contains(term);
    }
}