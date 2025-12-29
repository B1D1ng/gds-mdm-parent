package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.audit.AuditLogParams;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.signal.common.model.EventTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.search.EventSearchBy;
import com.ebay.behavior.gds.mdm.signal.repository.audit.EventTemplateHistoryRepository;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;

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

import static com.ebay.behavior.gds.mdm.common.model.audit.AuditMode.BASIC;
import static com.ebay.behavior.gds.mdm.common.model.audit.AuditMode.FULL;
import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.CREATED;
import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.DELETED;
import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.UPDATED;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.CONTAINS;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.EXACT_MATCH;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.STARTS_WITH;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.common.model.search.EventSearchBy.DESCRIPTION;
import static com.ebay.behavior.gds.mdm.signal.common.model.search.EventSearchBy.TYPE;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.attributeTemplate;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.eventTemplate;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.fieldTemplate;
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
class EventTemplateServiceIT {

    private final Pageable pageable = PageRequest.of(0, 10);

    @Autowired
    private EventTemplateService service;

    @Autowired
    private SignalTemplateService signalService;

    @Autowired
    private FieldTemplateService fieldService;

    @Autowired
    private TemplateQuestionService questionService;

    @Autowired
    private AttributeTemplateService attributeService;

    @Autowired
    private EventTemplateHistoryRepository historyRepository;

    private long eventId;
    private EventTemplate event;

    @BeforeEach
    void setUp() {
        event = eventTemplate();
        event = service.create(event);
        event = service.getById(event.getId());
        eventId = event.getId();
    }

    @Test
    void create() {
        assertThat(event.getId()).isNotNull();
        assertThat(event.getCreateBy()).isNotBlank();
        assertThat(event.getCreateDate()).isNotNull();
        assertThat(event.getUpdateBy()).isNotBlank();
        assertThat(event.getUpdateDate()).isNotNull();
        assertThat(service.findById(eventId)).isPresent();

        var histories = historyRepository.findByOriginalId(eventId);

        assertThat(histories.size()).isEqualTo(1);
        var history = histories.get(0);
        assertThat(history.getOriginalId()).isEqualTo(eventId);
        assertThat(history.getOriginalRevision()).isEqualTo(event.getRevision());
        assertThat(history.getOriginalCreateDate()).isEqualTo(event.getCreateDate());
        assertThat(history.getOriginalUpdateDate()).isEqualTo(event.getUpdateDate());
        assertThat(history.getName()).isEqualTo(event.getName());
        assertThat(history.getChangeType()).isEqualTo(CREATED);
        assertThat(history.getCreateBy()).isNotBlank();
        assertThat(history.getCreateDate()).isNotNull();
        assertThat(history.getUpdateBy()).isNotBlank();
        assertThat(history.getUpdateDate()).isNotNull();
    }

    @Test
    void update() {
        event.setName("updated");
        var updated = service.update(event);

        assertThat(updated.getName()).isEqualTo("updated");

        var histories = historyRepository.findByOriginalId(eventId);

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
        service.delete(eventId);

        assertThatThrownBy(() -> service.getById(eventId))
                .isInstanceOf(DataNotFoundException.class);

        var histories = historyRepository.findByOriginalId(eventId);

        assertThat(histories.size()).isEqualTo(2);
        var record = histories.get(1);
        assertThat(record.getOriginalId()).isEqualTo(eventId);
        assertThat(record.getOriginalRevision()).isEqualTo(event.getRevision());
        assertThat(record.getOriginalCreateDate()).isEqualTo(event.getCreateDate());
        assertThat(record.getOriginalUpdateDate()).isEqualTo(event.getUpdateDate());
        assertThat(record.getName()).isEqualTo(event.getName());
        assertThat(record.getChangeType()).isEqualTo(DELETED);
    }

