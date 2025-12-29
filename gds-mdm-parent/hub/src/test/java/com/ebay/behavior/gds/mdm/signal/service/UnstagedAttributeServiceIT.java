package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.JavaType;
import com.ebay.behavior.gds.mdm.common.model.audit.AuditLogParams;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedAttribute;
import com.ebay.behavior.gds.mdm.signal.common.model.UpdateUnstagedAttributeRequest;
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

import static com.ebay.behavior.gds.mdm.common.model.audit.AuditMode.BASIC;
import static com.ebay.behavior.gds.mdm.common.model.audit.AuditMode.FULL;
import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.CREATED;
import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.UPDATED;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.CONTAINS;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.EXACT_MATCH;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.common.model.search.AttributeSearchBy.DESCRIPTION;
import static com.ebay.behavior.gds.mdm.signal.common.model.search.AttributeSearchBy.TAG;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedAttribute;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UnstagedAttributeServiceIT {

    private final Pageable pageable = PageRequest.of(0, 10);

    @Autowired
    private UnstagedAttributeService service;

    @Autowired
    private UnstagedEventService eventService;

    private long eventId;
    private long attributeId;
    private UnstagedAttribute attribute;

    @BeforeAll
    void setUpAll() {
        var event = TestModelUtils.unstagedEvent();
        eventId = eventService.create(event).getId();
    }

    @BeforeEach
    void setUp() {
        attribute = unstagedAttribute(eventId);
        attribute = service.create(attribute);
        attribute = service.getById(attribute.getId());
        attributeId = attribute.getId();
    }

    @Test
    void create() {
        assertThat(attribute.getId()).isNotNull();
        assertThat(service.findById(attributeId)).isPresent();
    }

    @Test
    void update_errorByDesign() {
        assertThatThrownBy(() -> service.update(attribute))
                .isInstanceOf(NotImplementedException.class);
    }

    @Test
    void update_withUpdateRequest() {
        var request = UpdateUnstagedAttributeRequest.builder().id(eventId).description("updated").javaType(JavaType.FLOAT).build();

        var updated = service.update(request);

        assertThat(updated.getDescription()).isEqualTo("updated");
        assertThat(updated.getJavaType()).isEqualTo(JavaType.FLOAT);
    }

    @Test
    void delete() {
        service.delete(attributeId);

        assertThatThrownBy(() -> service.getById(attributeId))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void tryDelete() {
        var result = service.tryDelete(attributeId);
        assertThat(result).isTrue();
        assertThat(service.findById(attributeId)).isEmpty();
    }

    @Test
    void getByIdWithAssociations_initializesAssociations() {
        var persisted = service.getByIdWithAssociations(attributeId);

        assertThat(persisted.getEvent().getId()).isEqualTo(eventId);
    }

    @Test
    void getById_nonExistentId_error() {
        assertThatThrownBy(() -> service.getById(9999L))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void getAll_byTagExactMatch() {
        var term = attribute.getTag();
        var search = new Search(TAG.name(), term, EXACT_MATCH, pageable);

        var page = service.getAll(search);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTag()).isEqualTo(term);
    }

    @Test
    void getAll_byDescriptionContains() {
        var term = attribute.getDescription().substring(3);
        var search = new Search(DESCRIPTION.name(), term, CONTAINS, pageable);

        var page = service.getAll(search);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getDescription()).contains(term);
    }

    @Test
    void getAuditLog_basic() {
        var originalUpdateBy = attribute.getUpdateBy();
        var request = UpdateUnstagedAttributeRequest.builder().id(attributeId).description(getRandomSmallString()).build();
        var updated = service.update(request);

        var auditParams = AuditLogParams.ofNonVersioned(attributeId, BASIC);
        var auditRecords = service.getAuditLog(auditParams);

        assertThat(auditRecords).hasSize(2);
        var record1 = auditRecords.get(0);
        assertThat(record1.getUpdateBy()).isEqualTo(originalUpdateBy);
        assertThat(record1.getRight().getOriginalUpdateDate()).isEqualTo(attribute.getUpdateDate());
        assertThat(record1.getRevision()).isEqualTo(attribute.getRevision());
        assertThat(record1.getChangeType()).isEqualTo(CREATED);
        var record2 = auditRecords.get(1);
        assertThat(record2.getUpdateBy()).isEqualTo(updated.getUpdateBy());
        assertThat(record2.getRevision()).isEqualTo(updated.getRevision());
        assertThat(record2.getChangeType()).isEqualTo(UPDATED);
    }

    @Test
    void getAuditLog_full() {
        var originalDesc = attribute.getDescription();
        var request = UpdateUnstagedAttributeRequest.builder().id(attributeId).description(getRandomSmallString()).build();
        var updated = service.update(request);

        var auditParams = AuditLogParams.ofNonVersioned(attributeId, FULL);
        var auditRecords = service.getAuditLog(auditParams);

        assertThat(auditRecords).hasSize(2);
        assertThat(auditRecords.get(0).getChangeType()).isEqualTo(CREATED);

        var record = auditRecords.get(1);
        assertThat(record.getUpdateBy()).isEqualTo(updated.getUpdateBy());
        assertThat(record.getRevision()).isEqualTo(updated.getRevision());
        assertThat(record.getChangeType()).isEqualTo(UPDATED);
        var diff = record.getDiff();
        assertThat(diff.getChanges()).hasSize(1);
        assertThat(diff.getPropertyChanges("description").get(0).getLeft()).isEqualTo(originalDesc);
        assertThat(diff.getPropertyChanges("description").get(0).getRight()).isEqualTo(updated.getDescription());
    }

    @Test
    void getAuditLog_nonExistentId_error() {
        var auditParams = AuditLogParams.ofNonVersioned(999_999L, BASIC);
        assertThatThrownBy(() -> service.getAuditLog(auditParams))
                .isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("doesn't found");
    }
}