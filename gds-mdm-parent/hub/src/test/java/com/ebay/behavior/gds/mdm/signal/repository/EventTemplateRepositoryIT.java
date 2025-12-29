package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.commonSvc.util.DbUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.EventTemplate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.common.model.EventSource.SOJ;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.eventTemplate;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class EventTemplateRepositoryIT {

    private final Pageable pageable = DbUtils.getAuditablePageable(0, 10);

    @Autowired
    private EventTemplateRepository repository;

    private String term;
    private EventTemplate event;

    @BeforeEach
    void setUp() {
        term = getRandomSmallString();
        event = eventTemplate();
    }

    @Test
    void create() {
        var saved = repository.save(event);

        var persisted = repository.findById(saved.getId()).get();

        assertThat(persisted.getId()).isEqualTo(saved.getId());
        assertThat(persisted.getRevision()).isEqualTo(0);
    }

    @Test
    void update() {
        var created = repository.save(event);
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
        var saved = repository.save(event);

        repository.deleteById(saved.getId());

        var maybeDeleted = repository.findById(saved.getId());
        assertThat(maybeDeleted).isEmpty();
    }

    @Test
    void findByName() {
        var event1 = eventTemplate().setName(term);
        var event2 = eventTemplate().setName(term);
        repository.save(event1);
        repository.save(event2);

        var result = repository.findByName(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo(term);
        assertThat(result.getContent().get(1).getName()).isEqualTo(term);
    }

    @Test
    void findByNameContaining() {
        var event1 = eventTemplate().setName("prefix_" + term);
        var event2 = eventTemplate().setName(term + "_suffix");
        repository.save(event1);
        repository.save(event2);

        var result = repository.findByNameContaining(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).contains(term);
        assertThat(result.getContent().get(1).getName()).contains(term);
    }

    @Test
    void findByNameStartingWith() {
        var event1 = eventTemplate().setName(term + "event1");
        var event2 = eventTemplate().setName(term + "event2");
        repository.save(event1);
        repository.save(event2);

        var result = repository.findByNameStartingWith(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).startsWith(term);
        assertThat(result.getContent().get(1).getName()).startsWith(term);
    }

    @Test
    void findByType() {
        var eventTemplate = eventTemplate().setType(term);
        repository.save(eventTemplate);

        var result = repository.findByType(term, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo(term);
    }

    @Test
    void findByTypeStartingWith() {
        var event1 = eventTemplate().setType(term + "Type1");
        var event2 = eventTemplate().setType(term + "Type2");
        repository.save(event1);
        repository.save(event2);

        var result = repository.findByTypeStartingWith(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getType()).startsWith(term);
        assertThat(result.getContent().get(1).getType()).startsWith(term);
    }

    @Test
    void findByTypeContaining() {
        var event1 = eventTemplate().setType("Type1" + term);
        var event2 = eventTemplate().setType(term + "Type2");
        repository.save(event1);
        repository.save(event2);

        var result = repository.findByTypeContaining(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getType()).contains(term);
        assertThat(result.getContent().get(1).getType()).contains(term);
    }

    @Test
    void findByDescription() {
        var event = eventTemplate().setDescription(term);
        repository.save(event);

        var result = repository.findByDescription(term, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDescription()).isEqualTo(term);
    }

    @Test
    void findByDescriptionStartingWith() {
        var event1 = eventTemplate().setDescription(term + "Type1");
        var event2 = eventTemplate().setDescription(term + "Type2");
        repository.save(event1);
        repository.save(event2);

        var result = repository.findByDescriptionStartingWith(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getDescription()).startsWith(term);
        assertThat(result.getContent().get(1).getDescription()).startsWith(term);
    }

    @Test
    void findByDescriptionContaining() {
        var event1 = eventTemplate().setDescription("Test " + term + " 1");
        var event2 = eventTemplate().setDescription("Another " + term + " 2");
        repository.save(event1);
        repository.save(event2);

        var result = repository.findByDescriptionContaining(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getDescription()).contains(term);
        assertThat(result.getContent().get(1).getDescription()).contains(term);
    }

    @Test
    void findBySource() {
        repository.save(event);

        var result = repository.findBySource(SOJ);

        assertThat(result.size()).isGreaterThanOrEqualTo(1);
        assertThat(result.iterator().next().getSource()).isEqualTo(SOJ);
    }
}