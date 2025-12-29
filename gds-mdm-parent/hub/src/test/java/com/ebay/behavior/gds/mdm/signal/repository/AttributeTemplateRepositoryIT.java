package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.commonSvc.util.DbUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.AttributeTemplate;

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
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.attributeTemplate;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.eventTemplate;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AttributeTemplateRepositoryIT {

    private final Pageable pageable = DbUtils.getAuditablePageable(0, 10);

    @Autowired
    private AttributeTemplateRepository repository;

    @Autowired
    private EventTemplateRepository eventRepository;

    private String term;
    private long eventId;
    private AttributeTemplate attribute;

    @BeforeAll
    void setUpAll() {
        var event = eventTemplate();
        event = eventRepository.save(event);
        eventId = event.getId();
    }

    @BeforeEach
    void setUp() {
        term = getRandomSmallString();
        attribute = attributeTemplate(eventId);
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
        attribute.setEventTemplateId(null);

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
    void findByEventTemplateId() {
        var attribute1 = attributeTemplate(eventId);
        var attribute2 = attributeTemplate(eventId);
        var saved1 = repository.save(attribute1);
        var saved2 = repository.save(attribute2);

        var attributes = repository.findByEventTemplateId(eventId);

        assertThat(attributes).isNotEmpty();
        assertThat(attributes).extracting(ID).containsAll(List.of(saved1.getId(), saved2.getId()));
    }

    @Test
    void findByTag() {
        var attributeTemplate1 = attributeTemplate(eventId).setTag(term);
        var attributeTemplate2 = attributeTemplate(eventId).setTag(term);
        repository.save(attributeTemplate1);
        repository.save(attributeTemplate2);

        var result = repository.findByTag(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTag()).isEqualTo(term);
        assertThat(result.getContent().get(1).getTag()).isEqualTo(term);
    }

    @Test
    void findByTagStartingWith() {
        var attributeTemplate1 = attributeTemplate(eventId).setTag(term + "_suffix1");
        var attributeTemplate2 = attributeTemplate(eventId).setTag(term + "_suffix2");
        repository.save(attributeTemplate1);
        repository.save(attributeTemplate2);

        var result = repository.findByTagStartingWith(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTag()).startsWith(term);
        assertThat(result.getContent().get(1).getTag()).startsWith(term);
    }

    @Test
    void findByTagContaining() {
        var attributeTemplate1 = attributeTemplate(eventId).setTag("prefix_" + term);
        var attributeTemplate2 = attributeTemplate(eventId).setTag(term + "_suffix");
        repository.save(attributeTemplate1);
        repository.save(attributeTemplate2);

        var result = repository.findByTagContaining(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTag()).contains(term);
        assertThat(result.getContent().get(1).getTag()).contains(term);
    }

    @Test
    void findBySchemaPathStartingWith() {
        var attribute1 = attributeTemplate(eventId).setSchemaPath(term + "/schemaPath1");
        var attribute2 = attributeTemplate(eventId).setSchemaPath(term + "/schemaPath2");
        repository.save(attribute1);
        repository.save(attribute2);

        var result = repository.findBySchemaPathStartingWith(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getSchemaPath()).startsWith(term);
        assertThat(result.getContent().get(1).getSchemaPath()).startsWith(term);
    }

    @Test
    void findBySchemaPathContaining() {
        var attribute1 = attributeTemplate(eventId).setSchemaPath("test/" + term + "/path1");
        var attribute2 = attributeTemplate(eventId).setSchemaPath("test/path2/" + term);
        repository.save(attribute1);
        repository.save(attribute2);

        var result = repository.findBySchemaPathContaining(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getSchemaPath()).contains(term);
        assertThat(result.getContent().get(1).getSchemaPath()).contains(term);
    }

    @Test
    void findByDescription() {
        var attributeTemplate1 = attributeTemplate(eventId).setDescription(term);
        var attributeTemplate2 = attributeTemplate(eventId).setDescription(term);
        repository.save(attributeTemplate1);
        repository.save(attributeTemplate2);

        var result = repository.findByDescription(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getDescription()).isEqualTo(term);
        assertThat(result.getContent().get(1).getDescription()).isEqualTo(term);
    }

    @Test
    void findByDescriptionStartingWith() {
        var attributeTemplate1 = attributeTemplate(eventId).setDescription(term + "_suffix1");
        var attributeTemplate2 = attributeTemplate(eventId).setDescription(term + "_suffix2");
        repository.save(attributeTemplate1);
        repository.save(attributeTemplate2);

        var result = repository.findByDescriptionStartingWith(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getDescription()).startsWith(term);
        assertThat(result.getContent().get(1).getDescription()).startsWith(term);
    }

    @Test
    void findByDescriptionContaining() {
        var attributeTemplate1 = attributeTemplate(eventId).setDescription("prefix_" + term);
        var attributeTemplate2 = attributeTemplate(eventId).setDescription(term + "_suffix");
        repository.save(attributeTemplate1);
        repository.save(attributeTemplate2);

        var result = repository.findByDescriptionContaining(term, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getDescription()).contains(term);
        assertThat(result.getContent().get(1).getDescription()).contains(term);
    }
}