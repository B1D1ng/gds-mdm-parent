package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.audit.AuditLogParams;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.TemplateQuestion;
import com.ebay.behavior.gds.mdm.signal.repository.audit.SignalTemplateHistoryRepository;

import lombok.val;
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

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.model.audit.AuditMode.BASIC;
import static com.ebay.behavior.gds.mdm.common.model.audit.AuditMode.FULL;
import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.CREATED;
import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.DELETED;
import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.UPDATED;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.CONTAINS;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.EXACT_MATCH;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.STARTS_WITH;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.common.model.search.SignalSearchBy.DESCRIPTION;
import static com.ebay.behavior.gds.mdm.signal.common.model.search.SignalSearchBy.DOMAIN;
import static com.ebay.behavior.gds.mdm.signal.common.model.search.SignalSearchBy.NAME;
import static com.ebay.behavior.gds.mdm.signal.common.model.search.SignalSearchBy.TYPE;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.CJS_PLATFORM_ID;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.attributeTemplate;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.eventTemplate;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.fieldTemplate;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.signalTemplate;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.templateQuestion;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SignalTemplateServiceIT {

    private final Pageable pageable = PageRequest.of(0, 10);

    @Autowired
    private SignalTemplateService service;

    @Autowired
    private EventTemplateService eventService;

    @Autowired
    private FieldTemplateService fieldService;

    @Autowired
    private AttributeTemplateService attributeService;

    @Autowired
    private TemplateQuestionService questionService;

    @Autowired
    private SignalTemplateHistoryRepository historyRepository;

    private long eventId;
    private long signalId;
    private SignalTemplate signal;

    @BeforeEach
    void setUp() {
        signal = signalTemplate();
        signal = service.create(signal);
        signal = service.getById(signal.getId());
        signalId = signal.getId();

        var event = eventTemplate();
        eventId = eventService.create(event).getId();
    }

    @Test
    void create() {
        assertThat(signal.getId()).isNotNull();
        assertThat(service.findById(signalId)).isPresent();

        var histories = historyRepository.findByOriginalId(signalId);

        assertThat(histories.size()).isEqualTo(1);
        var history = histories.get(0);
        assertThat(history.getOriginalId()).isEqualTo(signalId);
        assertThat(history.getOriginalRevision()).isEqualTo(signal.getRevision());
        assertThat(history.getOriginalCreateDate()).isEqualTo(signal.getCreateDate());
        assertThat(history.getOriginalUpdateDate()).isEqualTo(signal.getUpdateDate());
        assertThat(history.getName()).isEqualTo(signal.getName());
        assertThat(history.getChangeType()).isEqualTo(CREATED);
    }

    @Test
    void update() {
        signal.setName("updated");
        var updated = service.update(signal);

        assertThat(updated.getName()).isEqualTo("updated");

        var histories = historyRepository.findByOriginalId(signalId);

        assertThat(histories.size()).isEqualTo(2);
        var history = histories.get(1);
        assertThat(history.getOriginalId()).isEqualTo(updated.getId());
        assertThat(history.getOriginalRevision()).isEqualTo(updated.getRevision());
        assertThat(history.getOriginalCreateDate()).isEqualTo(updated.getCreateDate());
        assertThat(history.getOriginalUpdateDate()).isEqualTo(updated.getUpdateDate());
        assertThat(history.getName()).isEqualTo("updated");
        assertThat(history.getChangeType()).isEqualTo(UPDATED);
    }

    @Test
    void delete() {
        var field = fieldTemplate(signalId);
        var fieldId = fieldService.create(field, Set.of(), Set.of()).getId();

        service.delete(signalId);

        assertThat(service.findById(signalId)).isEmpty();
        assertThat(fieldService.findById(fieldId)).isEmpty();

        var histories = historyRepository.findByOriginalId(signalId);

        assertThat(histories.size()).isEqualTo(2);
        var record = histories.get(1);
        assertThat(record.getOriginalId()).isEqualTo(signalId);
        assertThat(record.getOriginalRevision()).isEqualTo(signal.getRevision());
        assertThat(record.getOriginalCreateDate()).isEqualTo(signal.getCreateDate());
        assertThat(record.getOriginalUpdateDate()).isEqualTo(signal.getUpdateDate());
        assertThat(record.getName()).isEqualTo(signal.getName());
        assertThat(record.getChangeType()).isEqualTo(DELETED);
    }

    @Test
    void deleteRecursive() {
        var signal1 = signalTemplate();
        signal1 = service.create(signal1);
        signal1 = service.getById(signal1.getId());
        var signalId1 = signal1.getId();

        var event1 = eventTemplate();
        var eventId1 = eventService.create(event1).getId();

        var attribute = attributeTemplate(eventId1);
        var attributeId = attributeService.create(attribute).getId();

        var field = fieldTemplate(signalId1);
        var fieldId = fieldService.create(field, Set.of(attributeId), Set.of()).getId(); // associate with fields/events

        signal = service.getByIdWithAssociations(signalId1);
        var events = signal.getEvents();
        assertThat(events).hasSize(1);
        var eventId = events.iterator().next().getId();

        service.deleteRecursive(signalId1);

        assertThat(service.findById(signalId1)).isEmpty();
        assertThat(fieldService.findById(fieldId)).isEmpty();
        assertThat(attributeService.findById(attributeId)).isEmpty();
        assertThat(eventService.findById(eventId)).isEmpty();
    }

    @Test
    void getById_nonExistentId_error() {
        assertThatThrownBy(() -> service.getById(9_999L)).isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void getByIdWithAssociations() {
        var persisted = service.getByIdWithAssociations(signalId);
        assertThat(persisted.getEvents()).isEmpty();
        assertThat(persisted.getFields()).isEmpty();

        var field = fieldTemplate(signalId);
        field = fieldService.create(field, Set.of(), Set.of()); // associate with fields
        service.createEventMapping(signalId, eventId); // associate with events

        persisted = service.getByIdWithAssociations(signalId);
        assertThat(persisted.getEvents()).isNotEmpty();
        assertThat(persisted.getFields()).isNotEmpty();

        // nullify fields to test "fields" not updatable
        persisted.setFields(null);
        service.update(persisted);

        persisted = service.getByIdWithAssociations(signalId);
        assertThat(persisted.getFields()).isNotEmpty();

        fieldService.delete(field.getId()); // dissociate with fields
        service.deleteEventMapping(signalId, eventId); // dissociate with events

        persisted = service.getByIdWithAssociations(signalId);
        assertThat(persisted.getEvents()).isEmpty();
        assertThat(persisted.getFields()).isEmpty();
    }

    @Test
    void getByIdWithAssociationsRecursive() {
        var persisted = service.getByIdWithAssociationsRecursive(signalId);
        assertThat(persisted.getEvents()).isEmpty();
        assertThat(persisted.getFields()).isEmpty();

        var attribute = attributeTemplate(eventId);
        var attributeId = attributeService.create(attribute).getId();

        var field = fieldTemplate(signalId);
        fieldService.create(field, Set.of(attributeId), Set.of()); // associate with fields/events

        persisted = service.getByIdWithAssociationsRecursive(signalId);

        var fields = persisted.getFields();
        assertThat(fields.size()).isEqualTo(1);
        assertThat(fields.iterator().next().getAttributes().size()).isEqualTo(1);

        var events = persisted.getEvents();
        assertThat(events.size()).isEqualTo(1);
        assertThat(events.iterator().next().getAttributes().size()).isEqualTo(1);
    }

    @Test
    void getAll_byNameExactMatch() {
        var term = signal.getName();
        var search = new Search(NAME.name(), term, EXACT_MATCH, pageable);

        var page = service.getAll(search);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getName()).isEqualTo(term);
    }

    @Test
    void getAll_byTypeStartsWith() {
        var term = signal.getType().substring(0, 5);
        var search = new Search(TYPE.name(), term, STARTS_WITH, pageable);

        var page = service.getAll(search);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getType()).contains(term);
    }

    @Test
    void getAll_byDescriptionContains() {
        var term = signal.getDescription().substring(3);
        var search = new Search(DESCRIPTION.name(), term, CONTAINS, pageable);

        var page = service.getAll(search);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getDescription()).contains(term);
    }

    @Test
    void getAll_byDomainExactMatch() {
        var term = signal.getDomain();
        var search = new Search(DOMAIN.name(), term, EXACT_MATCH, pageable);

        var page = service.getAll(search);

        assertThat(page.getContent()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(page.getContent().get(0).getDomain()).isEqualTo(term);
    }

    @Test
    void getQuestions() {
        var signalId1 = service.create(signalTemplate()).getId();
        var event1 = eventTemplate();
        var eventId1 = eventService.create(event1).getId();
        service.createEventMapping(signalId1, eventId1);

        // map questions to events
        var event2 = eventTemplate();
        var eventId2 = eventService.create(event2).getId();
        service.createEventMapping(signalId1, eventId2);

        var questions = service.getQuestions(signalId1);
        assertThat(questions).isEmpty();

        var question1 = templateQuestion();
        var question2 = templateQuestion();
        var questionId1 = questionService.create(question1, Set.of(eventId1)).getId();
        var questionId2 = questionService.create(question2, Set.of(eventId2)).getId();

        questions = service.getQuestions(signalId1);
        assertThat(questions).extracting(TemplateQuestion::getId).containsExactlyInAnyOrder(questionId1, questionId2);

        // remove first question mapping from event
        service.deleteEventMapping(signalId1, eventId1);
        questions = service.getQuestions(signalId1);
        assertThat(questions).extracting(TemplateQuestion::getId).containsExactly(questionId2);

        // remove second question mapping from event
        service.deleteEventMapping(signalId1, eventId2);
        questions = service.getQuestions(signalId1);
        assertThat(questions).isEmpty();
    }

    @Test
    void getFields() {
        var fields = service.getFields(signalId);
        assertThat(fields).isEmpty();

        var field = fieldTemplate(signalId);
        fieldService.create(field, Set.of(), Set.of());
        fields = service.getFields(signalId);

        assertThat(fields).isNotEmpty();
    }

    @Test
    void getEvents() {
        var events = service.getEvents(signalId);
        assertThat(events).isEmpty();

        var event = eventTemplate();
        eventService.create(event);
        service.createEventMapping(signalId, event.getId());
        events = service.getEvents(signalId);

        assertThat(events).isNotEmpty();
    }

    @Test
    void createEventMapping() {
        var persisted = service.getByIdWithAssociations(signalId);
        assertThat(persisted.getEvents()).isEmpty();

        service.createEventMapping(signalId, eventId);
        persisted = service.getByIdWithAssociations(signalId);
        assertThat(persisted.getEvents()).isNotEmpty();
    }

    @Test
    void deleteEventMapping() {
        service.createEventMapping(signalId, eventId);
        var persisted = service.getByIdWithAssociations(signalId);
        assertThat(persisted.getEvents()).isNotEmpty();

        service.deleteEventMapping(signalId, eventId);
        persisted = service.getByIdWithAssociations(signalId);
        assertThat(persisted.getEvents()).isEmpty();
    }

    @Test
    void deleteEventMappings() {
        var event1 = eventTemplate();
        var event2 = eventTemplate();
        var eventId1 = eventService.create(event1).getId();
        var eventId2 = eventService.create(event2).getId();
        service.createEventMapping(signalId, eventId1);
        service.createEventMapping(signalId, eventId2);

        var persisted = service.getByIdWithAssociations(signalId);
        assertThat(persisted.getEvents()).extracting(ID).contains(eventId1, eventId2);
        assertThat(service.getEvents(signalId)).extracting(ID).contains(eventId1, eventId2); // double check

        service.deleteEventMappings(signalId, Set.of(eventId1, eventId2));
        persisted = service.getByIdWithAssociations(signalId);
        assertThat(persisted.getEvents()).extracting(ID).doesNotContain(eventId1, eventId2);
        assertThat(service.getEvents(signalId)).extracting(ID).doesNotContain(eventId1, eventId2); // double check
    }

    @Test
    void getTypes() {
        var template = signalTemplate().setType("PAGE_IMPRESSION").setPlatformId(CJS_PLATFORM_ID);
        service.create(template);
        template = signalTemplate().setType("ONSITE_CLICK").setPlatformId(CJS_PLATFORM_ID);
        service.create(template);
        val types = service.getTypesByPlatformId(CJS_PLATFORM_ID);

        assertThat(types).hasSizeGreaterThanOrEqualTo(2);
        assertThat(types).contains("PAGE_IMPRESSION", "ONSITE_CLICK");
    }

    @Test
    void getAuditLog_basic() {
        var originalUpdateBy = signal.getUpdateBy();
        signal.setName(getRandomSmallString());
        var updated = service.update(signal);

        var auditParams = AuditLogParams.ofNonVersioned(signalId, BASIC);
        var auditRecords = service.getAuditLog(auditParams);

        assertThat(auditRecords).hasSize(2);
        var record1 = auditRecords.get(0);
        assertThat(record1.getUpdateBy()).isEqualTo(originalUpdateBy);
        assertThat(record1.getRight().getOriginalUpdateDate()).isEqualTo(signal.getUpdateDate());
        assertThat(record1.getRevision()).isEqualTo(signal.getRevision());
        assertThat(record1.getChangeType()).isEqualTo(CREATED);
        var record2 = auditRecords.get(1);
        assertThat(record2.getUpdateBy()).isEqualTo(updated.getUpdateBy());
        assertThat(record2.getRight().getOriginalUpdateDate()).isEqualTo(updated.getUpdateDate());
        assertThat(record2.getRevision()).isEqualTo(updated.getRevision());
        assertThat(record2.getChangeType()).isEqualTo(UPDATED);
    }

    @Test
    void getAuditLog_full() {
        var originalName = signal.getName();
        var originalDesc = signal.getDescription();
        signal.setName(getRandomSmallString()).setDescription(getRandomSmallString());
        var updated = service.update(signal);

        var auditParams = AuditLogParams.ofNonVersioned(signalId, FULL);
        var auditRecords = service.getAuditLog(auditParams);

        assertThat(auditRecords).hasSize(2);
        assertThat(auditRecords.get(0).getChangeType()).isEqualTo(CREATED);

        var record = auditRecords.get(1);
        assertThat(record.getUpdateBy()).isEqualTo(updated.getUpdateBy());
        assertThat(record.getRevision()).isEqualTo(updated.getRevision());
        assertThat(record.getChangeType()).isEqualTo(UPDATED);
        var diff = record.getDiff();
        assertThat(diff.getChanges()).hasSize(2);
        assertThat(diff.getPropertyChanges("name").get(0).getLeft()).isEqualTo(originalName);
        assertThat(diff.getPropertyChanges("name").get(0).getRight()).isEqualTo(updated.getName());
        assertThat(diff.getPropertyChanges("description").get(0).getLeft()).isEqualTo(originalDesc);
        assertThat(diff.getPropertyChanges("description").get(0).getRight()).isEqualTo(updated.getDescription());
    }

    @Test
    void getAuditLog_nonExistentId_error() {
        var auditParams = AuditLogParams.ofNonVersioned(getRandomLong(), BASIC);
        assertThatThrownBy(() -> service.getAuditLog(auditParams)).isInstanceOf(DataNotFoundException.class).hasMessageContaining("doesn't found");
    }
}
