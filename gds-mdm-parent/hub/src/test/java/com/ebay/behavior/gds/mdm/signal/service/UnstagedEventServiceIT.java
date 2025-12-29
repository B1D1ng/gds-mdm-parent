package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.audit.AuditLogParams;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.signal.common.model.FieldGroup;
import com.ebay.behavior.gds.mdm.signal.common.model.MetadataField;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.UpdateUnstagedEventRequest;
import com.ebay.behavior.gds.mdm.signal.common.model.search.EventSearchBy;
import com.ebay.behavior.gds.mdm.signal.model.manyToMany.SojEventTagMapping;
import com.ebay.behavior.gds.mdm.signal.repository.SojBusinessTagRepository;
import com.ebay.behavior.gds.mdm.signal.repository.SojEventRepository;
import com.ebay.behavior.gds.mdm.signal.repository.audit.UnstagedEventHistoryRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.SojEventTagMappingRepository;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;

import lombok.val;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.ExpressionType.JEXL;
import static com.ebay.behavior.gds.mdm.common.model.Model.COMMA;
import static com.ebay.behavior.gds.mdm.common.model.audit.AuditMode.BASIC;
import static com.ebay.behavior.gds.mdm.common.model.audit.AuditMode.FULL;
import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.CREATED;
import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.DELETED;
import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.UPDATED;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.CONTAINS;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.EXACT_MATCH;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.EXACT_MATCH_IGNORE_CASE;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.STARTS_WITH;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.common.model.search.EventSearchBy.DESCRIPTION;
import static com.ebay.behavior.gds.mdm.signal.common.model.search.EventSearchBy.PAGE_ID;
import static com.ebay.behavior.gds.mdm.signal.common.model.search.EventSearchBy.TYPE;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.CLIENT_PAGE_VIEW;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.PAGE_SERVE;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.sojBusinessTag;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.sojEvent;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedAttribute;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedEvent;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedField;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UnstagedEventServiceIT {

    private final Pageable pageable = PageRequest.of(0, 10);

    @Autowired
    private UnstagedEventService eventService;

    @Autowired
    private UnstagedAttributeService attributeService;

    @Autowired
    private UnstagedSignalService signalService;

    @Autowired
    private UnstagedFieldService fieldService;

    @Autowired
    private PlanService planService;

    @Autowired
    private UnstagedEventHistoryRepository historyRepository;

    @Autowired
    private SojBusinessTagRepository sojBusinessTagRepository;

    @Autowired
    private SojEventTagMappingRepository sojTagMappingRepository;

    @Autowired
    private SojEventRepository sojEventRepository;

    @MockitoSpyBean
    private BusinessFieldService businessFieldService;

    private long planId;
    private VersionedId signalId;
    private long eventId;
    private UnstagedEvent event;

    @BeforeAll
    void setUpAll() {
        var plan = TestModelUtils.plan();
        planId = planService.create(plan).getId();

        var signal = TestModelUtils.unstagedSignal(planId);
        signalId = signalService.create(signal).getSignalId();

        createSojEventAndTagSetup(List.of(456L, 1L)); // old moduleId: 1, new moduleId: 456
    }

    @BeforeEach
    void setUp() {
        event = unstagedEvent().toBuilder()
                .expressionType(JEXL)
                .moduleIds(Set.of(1L))
                .build();
        event = eventService.create(event);
        event = eventService.getById(event.getId());
        eventId = event.getId();
        Mockito.reset(businessFieldService);
    }

    @Test
    void create() {
        assertThat(event.getId()).isNotNull();
        assertThat(event.getPageIds()).isEmpty();
        assertThat(event.getModuleIds()).hasSize(1);
        assertThat(event.getClickIds()).isEmpty();
        assertThat(eventService.findById(eventId)).isPresent();

        var histories = historyRepository.findByOriginalId(eventId);

        assertThat(histories.size()).isEqualTo(1);
        var history = histories.get(0);
        assertThat(history.getOriginalId()).isEqualTo(eventId);
        assertThat(history.getOriginalRevision()).isEqualTo(event.getRevision());
        assertThat(history.getOriginalCreateDate()).isEqualTo(event.getCreateDate());
        assertThat(history.getOriginalUpdateDate()).isEqualTo(event.getUpdateDate());
        assertThat(history.getName()).isEqualTo(event.getName());
        assertThat(history.getChangeType()).isEqualTo(CREATED);
    }

    @Test
    void update_notImplemented_error() {
        assertThatThrownBy(() -> eventService.update(event))
                .isInstanceOf(NotImplementedException.class);
    }

    @Test
    void update_withoutExpression() {
        var desc = event.getDescription();
        var fsmOrder = event.getFsmOrder();
        var cardinality = event.getCardinality();
        var request = UpdateUnstagedEventRequest.builder().id(eventId).name("updated").build();
        var updated = eventService.update(request);

        assertThat(updated.getName()).isEqualTo("updated"); // only name was updated
        assertThat(updated.getDescription()).isEqualTo(desc);
        assertThat(updated.getFsmOrder()).isEqualTo(fsmOrder);
        assertThat(updated.getCardinality()).isEqualTo(cardinality);
        assertThat(updated.getPageIds()).isEmpty();
        assertThat(updated.getModuleIds()).isNotEmpty();
        verify(businessFieldService, never()).simulateBusinessFields(eq(signalId), any(), eq(true));
        verify(businessFieldService, never()).createBusinessFields(any(), anyLong(), anyBoolean());

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
    void update_withExpression_assetIdsAndBusinessFieldsUpdated() {
        var attribute = unstagedAttribute(eventId);
        var attributeId = attributeService.create(attribute).getId();
        var field = unstagedField(signalId);
        fieldService.create(field, Set.of(attributeId));
        businessFieldService.createBusinessFields(signalId, eventId, true);
        Mockito.reset(businessFieldService);

        var attributesBefore = eventService.getAttributes(eventId);
        var fieldsBefore = signalService.getFields(signalId);
        assertThat(attributesBefore).hasSize(2);
        assertThat(attributesBefore).extracting("tag").contains("tag2"); // tag2 is old business tag
        assertThat(fieldsBefore).hasSize(2);
        assertThat(fieldsBefore).extracting("tag").contains("tag2"); // tag2 is old business tag

        var request = UpdateUnstagedEventRequest.builder()
                .id(eventId)
                .expressionType(JEXL)
                .expression("[456].contains(event.context.pageInteractionContext.moduleId)")
                .build();

        var updated = eventService.update(request);

        assertThat(updated.getModuleIds()).containsExactly(456L);
        assertThat(updated.getExpression()).contains("456");
        verify(businessFieldService, times(1)).simulateBusinessFields(eq(signalId), any(), eq(true));
        verify(businessFieldService, times(1)).createBusinessFields(any(), anyLong(), eq(true));

        var attributesAfter = eventService.getAttributes(eventId);
        var fieldsAfter = signalService.getFields(signalId);
        assertThat(attributesAfter).hasSize(2);
        assertThat(attributesAfter).extracting("tag").contains("tag1").doesNotContain("tag2"); // old business tag replaced (tag2 -> tag1)
        assertThat(fieldsAfter).hasSize(2);
        assertThat(fieldsAfter).extracting("tag").contains("tag1").doesNotContain("tag2"); // old business tag replaced (tag2 -> tag1)
    }

    @Test
    void evaluateExpressionUpdate() {
        var signal = TestModelUtils.unstagedSignal(planId);
        signalId = signalService.create(signal).getSignalId();

        event = unstagedEvent().toBuilder()
                .expressionType(JEXL)
                .moduleIds(Set.of(1L))
                .build();
        event = eventService.create(event);
        eventId = event.getId();

        var attribute = unstagedAttribute(eventId);
        var attributeId = attributeService.create(attribute).getId();

        var field = unstagedField(signalId).toBuilder().tag("tag3").build();
        field.setIsMandatory(true);
        fieldService.create(field, Set.of(attributeId));

        businessFieldService.createBusinessFields(signalId, eventId, true);
        Mockito.reset(businessFieldService);

        var fieldsBefore = signalService.getFields(signalId).stream()
                .filter(not(MetadataField::getIsMandatory))
                .toList();
        assertThat(fieldsBefore).extracting("tag").contains("tag2").doesNotContain("tag1", "tag3"); // tag2 is old business tag

        val expression = "[456].contains(event.context.pageInteractionContext.moduleId)";
        var response = eventService.evaluateExpressionUpdate(eventId, expression, JEXL);

        assertThat(response.currentFields()).hasSize(1);
        assertThat(response.currentFields()).extracting(FieldGroup::getTag).contains("tag2").doesNotContain("tag3"); // tag2 is old business tag
        assertThat(response.nextFields()).hasSize(1);
        assertThat(response.nextFields()).extracting(FieldGroup::getTag).contains("tag1").doesNotContain("tag3"); // tag1 is new business tag

        verify(businessFieldService, times(1)).simulateBusinessFields(eq(signalId), any(), eq(true));
        verify(businessFieldService, never()).createBusinessFields(any(), anyLong(), eq(true));
    }

    @Test
    void delete() {
        var attribute = unstagedAttribute(eventId);
        attributeService.create(attribute);

        eventService.delete(eventId);

        assertThatThrownBy(() -> eventService.getById(eventId))
                .isInstanceOf(DataNotFoundException.class);

        assertThatThrownBy(() -> eventService.getAttributes(eventId))
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
    void delete_fieldWithAnotherEventNotDeleted() {
        var signal = TestModelUtils.unstagedSignal(planId);
        var signalId = signalService.create(signal).getSignalId();

        var event1 = unstagedEvent().toBuilder()
                .pageIds(Set.of(getRandomLong(), getRandomLong()))
                .moduleIds(Set.of(getRandomLong()))
                .build();
        var eventId1 = eventService.create(event1).getId();

        var attribute = unstagedAttribute(eventId);
        var attributeId = attributeService.create(attribute).getId();
        var attribute1 = unstagedAttribute(eventId1);
        var attributeId1 = attributeService.create(attribute1).getId();

        var field = unstagedField(signalId);
        // this field also associated to another event (event1, using attributeId1), so it should not be deleted by delete(eventId) call
        var fieldId = fieldService.create(field, Set.of(attributeId, attributeId1)).getId();

        eventService.delete(eventId);

        assertThatThrownBy(() -> eventService.getById(eventId))
                .isInstanceOf(DataNotFoundException.class);
        assertThatThrownBy(() -> eventService.getAttributes(eventId))
                .isInstanceOf(DataNotFoundException.class);
        assertThat(eventService.getAttributes(eventId1).get(0).getId()).isEqualTo(attributeId1);
        assertThat(fieldService.findById(fieldId)).isPresent();
        assertThat(signalService.getFields(signalId)).extracting(UnstagedField::getId).containsOnly(fieldId);
        assertThat(signalService.getEvents(signalId)).extracting(UnstagedEvent::getId).containsOnly(eventId1);
    }

    @Test
    void delete_withNoAttributeFields_fieldsOfSameEventTypeWasDeleted() {
        // Given
        var signal = TestModelUtils.unstagedSignal(planId);
        var signalId = signalService.create(signal).getSignalId();

        var event1 = unstagedEvent().toBuilder()
                .type(PAGE_SERVE)
                .pageIds(Set.of(getRandomLong(), getRandomLong()))
                .moduleIds(Set.of(getRandomLong()))
                .build();
        var eventId1 = eventService.create(event1).getId();

        var attribute1 = unstagedAttribute(eventId1);
        var attributeId1 = attributeService.create(attribute1).getId();

        // A field for PAGE_SERVE and with attributes. Should be deleted.
        var field1 = unstagedField(signalId).toBuilder().eventTypes(PAGE_SERVE).build();
        field1 = fieldService.create(field1, Set.of(attributeId1));
        var fieldId1 = field1.getId();

        // A field for PAGE_SERVE and without attributes. Should be deleted.
        var field2 = unstagedField(signalId).toBuilder().eventTypes(PAGE_SERVE).build();
        field2 = fieldService.create(field2, Set.of());
        var fieldId2 = field2.getId();

        // A field with two event types: PAGE_SERVE and CLIENT_PAGE_VIEW. Should not be deleted.
        var field3 = unstagedField(signalId).toBuilder().eventTypes(String.join(COMMA, PAGE_SERVE, CLIENT_PAGE_VIEW)).build();
        fieldService.create(field3, Set.of());

        // When
        eventService.delete(eventId1);

        // Then
        assertThatThrownBy(() -> eventService.getById(eventId1))
                .isInstanceOf(DataNotFoundException.class);

        assertThat(fieldService.findById(fieldId1)).isEmpty();
        assertThat(fieldService.findById(fieldId2)).isEmpty();
        assertThat(signalService.getEvents(signalId)).isEmpty();

        var fields = signalService.getFields(signalId);
        assertThat(fields).hasSize(1);
        val field = fields.iterator().next();
        assertThat(field.getEventTypes()).isEqualTo(CLIENT_PAGE_VIEW); // since PAGE_SERVE event was deleted
    }

    @Test
    void getById_nonExistentId_error() {
        assertThatThrownBy(() -> eventService.getById(9999L))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void getAttributes() {
        var attributes = eventService.getAttributes(eventId);
        assertThat(attributes).isEmpty();

        var attribute = unstagedAttribute(eventId);
        attributeService.create(attribute);
        attributes = eventService.getAttributes(eventId);

        assertThat(attributes.size()).isEqualTo(1);
    }

    @Test
    void getAll_byNameExactMatch() {
        var term = event.getName();
        var search = new Search(EventSearchBy.NAME.name(), term, EXACT_MATCH, pageable);

        var page = eventService.getAll(search);

        var persisted = page.getContent().get(0);
        assertThat(page.getContent()).hasSize(1);
        assertThat(persisted.getName()).isEqualTo(term);
        assertThat(persisted.getPageIds()).isEmpty();
        assertThat(persisted.getModuleIds()).hasSize(1);
        assertThat(persisted.getClickIds()).isEmpty();
    }

    @Test
    void getAll_byTypeStartsWith() {
        var term = event.getType().substring(0, 5);
        var search = new Search(TYPE.name(), term, STARTS_WITH, pageable);

        var page = eventService.getAll(search);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getType()).contains(term);
    }

    @Test
    void getAll_byDescriptionContains() {
        var term = event.getDescription().substring(3);
        var search = new Search(DESCRIPTION.name(), term, CONTAINS, pageable);

        var page = eventService.getAll(search);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getDescription()).contains(term);
    }

    @Test
    void getAll_byPageId() {
        var pageId = getRandomLong();
        var event1 = unstagedEvent().toBuilder()
                .pageIds(Set.of(pageId, getRandomLong(), getRandomLong()))
                .moduleIds(Set.of(getRandomLong(), getRandomLong()))
                .build();
        var event2 = unstagedEvent().toBuilder()
                .pageIds(Set.of(pageId, getRandomLong(), getRandomLong()))
                .moduleIds(Set.of(getRandomLong(), getRandomLong()))
                .build();
        eventService.create(event1);
        eventService.create(event2);

        var term = String.valueOf(pageId);
        var search = new Search(PAGE_ID.name(), term, EXACT_MATCH, pageable);

        var page = eventService.getAll(search);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getPageIds()).contains(pageId);
        assertThat(page.getContent().get(1).getPageIds()).contains(pageId);
    }

    @Test
    void getAll_byPageIdWithUnsupportedCriterion_error() {
        var search = new Search(PAGE_ID.name(), "123", EXACT_MATCH_IGNORE_CASE, pageable);

        assertThatThrownBy(() -> eventService.getAll(search))
                .isInstanceOf(NotImplementedException.class)
                .hasMessageContaining("not supported");
    }

    @Test
    void getAll_byModuleId() {
        var moduleId = getRandomLong();
        var event1 = unstagedEvent().toBuilder().moduleIds(Set.of(moduleId, getRandomLong())).build();
        var event2 = unstagedEvent().toBuilder().moduleIds(Set.of(moduleId, getRandomLong())).build();
        eventService.create(event1);
        eventService.create(event2);

        var term = String.valueOf(moduleId);
        var search = new Search(EventSearchBy.MODULE_ID.name(), term, EXACT_MATCH, pageable);

        var page = eventService.getAll(search);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getModuleIds()).contains(moduleId);
        assertThat(page.getContent().get(1).getModuleIds()).contains(moduleId);
    }

    @Test
    void getAll_byClickId() {
        var clickId = getRandomLong();
        var event1 = unstagedEvent().toBuilder().clickIds(Set.of(clickId, getRandomLong())).build();
        var event2 = unstagedEvent().toBuilder().clickIds(Set.of(clickId, getRandomLong())).build();
        eventService.create(event1);
        eventService.create(event2);

        var term = String.valueOf(clickId);
        var search = new Search(EventSearchBy.CLICK_ID.name(), term, EXACT_MATCH, pageable);

        var page = eventService.getAll(search);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getClickIds()).contains(clickId);
        assertThat(page.getContent().get(1).getClickIds()).contains(clickId);
    }

    @Test
    void getAuditLog_basic() {
        var originalUpdateBy = event.getUpdateBy();
        var request = UpdateUnstagedEventRequest.builder().id(eventId).name(getRandomSmallString()).build();
        var updated = eventService.update(request);

        var auditParams = AuditLogParams.ofNonVersioned(eventId, BASIC);
        var auditRecords = eventService.getAuditLog(auditParams);

        assertThat(auditRecords).hasSize(2);
        var record1 = auditRecords.get(0);
        assertThat(record1.getUpdateBy()).isEqualTo(originalUpdateBy);
        assertThat(record1.getRevision()).isEqualTo(event.getRevision());
        assertThat(record1.getChangeType()).isEqualTo(CREATED);
        var record2 = auditRecords.get(1);
        assertThat(record2.getRevision()).isEqualTo(updated.getRevision());
        assertThat(record2.getChangeType()).isEqualTo(UPDATED);
    }

    @Test
    void getAuditLog_full() {
        var originalName = event.getName();
        var originalDesc = event.getDescription();
        var request = UpdateUnstagedEventRequest.builder().id(eventId).name(getRandomSmallString()).description(getRandomSmallString()).build();
        var updated = eventService.update(request);

        var auditParams = AuditLogParams.ofNonVersioned(eventId, FULL);
        var auditRecords = eventService.getAuditLog(auditParams);

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
        assertThatThrownBy(() -> eventService.getAuditLog(auditParams))
                .isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("doesn't found");
    }

    private void createSojEventAndTagSetup(List<Long> ids) {
        var sojEvent1 = sojEvent("EXPC", 111L, ids.get(0), null);
        var sojEvent2 = sojEvent("EXPC", 111L, ids.get(1), null);
        sojEventRepository.saveAll(Set.of(sojEvent1, sojEvent2));

        var sojTag1 = sojBusinessTagRepository.save(sojBusinessTag("tag1"));
        var sojTag2 = sojBusinessTagRepository.save(sojBusinessTag("tag2"));

        sojTagMappingRepository.saveAll(Set.of(
                new SojEventTagMapping(sojEvent1, sojTag1),
                new SojEventTagMapping(sojEvent2, sojTag2)
        ));
    }
}