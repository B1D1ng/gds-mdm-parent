package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.signal.common.model.EventTypeLookup;
import com.ebay.behavior.gds.mdm.signal.common.model.FieldTemplate;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.EventTypeFieldTemplateMappingRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.FieldAttributeTemplateMappingRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.SignalEventTemplateMappingRepository;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;

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

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.CONTAINS;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.EXACT_MATCH;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.STARTS_WITH;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.common.model.search.FieldSearchBy.DESCRIPTION;
import static com.ebay.behavior.gds.mdm.signal.common.model.search.FieldSearchBy.NAME;
import static com.ebay.behavior.gds.mdm.signal.common.model.search.FieldSearchBy.TAG;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.PAGE_SERVE;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.PAGE_VIEW_ENTRY;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.PAGE_VIEW_EXIT;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.attributeTemplate;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.fieldTemplate;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class FieldTemplateServiceIT {

    private final Pageable pageable = PageRequest.of(0, 10);

    @Autowired
    private FieldAttributeTemplateMappingRepository mappingAttributeRepository;

    @Autowired
    private SignalEventTemplateMappingRepository mappingSignalRepository;

    @Autowired
    private EventTypeFieldTemplateMappingRepository mappingEventRepository;

    @Autowired
    private AttributeTemplateService attributeService;

    @Autowired
    private EventTemplateService eventService;

    @Autowired
    private SignalTemplateService signalService;

    @Autowired
    private FieldTemplateService fieldService;

    @Autowired
    private EventTypeLookupService eventTypeLookupService;

    private long immutableEventTypeId;
    private long eventTypeId;
    private long attributeId;
    private long signalId;
    private long eventId;
    private long fieldId;
    private FieldTemplate field;

    @BeforeAll
    void setUpAll() {
        immutableEventTypeId = eventTypeLookupService.getByName(PAGE_VIEW_ENTRY).getId();
        eventTypeId = eventTypeLookupService.getByName(PAGE_SERVE).getId();

        var event = TestModelUtils.eventTemplate().setType(PAGE_SERVE);
        eventId = eventService.create(event).getId();

        var attribute = attributeTemplate(eventId);
        attributeId = attributeService.create(attribute).getId();

        var signal = TestModelUtils.signalTemplate();
        signalId = signalService.create(signal).getId();
    }

    @BeforeEach
    void setUp() {
        field = fieldTemplate(signalId);
        field = fieldService.create(field, Set.of(attributeId), Set.of(immutableEventTypeId));
        field = fieldService.getByIdWithAssociations(field.getId());
        fieldId = field.getId();
    }

    @Test
    void getByIdWithAssociations_initializesAssociations() {
        var persisted = fieldService.getByIdWithAssociations(fieldId);

        assertThat(persisted.getSignal().getId()).isEqualTo(signalId);
        assertThat(persisted.getAttributes().size()).isEqualTo(1);
    }

    @Test
    void create() {
        assertThat(field.getId()).isNotNull();
        assertThat(field.getSignal().getId()).isEqualTo(signalId);
        assertThat(fieldService.findById(fieldId)).isPresent();
        assertThat(mappingAttributeRepository.findByFieldIdAndAttributeId(fieldId, attributeId)).isPresent();
        assertThat(mappingSignalRepository.findBySignalIdAndEventId(signalId, eventId)).isPresent();
        assertThat(mappingEventRepository.findByFieldIdAndEventTypeId(fieldId, eventTypeId)).isPresent();
        assertThat(mappingEventRepository.findByFieldIdAndEventTypeId(fieldId, immutableEventTypeId)).isPresent();

        field = fieldService.getByIdWithAssociations(fieldId);
        assertThat(field.getSignal().getId()).isEqualTo(signalId);
        assertThat(field.getAttributes().size()).isEqualTo(1);
        assertThat(field.getEventTypes()).extracting(EventTypeLookup::getName).contains(PAGE_SERVE, PAGE_VIEW_ENTRY);

        var signal = signalService.getByIdWithAssociations(signalId);
        assertThat(signal.getEvents()).isNotEmpty();
    }

    @Test
    void create_errorByDesign() {
        assertThatThrownBy(() -> fieldService.create(field))
                .isInstanceOf(NotImplementedException.class);
    }

    @Test
    void createAll_errorByDesign() {
        assertThatThrownBy(() -> fieldService.createAll(Set.of(field)))
                .isInstanceOf(NotImplementedException.class);
    }

    @Test
    void update_errorByDesign() {
        assertThatThrownBy(() -> fieldService.update(field))
                .isInstanceOf(NotImplementedException.class);
    }

    @Test
    void delete() {
        signalService.getFields(signalId)
                .forEach(field -> fieldService.delete(field.getId()));

        assertThat(mappingAttributeRepository.findByFieldIdAndAttributeId(fieldId, attributeId)).isEmpty();
        assertThat(mappingSignalRepository.findBySignalIdAndEventId(signalId, eventId)).isEmpty();
        assertThat(mappingEventRepository.findByFieldId(fieldId)).isEmpty();
        assertThatThrownBy(() -> fieldService.getById(fieldId))
                .isInstanceOf(DataNotFoundException.class);

        var signal = signalService.getByIdWithAssociations(signalId);
        assertThat(signal.getEvents()).isEmpty();
    }

    @Test
    void getAttributes() {
        var attributes = fieldService.getAttributes(fieldId);

        assertThat(attributes).hasSize(1);
        assertThat(attributes).extracting(ID).contains(attributeId);
    }

    @Test
    void createAttributeMapping() {
        var attr1 = attributeTemplate(eventId);
        attr1 = attributeService.create(attr1);
        var attr1Id = attr1.getId();

        fieldService.createAttributeMapping(fieldId, attr1Id);

        assertThat(mappingAttributeRepository.findByFieldIdAndAttributeId(fieldId, attr1Id)).isPresent();
        assertThat(mappingEventRepository.findByFieldIdAndEventTypeId(fieldId, eventTypeId)).isPresent();
    }

    @Test
    void deleteAttributeMapping() {
        fieldService.deleteAttributeMapping(fieldId, attributeId);

        assertThat(mappingAttributeRepository.findByFieldIdAndAttributeId(fieldId, attributeId)).isEmpty();
        assertThat(mappingEventRepository.findByFieldIdAndEventTypeId(fieldId, immutableEventTypeId)).isPresent();
        assertThat(mappingEventRepository.findByFieldIdAndEventTypeId(fieldId, eventTypeId)).isEmpty();
    }

    @Test
    void getEventTypes() {
        var eventTypes = fieldService.getEventTypes(fieldId);

        assertThat(eventTypes).isNotEmpty();
    }

    @Test
    void createAndDeleteEventTypeMapping_notImmutable() {
        var eventTypeId1 = eventTypeLookupService.getByName(PAGE_VIEW_EXIT).getId();

        fieldService.createEventTypeMapping(fieldId, eventTypeId1, false);
        assertThat(mappingEventRepository.findByFieldIdAndEventTypeId(fieldId, eventTypeId1)).isPresent();

        fieldService.deleteEventTypeMapping(fieldId, eventTypeId1);
        assertThat(mappingEventRepository.findByFieldIdAndEventTypeId(fieldId, eventTypeId1)).isEmpty();
    }

    @Test
    void createAndDeleteEventTypeMapping_immutable() {
        var eventTypeId1 = eventTypeLookupService.getByName(PAGE_VIEW_EXIT).getId();

        fieldService.createEventTypeMapping(fieldId, eventTypeId1, true);
        assertThat(mappingEventRepository.findByFieldIdAndEventTypeId(fieldId, eventTypeId1)).isPresent();

        fieldService.deleteEventTypeMapping(fieldId, eventTypeId1);
        assertThat(mappingEventRepository.findByFieldIdAndEventTypeId(fieldId, eventTypeId1)).isPresent();
    }

    @Test
    void getById_nonExistentId_error() {
        assertThatThrownBy(() -> fieldService.getById(9999L))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void getAll_byTagNameExactMatch() {
        var term = field.getTag();
        var search = new Search(TAG.name(), term, EXACT_MATCH, pageable);

        var page = fieldService.getAll(search);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTag()).isEqualTo(term);
    }

    @Test
    void getAll_byNameStartsWith() {
        var term = field.getName().substring(0, 5);
        var search = new Search(NAME.name(), term, STARTS_WITH, pageable);

        var page = fieldService.getAll(search);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getName()).contains(term);
    }

    @Test
    void getAll_byDescriptionContains() {
        var term = field.getDescription().substring(3);
        var search = new Search(DESCRIPTION.name(), term, CONTAINS, pageable);

        var page = fieldService.getAll(search);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getDescription()).contains(term);
    }
}