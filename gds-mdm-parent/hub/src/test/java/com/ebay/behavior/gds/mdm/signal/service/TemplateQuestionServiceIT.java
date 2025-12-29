package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.signal.common.model.TemplateQuestion;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.TemplateQuestionEventMappingRepository;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.CONTAINS;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.eventTemplate;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.templateQuestion;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class TemplateQuestionServiceIT {

    @Autowired
    private TemplateQuestionService service;

    @Autowired
    private EventTemplateService eventService;

    @Autowired
    private TemplateQuestionEventMappingRepository mappingRepository;

    private long eventId;
    private long questionId;
    private TemplateQuestion question;
    private final Pageable pageable = PageRequest.of(0, 10);

    @BeforeAll
    void setUpAll() {
        var event = eventTemplate();
        eventId = eventService.create(event).getId();
    }

    @BeforeEach
    void setUp() {
        question = templateQuestion();
        question = service.create(question, Set.of(eventId));
        question = service.getById(question.getId());
        questionId = question.getId();
    }

    @Test
    void createAll() {
        var event1 = eventTemplate();
        var eventId1 = eventService.create(event1).getId();
        var question1 = templateQuestion();
        var question2 = templateQuestion();

        var questions = service.createAll(Set.of(question1, question2), Set.of(eventId1));

        var mappings = mappingRepository.findByEventTemplateId(eventId1);
        assertThat(mappings).hasSize(2);
        assertThat(questions).extracting(TemplateQuestion::getId).containsExactlyInAnyOrder(question1.getId(), question2.getId());
    }

    @Test
    void createAll_errorByDesign() {
        assertThatThrownBy(() -> service.createAll(Set.of(templateQuestion())))
                .isInstanceOf(NotImplementedException.class)
                .hasMessageContaining("Not implemented by design");
    }

    @Test
    void create_errorByDesign() {
        assertThatThrownBy(() -> service.create(templateQuestion()))
                .isInstanceOf(NotImplementedException.class)
                .hasMessageContaining("Not implemented by design");
    }

    @Test
    void create() {
        var mappings = mappingRepository.findByEventTemplateId(eventId);
        assertThat(mappings).isNotEmpty();
        assertThat(question.getId()).isNotNull();
        assertThat(service.findById(questionId)).isPresent();
    }

    @Test
    void update() {
        question.setQuestion("updated");
        var updated = service.update(question);

        assertThat(updated.getQuestion()).isEqualTo("updated");
    }

    @Test
    void getAll_errorByDesign() {
        assertThatThrownBy(() -> service.getAll(new Search("by", "term", CONTAINS, pageable)))
                .isInstanceOf(NotImplementedException.class);
    }

    @Test
    void delete() {
        var maybeMapping = mappingRepository.findByQuestionIdAndEventTemplateId(questionId, eventId);
        assertThat(maybeMapping).isPresent();

        service.delete(questionId);

        assertThatThrownBy(() -> service.getById(questionId))
                .isInstanceOf(DataNotFoundException.class);

        maybeMapping = mappingRepository.findByQuestionIdAndEventTemplateId(questionId, eventId);
        assertThat(maybeMapping).isEmpty();
    }

    @Test
    void deleteEventMapping() {
        var maybeMapping = mappingRepository.findByQuestionIdAndEventTemplateId(questionId, eventId);
        assertThat(maybeMapping).isPresent();

        service.deleteEventMapping(questionId, eventId);

        maybeMapping = mappingRepository.findByQuestionIdAndEventTemplateId(questionId, eventId);
        assertThat(maybeMapping).isEmpty();
    }

    @Test
    void getByIdWithAssociations_initializesAssociations() {
        var persisted = service.getByIdWithAssociations(questionId);

        assertThat(persisted.getEvents().iterator().next().getId()).isEqualTo(eventId);
    }

    @Test
    void getById_nonExistentId_error() {
        assertThatThrownBy(() -> service.getById(9999L))
                .isInstanceOf(DataNotFoundException.class);
    }
}