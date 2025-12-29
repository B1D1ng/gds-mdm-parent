package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.audit.AuditLogParams;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.model.UpdateUnstagedSignalRequest;
import com.ebay.behavior.gds.mdm.signal.repository.UnstagedSignalRepository;
import com.ebay.behavior.gds.mdm.signal.repository.audit.UnstagedSignalHistoryRepository;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.model.VersionedId.MIN_VERSION;
import static com.ebay.behavior.gds.mdm.common.model.audit.AuditMode.BASIC;
import static com.ebay.behavior.gds.mdm.common.model.audit.AuditMode.FULL;
import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.CREATED;
import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.DELETED;
import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.UPDATED;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedEvent;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedField;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedSignal;
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
class UnstagedSignalServiceIT {

    @Autowired
    private UnstagedSignalRepository repository;

    @Autowired
    private UnstagedSignalService service;

    @Autowired
    private UnstagedEventService eventService;

    @Autowired
    private UnstagedAttributeService attributeService;

    @Autowired
    private UnstagedFieldService fieldService;

    @Autowired
    private PlanService planService;

    @Autowired
    private UnstagedSignalHistoryRepository historyRepository;

    private long planId;
    private long eventId;
    private VersionedId signalId;
    private UnstagedSignal signal;

    @BeforeAll
    void setUpAll() {
        var plan = TestModelUtils.plan();
        planId = planService.create(plan).getId();
    }

    @BeforeEach
    void setUp() {
        var event = unstagedEvent();
        eventId = eventService.create(event).getId();

        signal = unstagedSignal(planId);
        signal = service.create(signal);
        signal = service.getById(signal.getSignalId());
        signalId = signal.getSignalId();
    }

    @Test
    void create() {
        assertThat(signal.getId()).isNotNull();
        assertThat(service.findById(signalId)).isPresent();

        var histories = historyRepository.findByOriginalId(signalId.getId());

        assertThat(histories.size()).isEqualTo(1);
        var history = histories.get(0);
        assertThat(history.getOriginalId()).isEqualTo(signalId.getId());
        assertThat(history.getOriginalVersion()).isEqualTo(signalId.getVersion());
        assertThat(history.getOriginalRevision()).isEqualTo(signal.getRevision());
        assertThat(history.getOriginalCreateDate()).isEqualTo(signal.getCreateDate());
        assertThat(history.getOriginalUpdateDate()).isEqualTo(signal.getUpdateDate());
        assertThat(history.getName()).isEqualTo(signal.getName());
        assertThat(history.getChangeType()).isEqualTo(CREATED);
    }

