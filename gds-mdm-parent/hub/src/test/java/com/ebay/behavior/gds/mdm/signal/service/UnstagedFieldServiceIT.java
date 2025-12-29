package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.audit.AuditLogParams;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.UpdateUnstagedFieldRequest;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.UnstagedFieldAttributeMappingRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.UnstagedSignalEventMappingRepository;
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
import static com.ebay.behavior.gds.mdm.common.model.audit.AuditMode.BASIC;
import static com.ebay.behavior.gds.mdm.common.model.audit.AuditMode.FULL;
import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.CREATED;
import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.UPDATED;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.CONTAINS;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.EXACT_MATCH;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.STARTS_WITH;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.common.model.search.FieldSearchBy.DESCRIPTION;
import static com.ebay.behavior.gds.mdm.signal.common.model.search.FieldSearchBy.NAME;
import static com.ebay.behavior.gds.mdm.signal.common.model.search.FieldSearchBy.TAG;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedAttribute;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedField;
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
class UnstagedFieldServiceIT {

    private final Pageable pageable = PageRequest.of(0, 10);

    @Autowired
    private UnstagedFieldAttributeMappingRepository mappingAttributeRepository;

    @Autowired
    private UnstagedSignalEventMappingRepository mappingEventRepository;

    @Autowired
    private UnstagedAttributeService attributeService;

    @Autowired
    private UnstagedEventService eventService;

    @Autowired
    private UnstagedSignalService signalService;

    @Autowired
    private UnstagedFieldService fieldService;

    @Autowired
    private PlanService planService;

    private long attributeId;
    private long eventId;
    private long fieldId;
    private VersionedId signalId;
    private UnstagedField field;

    @BeforeAll
    void setUpAll() {
        var plan = TestModelUtils.plan();
        var planId = planService.create(plan).getId();

        var event = TestModelUtils.unstagedEvent();
        eventId = eventService.create(event).getId();

        var attribute = unstagedAttribute(eventId);
        attributeId = attributeService.create(attribute).getId();

        var signal = TestModelUtils.unstagedSignal(planId);
        signalId = signalService.create(signal).getSignalId();
    }

    @BeforeEach
    void setUp() {
        field = unstagedField(signalId);
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
        assertThat(mappingEventRepository.findBySignalIdAndSignalVersionAndEventId(signalId.getId(), signalId.getVersion(), eventId)).isPresent();

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
    void update_withUpdateRequest() {
        var request = UpdateUnstagedFieldRequest.builder()
                .id(fieldId)
                .description("updated")
                .build();

        var updated = fieldService.update(request);

        updated = fieldService.getByIdWithAssociations(updated.getId());
        assertThat(updated.getDescription()).isEqualTo("updated");
        assertThat(updated.getAttributes().size()).isEqualTo(1);
    }

    @Test
    void update_deleteAssociations() {
        var request = UpdateUnstagedFieldRequest.builder()
                .id(fieldId)
                .attributeIds(Set.of()) // this line mention we delete all previous associations
                .build();

        var updated = fieldService.update(request);

        updated = fieldService.getByIdWithAssociations(updated.getId());
        assertThat(updated.getAttributes()).isEmpty();
    }

    @Test
    void delete() {
        signalService.getFields(signalId)
                .forEach(fld -> fieldService.delete(fld.getId()));

        assertThat(mappingAttributeRepository.findByFieldIdAndAttributeId(fieldId, attributeId)).isEmpty();
        assertThat(mappingEventRepository.findBySignalIdAndSignalVersionAndEventId(signalId.getId(), signalId.getVersion(), eventId)).isEmpty();
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
    void deleteAttributeMapping() {
        fieldService.deleteAttributeMapping(fieldId, attributeId);

        assertThat(mappingAttributeRepository.findByFieldIdAndAttributeId(fieldId, attributeId)).isEmpty();
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

    @Test
    void getAuditLog_basic() {
        var originalUpdateBy = field.getUpdateBy();
        var request = UpdateUnstagedFieldRequest.builder().id(fieldId).name(getRandomSmallString()).build();
        var updated = fieldService.update(request);

        var auditParams = AuditLogParams.ofNonVersioned(fieldId, BASIC);
        var auditRecords = fieldService.getAuditLog(auditParams);

        assertThat(auditRecords).hasSize(2);
        var record1 = auditRecords.get(0);
        assertThat(record1.getUpdateBy()).isEqualTo(originalUpdateBy);
        assertThat(record1.getRevision()).isEqualTo(field.getRevision());
        assertThat(record1.getChangeType()).isEqualTo(CREATED);
        var record2 = auditRecords.get(1);
        assertThat(record2.getUpdateBy()).isEqualTo(updated.getUpdateBy());
        assertThat(record2.getRevision()).isEqualTo(updated.getRevision());
        assertThat(record2.getChangeType()).isEqualTo(UPDATED);
    }

    @Test
    void getAuditLog_full() {
        var originalName = field.getName();
        var originalDesc = field.getDescription();
        var request = UpdateUnstagedFieldRequest.builder().id(fieldId).name(getRandomSmallString()).description(getRandomSmallString()).build();
        var updated = fieldService.update(request);

        var auditParams = AuditLogParams.ofNonVersioned(fieldId, FULL);
        var auditRecords = fieldService.getAuditLog(auditParams);

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
        assertThatThrownBy(() -> fieldService.getAuditLog(auditParams))
                .isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("doesn't found");
    }
}