package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.commonSvc.util.DbUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.stagedEvent;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class StagedEventRepositoryIT {

    private final Pageable pageable = DbUtils.getAuditablePageable(0, 10);

    @Autowired
    private StagedEventRepository repository;

    private String term;
    private StagedEvent event;

    @BeforeEach
    void setUp() {
        term = getRandomSmallString();
        event = stagedEvent();
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
        var event1 = stagedEvent().toBuilder().name(term).build();
        var event2 = stagedEvent().toBuilder().name(term).build();
        repository.save(event1);
        repository.save(event2);

        var result = repository.findByName(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo(term);
        assertThat(result.getContent().get(1).getName()).isEqualTo(term);
    }

    @Test
    void findByNameContaining() {
        var event1 = stagedEvent().toBuilder().name("prefix_" + term).build();
        var event2 = stagedEvent().toBuilder().name(term + "_suffix").build();
        repository.save(event1);
        repository.save(event2);

        var result = repository.findByNameContaining(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).contains(term);
        assertThat(result.getContent().get(1).getName()).contains(term);
    }

    @Test
    void findByNameStartingWith() {
        var event1 = stagedEvent().toBuilder().name(term + "event1").build();
        var event2 = stagedEvent().toBuilder().name(term + "event2").build();
        repository.save(event1);
        repository.save(event2);

        var result = repository.findByNameStartingWith(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).startsWith(term);
        assertThat(result.getContent().get(1).getName()).startsWith(term);
    }

    @Test
    void findByType() {
        var event = stagedEvent().toBuilder().type(term).build();
        repository.save(event);

        var result = repository.findByType(term, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo(term);
    }

    @Test
    void findByTypeStartingWith() {
        var event1 = stagedEvent().toBuilder().type(term + "Type1").build();
        var event2 = stagedEvent().toBuilder().type(term + "Type2").build();
        repository.save(event1);
        repository.save(event2);

        var result = repository.findByTypeStartingWith(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getType()).startsWith(term);
        assertThat(result.getContent().get(1).getType()).startsWith(term);
    }

    @Test
    void findByTypeContaining() {
        var event1 = stagedEvent().toBuilder().type("Type1" + term).build();
        var event2 = stagedEvent().toBuilder().type(term + "Type2").build();
        repository.save(event1);
        repository.save(event2);

        var result = repository.findByTypeContaining(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getType()).contains(term);
        assertThat(result.getContent().get(1).getType()).contains(term);
    }

    @Test
    void findByDescription() {
        var event = stagedEvent().toBuilder().description(term).build();
        repository.save(event);

        var result = repository.findByDescription(term, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDescription()).isEqualTo(term);
    }

    @Test
    void findByDescriptionStartingWith() {
        var event1 = stagedEvent().toBuilder().description(term + "Type1").build();
        var event2 = stagedEvent().toBuilder().description(term + "Type2").build();
        repository.save(event1);
        repository.save(event2);

        var result = repository.findByDescriptionStartingWith(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getDescription()).startsWith(term);
        assertThat(result.getContent().get(1).getDescription()).startsWith(term);
    }

    @Test
    void findByDescriptionContaining() {
        var event1 = stagedEvent().toBuilder().description("Test " + term + " 1").build();
        var event2 = stagedEvent().toBuilder().description("Another " + term + " 2").build();
        repository.save(event1);
        repository.save(event2);

        var result = repository.findByDescriptionContaining(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getDescription()).contains(term);
        assertThat(result.getContent().get(1).getDescription()).contains(term);
    }

    @Test
    void findByPageId() {
        var pageId = getRandomLong();
        var event1 = stagedEvent().toBuilder().pageIds(Set.of(pageId, 2L)).build();
        var event2 = stagedEvent().toBuilder().pageIds(Set.of(pageId, 3L)).build();
        event1 = repository.save(event1);
        event2 = repository.save(event2);

        var result = repository.findByPageId(pageId, pageable);

        var events = result.getContent();
        assertThat(events).hasSize(2);
        assertThat(events).extracting(ID).containsExactlyInAnyOrder(event1.getId(), event2.getId());
        assertThat(events.get(0).getPageIds().size()).isEqualTo(2);
    }

    @Test
    void findByModuleId() {
        var moduleId = getRandomLong();
        var event1 = stagedEvent().toBuilder().moduleIds(Set.of(moduleId, 2L)).build();
        var event2 = stagedEvent().toBuilder().moduleIds(Set.of(moduleId, 3L)).build();
        event1 = repository.save(event1);
        event2 = repository.save(event2);

        var result = repository.findByModuleId(moduleId, pageable);

        var events = result.getContent();
        assertThat(events).hasSize(2);
        assertThat(events).extracting(ID).containsExactlyInAnyOrder(event1.getId(), event2.getId());
        assertThat(events.get(0).getModuleIds().size()).isEqualTo(2);
    }

    @Test
    void findByClickId() {
        var clickId = getRandomLong();
        var event1 = stagedEvent().toBuilder().clickIds(Set.of(clickId, 2L)).build();
        var event2 = stagedEvent().toBuilder().clickIds(Set.of(clickId, 3L)).build();
        event1 = repository.save(event1);
        event2 = repository.save(event2);

        var result = repository.findByClickId(clickId, pageable);

        var events = result.getContent();
        assertThat(events).hasSize(2);
        assertThat(events).extracting(ID).containsExactlyInAnyOrder(event1.getId(), event2.getId());
        assertThat(events.get(0).getClickIds().size()).isEqualTo(2);
    }

    @Test
    void findAllByIdIn() {
        var event1 = stagedEvent();
        var id = repository.save(event1).getId();

        var result = repository.findAllByIdIn(Set.of(id));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(id);
    }
}