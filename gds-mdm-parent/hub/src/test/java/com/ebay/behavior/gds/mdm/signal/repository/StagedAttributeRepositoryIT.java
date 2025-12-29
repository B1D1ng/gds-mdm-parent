package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.commonSvc.util.DbUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedAttribute;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.stagedAttribute;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.stagedEvent;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class StagedAttributeRepositoryIT {

    private final Pageable pageable = DbUtils.getAuditablePageable(0, 10);

    @Autowired
    private StagedAttributeRepository repository;

    @Autowired
    private StagedEventRepository eventRepository;

    private String term;
    private long eventId;
    private StagedAttribute attribute;

    @BeforeAll
    void setUpAll() {
        var event = stagedEvent();
        event = eventRepository.save(event);
        eventId = event.getId();
    }

    @BeforeEach
    void setUp() {
        term = getRandomSmallString();
        attribute = stagedAttribute(eventId);
    }

    @Test
    void create() {
        var saved = repository.save(attribute);

        var persisted = repository.findById(saved.getId()).get();

        assertThat(persisted.getId()).isEqualTo(saved.getId());
        assertThat(persisted.getRevision()).isEqualTo(0);
    }

    @Test
    void create_noParent_error() {
        attribute.setEventId(null);

        assertThatThrownBy(() -> repository.save(attribute))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void delete() {
        var saved = repository.save(attribute);

        repository.deleteById(saved.getId());

        var maybeDeleted = repository.findById(saved.getId());
        assertThat(maybeDeleted).isEmpty();
    }

    @Test
    void findByEventId() {
        var attribute1 = stagedAttribute(eventId);
        var attribute2 = stagedAttribute(eventId);
        var saved1 = repository.save(attribute1);
        var saved2 = repository.save(attribute2);

        var attributes = repository.findByEventId(eventId);

        assertThat(attributes).isNotEmpty();
        assertThat(attributes).extracting(ID).containsAll(List.of(saved1.getId(), saved2.getId()));
    }

    @Test
    void findByTag() {
        var attribute1 = stagedAttribute(eventId).toBuilder().tag(term).build();
        var attribute2 = stagedAttribute(eventId).toBuilder().tag(term).build();
        repository.save(attribute1);
        repository.save(attribute2);

        var result = repository.findByTag(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTag()).isEqualTo(term);
        assertThat(result.getContent().get(1).getTag()).isEqualTo(term);
    }

    @Test
    void findByTagStartingWith() {
        var attribute1 = stagedAttribute(eventId).toBuilder().tag(term + "_suffix1").build();
        var attribute2 = stagedAttribute(eventId).toBuilder().tag(term + "_suffix2").build();
        repository.save(attribute1);
        repository.save(attribute2);

        var result = repository.findByTagStartingWith(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTag()).startsWith(term);
        assertThat(result.getContent().get(1).getTag()).startsWith(term);
    }

    @Test
    void findByTagContaining() {
        var attribute1 = stagedAttribute(eventId).toBuilder().tag("prefix_" + term).build();
        var attribute2 = stagedAttribute(eventId).toBuilder().tag(term + "_suffix").build();
        repository.save(attribute1);
        repository.save(attribute2);

        var result = repository.findByTagContaining(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTag()).contains(term);
        assertThat(result.getContent().get(1).getTag()).contains(term);
    }

    @Test
    void findBySchemaPathStartingWith() {
        var attribute1 = stagedAttribute(eventId).toBuilder().schemaPath(term + "/schemaPath1").build();
        var attribute2 = stagedAttribute(eventId).toBuilder().schemaPath(term + "/schemaPath2").build();
        repository.save(attribute1);
        repository.save(attribute2);

        var result = repository.findBySchemaPathStartingWith(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getSchemaPath()).startsWith(term);
        assertThat(result.getContent().get(1).getSchemaPath()).startsWith(term);
    }

    @Test
    void findBySchemaPathContaining() {
        var attribute1 = stagedAttribute(eventId).toBuilder().schemaPath("test/" + term + "/path1").build();
        var attribute2 = stagedAttribute(eventId).toBuilder().schemaPath("test/path2/" + term).build();
        repository.save(attribute1);
        repository.save(attribute2);

        var result = repository.findBySchemaPathContaining(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getSchemaPath()).contains(term);
        assertThat(result.getContent().get(1).getSchemaPath()).contains(term);
    }

    @Test
    void findByDescription() {
        var attribute1 = stagedAttribute(eventId).toBuilder().description(term).build();
        var attribute2 = stagedAttribute(eventId).toBuilder().description(term).build();
        repository.save(attribute1);
        repository.save(attribute2);

        var result = repository.findByDescription(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getDescription()).isEqualTo(term);
        assertThat(result.getContent().get(1).getDescription()).isEqualTo(term);
    }

    @Test
    void findByDescriptionStartingWith() {
        var attribute1 = stagedAttribute(eventId).toBuilder().description(term + "_suffix1").build();
        var attribute2 = stagedAttribute(eventId).toBuilder().description(term + "_suffix2").build();
        repository.save(attribute1);
        repository.save(attribute2);

        var result = repository.findByDescriptionStartingWith(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getDescription()).startsWith(term);
        assertThat(result.getContent().get(1).getDescription()).startsWith(term);
    }

    @Test
    void findByDescriptionContaining() {
        var attribute1 = stagedAttribute(eventId).toBuilder().description("prefix_" + term).build();
        var attribute2 = stagedAttribute(eventId).toBuilder().description(term + "_suffix").build();
        repository.save(attribute1);
        repository.save(attribute2);

        var result = repository.findByDescriptionContaining(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getDescription()).contains(term);
        assertThat(result.getContent().get(1).getDescription()).contains(term);
    }
}