    @Test
    void delete_withAttributes_error() {
        var attribute = attributeTemplate(eventId);
        attributeService.create(attribute);

        assertThatThrownBy(() -> service.delete(eventId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getById_nonExistentId_error() {
        assertThatThrownBy(() -> service.getById(9999L))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void getAttributes() {
        var attributes = service.getAttributes(eventId);
        assertThat(attributes).isEmpty();

        var attribute = attributeTemplate(eventId);
        attributeService.create(attribute);
        attributes = service.getAttributes(eventId);

        assertThat(attributes.size()).isEqualTo(1);
    }

    @Test
    void getFields() {
        var signal = TestModelUtils.signalTemplate();
        var signalId = signalService.create(signal).getId();

        var attribute1 = attributeTemplate(eventId);
        attribute1 = attributeService.create(attribute1);
        var attributeId = attribute1.getId();

        var attribute2 = attributeTemplate(eventId);
        attribute2 = attributeService.create(attribute2);
        var attribute2Id = attribute2.getId();

        var field = fieldTemplate(signalId);
        field = fieldService.create(field, Set.of(attributeId, attribute2Id), null);
        field = fieldService.getByIdWithAssociations(field.getId());

        var event = service.getByIdWithAssociations(eventId);
        var fields = service.getFields(event);
        assertThat(fields.size()).isEqualTo(1);

        var result = fields.stream().toList().get(0);
        assertThat(result).isEqualTo(field);
    }

    @Test
    void getQuestions() {
        var questions = service.getQuestions(eventId);
        assertThat(questions).isEmpty();

        var question = templateQuestion();
        questionService.create(question, Set.of(eventId));
        questions = service.getQuestions(eventId);

        assertThat(questions.size()).isEqualTo(1);
    }

    @Test
    void getAll_byNameExactMatch() {
        var term = event.getName();
        var search = new Search(EventSearchBy.NAME.name(), term, EXACT_MATCH, pageable);

        var page = service.getAll(search);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getName()).isEqualTo(term);
    }

    @Test
    void getAll_byTypeStartsWith() {
        var term = event.getType().substring(0, 5);
        var search = new Search(TYPE.name(), term, STARTS_WITH, pageable);

        var page = service.getAll(search);

        assertThat(page.getContent()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(page.getContent().get(0).getType()).contains(term);
    }

    @Test
    void getAll_byDescriptionContains() {
        var term = event.getDescription().substring(3);
        var search = new Search(DESCRIPTION.name(), term, CONTAINS, pageable);

        var page = service.getAll(search);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getDescription()).contains(term);
    }

    @Test
    void getAuditLog_basic() {
        var originalUpdateBy = event.getUpdateBy();
        event.setName(getRandomSmallString());
        var updated = service.update(event);

        var auditParams = AuditLogParams.ofNonVersioned(eventId, BASIC);
        var auditLog = service.getAuditLog(auditParams);

        assertThat(auditLog).hasSize(2);
        var log1 = auditLog.get(0);
        assertThat(log1.getUpdateBy()).isEqualTo(originalUpdateBy);
        assertThat(log1.getRight().getOriginalUpdateDate()).isEqualTo(event.getUpdateDate());
        assertThat(log1.getRevision()).isEqualTo(event.getRevision());
        assertThat(log1.getChangeType()).isEqualTo(CREATED);
        var log2 = auditLog.get(1);
        assertThat(log2.getUpdateBy()).isEqualTo(updated.getUpdateBy());
        assertThat(log2.getRight().getOriginalUpdateDate()).isEqualTo(updated.getUpdateDate());
        assertThat(log2.getRevision()).isEqualTo(updated.getRevision());
        assertThat(log2.getChangeType()).isEqualTo(UPDATED);
    }

    @Test
    void getAuditLog_full() {
        var originalName = event.getName();
        var originalDesc = event.getDescription();
        event.setName(getRandomSmallString()).setDescription(getRandomSmallString());
        var updated = service.update(event);

        var auditParams = AuditLogParams.ofNonVersioned(eventId, FULL);
        var auditRecords = service.getAuditLog(auditParams);

        assertThat(auditRecords).hasSize(2);
        assertThat(auditRecords.get(0).getChangeType()).isEqualTo(CREATED);

        var record1 = auditRecords.get(1);
        assertThat(record1.getUpdateBy()).isEqualTo(updated.getUpdateBy());
        assertThat(record1.getRevision()).isEqualTo(updated.getRevision());
        assertThat(record1.getChangeType()).isEqualTo(UPDATED);
        var diff = record1.getDiff();
        assertThat(diff.getChanges()).hasSize(2);
        assertThat(diff.getPropertyChanges(NAME).get(0).getLeft()).isEqualTo(originalName);
        assertThat(diff.getPropertyChanges(NAME).get(0).getRight()).isEqualTo(updated.getName());
        assertThat(diff.getPropertyChanges("description").get(0).getLeft()).isEqualTo(originalDesc);
        assertThat(diff.getPropertyChanges("description").get(0).getRight()).isEqualTo(updated.getDescription());
    }

    @Test
    void getAuditLog_nonExistentId_error() {
        var auditParams = AuditLogParams.ofNonVersioned(getRandomLong(), BASIC);
        assertThatThrownBy(() -> service.getAuditLog(auditParams))
                .isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("doesn't found");
    }

    @Test
    void getAuditLog_withVersion_error() {
        var auditParams = AuditLogParams.ofVersioned(getRandomLong(), 1, BASIC);
        assertThatThrownBy(() -> service.getAuditLog(auditParams))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AuditLogParams must have no version");
    }

    @Test
    void findBySource() {
        var result = service.findBySource(event.getSource());
        assertThat(result.size()).isGreaterThanOrEqualTo(1);
        assertThat(result.iterator().next().getSource()).isEqualTo(event.getSource());
    }
}