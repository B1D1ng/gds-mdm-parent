package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedField;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.StagedFieldAttributeMappingRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.StagedSignalEventMappingRepository;
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
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.stagedAttribute;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.stagedEvent;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.stagedField;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.stagedSignal;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class StagedFieldServiceIT {

    private final Pageable pageable = PageRequest.of(0, 10);

    @Autowired
    private StagedFieldAttributeMappingRepository mappingAttributeRepository;

    @Autowired
    private StagedSignalEventMappingRepository mappingEventRepository;

    @Autowired
    private StagedAttributeService attributeService;

    @Autowired
    private StagedEventService eventService;

    @Autowired
    private StagedSignalService signalService;

    @Autowired
    private StagedFieldService fieldService;

    @Autowired
    private PlanService planService;

    private long attributeId;
    private long eventId;
    private long fieldId;
    private VersionedId signalId;
    private StagedField field;

    @BeforeAll
    void setUpAll() {
        var plan = TestModelUtils.plan();
        var planId = planService.create(plan).getId();

        var event = stagedEvent();
        eventId = eventService.create(event).getId();

        var attribute = stagedAttribute(eventId);
        attributeId = attributeService.create(attribute).getId();

        var signal = stagedSignal(planId).toBuilder().id(getRandomLong()).version(1).build();
        signalId = signalService.create(signal).getSignalId();
    }

    @BeforeEach
    void setUp() {
        field = stagedField(signalId);
        field = fieldService.create(field, Set.of(attributeId));
        field = fieldService.getById(field.getId());
        fieldId = field.getId();
    }

    @Test
    void getByIdWithAssociations_initializesAssociations() {
        var persisted = fieldService.getByIdWithAssociations(fieldId);

        assertThat(persisted.getSignal().getId()).isEqualTo(signalId.getId());
        assertThat(persisted.getAttributes().size()).isEqualTo(1);
    }

    @Test
    void create() {
        assertThat(field.getId()).isNotNull();
        assertThat(field.getSignalId()).isEqualTo(signalId.getId());
        assertThat(fieldService.findById(fieldId)).isPresent();
        assertThat(mappingAttributeRepository.findByFieldIdAndAttributeId(fieldId, attributeId)).isPresent();
        assertThat(mappingEventRepository.findBySignalIdAndSignalVersionAndEventId(signalId.getId(), signalId.getVersion(), eventId)).isNotEmpty();

        field = fieldService.getByIdWithAssociations(fieldId);
        assertThat(field.getSignalId()).isEqualTo(signalId.getId());
        assertThat(field.getAttributes().size()).isEqualTo(1);

        var signal = signalService.getByIdWithAssociations(signalId);
        assertThat(signal.getEvents()).isNotEmpty();
    }

    @Test
    void create_errorByDesign() {
        assertThatThrownBy(() -> fieldService.create(field))
                .isInstanceOf(NotImplementedException.class);
    }

    @Test
    void update_errorByDesign() {
        assertThatThrownBy(() -> fieldService.update(field))
                .isInstanceOf(NotImplementedException.class);
    }

    @Test
    void delete_errorByDesign() {
        assertThatThrownBy(() -> fieldService.delete(1L))
                .isInstanceOf(NotImplementedException.class);
    }

    @Test
    void getAttributes() {
        var attributes = fieldService.getAttributes(fieldId);

        assertThat(attributes).hasSize(1);
        assertThat(attributes).extracting(ID).contains(attributeId);
    }

    @Test
    void getById_nonExistentId_error() {
        assertThatThrownBy(() -> fieldService.getById(9999L))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void getAllByIds() {
        var fields = fieldService.getAllByIds(Set.of(fieldId));

        assertThat(fields).hasSize(1);
        assertThat(fields.iterator().next().getId()).isEqualTo(fieldId);
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