    @Test
    void update_withUpdateRequest() {
        var desc = signal.getDescription();
        var retention = signal.getRetentionPeriod();
        var status = signal.getCompletionStatus();
        var request = UpdateUnstagedSignalRequest.builder()
                .id(signalId.getId())
                .name("updated")
                .description(desc)
                .retentionPeriod(retention)
                .revision(signal.getRevision())
                .build();
        var updated = service.updateLatestVersion(request);

        assertThat(updated.getName()).isEqualTo("updated"); // only name was updated
        assertThat(updated.getDescription()).isEqualTo(desc);
        assertThat(updated.getRetentionPeriod()).isEqualTo(retention);
        assertThat(updated.getCompletionStatus()).isEqualTo(status);

        var histories = historyRepository.findByOriginalId(signalId.getId());

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
    void getById_nonExistentId_error() {
        assertThatThrownBy(() -> service.getById(VersionedId.of(9999L, MIN_VERSION)))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void getFields() {
        var fields = service.getFields(signalId);
        assertThat(fields).isEmpty();

        var field = unstagedField(signalId);
        fieldService.create(field, Set.of());
        fields = service.getFields(signalId);

        assertThat(fields).isNotEmpty();
    }

    @Test
    void getEvents() {
        var events = service.getEvents(signalId);
        assertThat(events).isEmpty();

        var event = unstagedEvent();
        eventService.create(event);
        service.createEventMapping(signalId, event.getId());
        events = service.getEvents(signalId);

        assertThat(events).isNotEmpty();
    }

    @Test
    void getByIdWithAssociations() {
        var persisted = service.getByIdWithAssociations(signalId);
        assertThat(persisted.getEvents()).isEmpty();
        assertThat(persisted.getFields()).isEmpty();

        var field = unstagedField(signalId);
        field = fieldService.create(field, Set.of()); // associate with fields
        service.createEventMapping(signalId, eventId); // associate with events

        persisted = service.getByIdWithAssociations(signalId);
        assertThat(persisted.getEvents()).isNotEmpty();
        assertThat(persisted.getFields()).isNotEmpty();

        // nullify fields to test "fields" not updatable
        persisted.setFields(null);
        repository.save(persisted);

        persisted = service.getByIdWithAssociations(signalId);
        assertThat(persisted.getFields()).isNotEmpty();

        fieldService.delete(field.getId()); // dissociate with fields
        service.deleteEventMapping(signalId, eventId); // dissociate with events

        persisted = service.getByIdWithAssociations(signalId);
        assertThat(persisted.getEvents()).isEmpty();
        assertThat(persisted.getFields()).isEmpty();
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
        var event1 = unstagedEvent();
        var event2 = unstagedEvent();
        var eventId1 = eventService.create(event1).getId();
        var eventId2 = eventService.create(event2).getId();
        service.createEventMapping(signalId, eventId1);
        service.createEventMapping(signalId, eventId2);

        var persisted = service.getByIdWithAssociations(signalId);
        assertThat(persisted.getEvents()).extracting(ID).contains(eventId1, eventId2);

        service.deleteEventMappings(signalId, Set.of(eventId1, eventId2));
        persisted = service.getByIdWithAssociations(signalId);
        assertThat(persisted.getEvents()).extracting(ID).doesNotContain(eventId1, eventId2);
    }

    @Test
    void deleteLatestVersion() {
        // given
        var attribute = TestModelUtils.unstagedAttribute(eventId);
        var attributeId = attributeService.create(attribute).getId();

        var field = unstagedField(signalId);
        var fieldId = fieldService.create(field, Set.of(attributeId)).getId();

        var signalWithAssociations = service.getByIdWithAssociations(signalId);
        assertThat(signalWithAssociations.getEvents().size()).isEqualTo(1);
        assertThat(signalWithAssociations.getFields().size()).isEqualTo(1);

        // when
        service.deleteLatestVersion(signalId.getId());

        // then
        assertThat(service.findById(signalId)).isEmpty();
        assertThat(fieldService.findById(fieldId)).isEmpty();
        assertThat(attributeService.findById(attributeId)).isEmpty();
        assertThat(eventService.findById(eventId)).isEmpty();

        var histories = historyRepository.findByOriginalId(signalId.getId());

        assertThat(histories.size()).isEqualTo(2);
        var record = histories.get(1);
        assertThat(record.getOriginalId()).isEqualTo(signalId.getId());
        assertThat(record.getOriginalRevision()).isEqualTo(signal.getRevision());
        assertThat(record.getOriginalVersion()).isEqualTo(signalId.getVersion());
        assertThat(record.getOriginalCreateDate()).isEqualTo(signal.getCreateDate());
        assertThat(record.getOriginalUpdateDate()).isEqualTo(signal.getUpdateDate());
        assertThat(record.getName()).isEqualTo(signal.getName());
        assertThat(record.getChangeType()).isEqualTo(DELETED);
    }

    @Test
    void delete() {
        // given
        var attribute = TestModelUtils.unstagedAttribute(eventId);
        var attributeId = attributeService.create(attribute).getId();

        var field = unstagedField(signalId);
        var fieldId = fieldService.create(field, Set.of(attributeId)).getId();

        var signalWithAssociations = service.getByIdWithAssociations(signalId);
        assertThat(signalWithAssociations.getEvents().size()).isEqualTo(1);
        assertThat(signalWithAssociations.getFields().size()).isEqualTo(1);

        // when
        service.delete(signalId);

        // then
        assertThat(service.findById(signalId)).isEmpty();
        assertThat(fieldService.findById(fieldId)).isEmpty();
        assertThat(attributeService.findById(attributeId)).isEmpty();
        assertThat(eventService.findById(eventId)).isEmpty();
    }

    @Test
    void getAuditLog_basic() {
        var originalUpdateBy = signal.getUpdateBy();
        var request = UpdateUnstagedSignalRequest.builder()
                .id(signalId.getId())
                .name(getRandomSmallString())
                .description(signal.getDescription())
                .retentionPeriod(signal.getRetentionPeriod())
                .revision(signal.getRevision())
                .build();
        var updated = service.updateLatestVersion(request);

        var auditParams = AuditLogParams.ofVersioned(signalId.getId(), signalId.getVersion(), BASIC);
        var auditRecords = service.getAuditLog(auditParams);

        assertThat(auditRecords).hasSize(2);
        var record1 = auditRecords.get(0);
        assertThat(record1.getUpdateBy()).isEqualTo(originalUpdateBy);
        assertThat(record1.getRight().getOriginalUpdateDate()).isEqualTo(signal.getUpdateDate());
        assertThat(record1.getRevision()).isEqualTo(signal.getRevision());
        assertThat(record1.getChangeType()).isEqualTo(CREATED);
        var record2 = auditRecords.get(1);
        assertThat(record2.getUpdateBy()).isEqualTo(updated.getUpdateBy());
        assertThat(record2.getRevision()).isEqualTo(updated.getRevision());
        assertThat(record2.getChangeType()).isEqualTo(UPDATED);
    }

    @Test
    void getAuditLog_full() {
        var originalName = signal.getName();
        var originalDesc = signal.getDescription();
        var request = UpdateUnstagedSignalRequest.builder()
                .id(signalId.getId())
                .name(getRandomSmallString())
                .revision(signal.getRevision())
                .description(getRandomSmallString())
                .retentionPeriod(signal.getRetentionPeriod())
                .build();
        var updated = service.updateLatestVersion(request);

        var auditParams = AuditLogParams.ofVersioned(signalId.getId(), signalId.getVersion(), FULL);
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
        var auditParams = AuditLogParams.ofVersioned(getRandomLong(), signalId.getVersion(), BASIC);
        assertThatThrownBy(() -> service.getAuditLog(auditParams))
                .isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("doesn't found");
    }
}
