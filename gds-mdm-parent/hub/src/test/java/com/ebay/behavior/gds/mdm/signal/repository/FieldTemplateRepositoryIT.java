package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.commonSvc.util.DbUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.FieldTemplate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.fieldTemplate;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.signalTemplate;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class FieldTemplateRepositoryIT {

    private final Pageable pageable = DbUtils.getAuditablePageable(0, 10);

    @Autowired
    private SignalTemplateRepository signalRepository;

    @Autowired
    private FieldTemplateRepository repository;

    private String term;
    private long signalId;
    private FieldTemplate field;

    @BeforeEach
    void setUp() {
        term = getRandomSmallString();
        var signal = signalTemplate();
        signalId = signalRepository.save(signal).getId();
        field = fieldTemplate(signalId);
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
    void findBySignalTemplateId() {
        var saved = repository.save(field);

        var fields = repository.findBySignalTemplateId(signalId);

        assertThat(fields.size()).isGreaterThanOrEqualTo(1);
        assertThat(fields).extracting(ID).contains(saved.getId());
    }

    @Test
    void findByName() {
        var template1 = fieldTemplate(signalId).setName(term);
        var template2 = fieldTemplate(signalId).setName(term);
        repository.save(template1);
        repository.save(template2);

        var result = repository.findByName(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo(term);
        assertThat(result.getContent().get(1).getName()).isEqualTo(term);
    }

    @Test
    void findByNameContaining() {
        var template1 = fieldTemplate(signalId).setName("prefix_" + term);
        var template2 = fieldTemplate(signalId).setName(term + "_suffix");
        repository.save(template1);
        repository.save(template2);

        var result = repository.findByNameContaining(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).contains(term);
        assertThat(result.getContent().get(1).getName()).contains(term);
    }

    @Test
    void findByNameStartingWith() {
        var template1 = fieldTemplate(signalId).setName(term + "Signal1");
        var template2 = fieldTemplate(signalId).setName(term + "Signal2");
        repository.save(template1);
        repository.save(template2);

        var result = repository.findByNameStartingWith(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).startsWith(term);
        assertThat(result.getContent().get(1).getName()).startsWith(term);
    }

    @Test
    void findByTag() {
        var template1 = fieldTemplate(signalId).setTag(term);
        var template2 = fieldTemplate(signalId).setTag(term);
        repository.save(template1);
        repository.save(template2);

        var result = repository.findByTag(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTag()).isEqualTo(term);
        assertThat(result.getContent().get(1).getTag()).isEqualTo(term);
    }

    @Test
    void findByTagStartingWith() {
        var template1 = fieldTemplate(signalId).setTag(term + "Signal1");
        var template2 = fieldTemplate(signalId).setTag(term + "Signal2");
        repository.save(template1);
        repository.save(template2);

        var result = repository.findByTagStartingWith(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTag()).startsWith(term);
        assertThat(result.getContent().get(1).getTag()).startsWith(term);
    }

    @Test
    void findByTagContaining() {
        var template1 = fieldTemplate(signalId).setTag("prefix_" + term);
        var template2 = fieldTemplate(signalId).setTag(term + "_suffix");
        repository.save(template1);
        repository.save(template2);

        var result = repository.findByTagContaining(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTag()).contains(term);
        assertThat(result.getContent().get(1).getTag()).contains(term);
    }

    @Test
    void findByDescription() {
        var template1 = fieldTemplate(signalId).setDescription(term);
        var template2 = fieldTemplate(signalId).setDescription(term);
        repository.save(template1);
        repository.save(template2);

        var result = repository.findByDescription(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getDescription()).isEqualTo(term);
        assertThat(result.getContent().get(1).getDescription()).isEqualTo(term);
    }

    @Test
    void findByDescriptionStartingWith() {
        var template1 = fieldTemplate(signalId).setDescription(term + "_suffix1");
        var template2 = fieldTemplate(signalId).setDescription(term + "_suffix2");
        repository.save(template1);
        repository.save(template2);

        var result = repository.findByDescriptionStartingWith(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getDescription()).startsWith(term);
        assertThat(result.getContent().get(1).getDescription()).startsWith(term);
    }

    @Test
    void findByDescriptionContaining() {
        var template1 = fieldTemplate(signalId).setDescription("prefix_" + term);
        var template2 = fieldTemplate(signalId).setDescription(term + "_suffix");
        repository.save(template1);
        repository.save(template2);

        var result = repository.findByDescriptionContaining(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getDescription()).contains(term);
        assertThat(result.getContent().get(1).getDescription()).contains(term);
    }
}