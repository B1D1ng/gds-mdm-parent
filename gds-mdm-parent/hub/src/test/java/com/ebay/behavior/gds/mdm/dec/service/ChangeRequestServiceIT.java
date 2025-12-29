package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.dec.model.LdmChangeRequest;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmChangeRequestLogRecord;
import com.ebay.behavior.gds.mdm.dec.model.enums.ChangeRequestStatus;
import com.ebay.behavior.gds.mdm.dec.model.enums.ViewType;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils;
import com.ebay.behavior.gds.mdm.dec.util.EntityUtils;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.CONTAINS;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.ldmEntityEmpty;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.namespace;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.INTEGRATION_TEST;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.getRandomLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ChangeRequestServiceIT {

    @Autowired
    private LdmChangeRequestService service;

    @Autowired
    private LdmEntityService entityService;

    @Autowired
    private NamespaceService namespaceService;

    @Autowired
    private LdmBaseEntityService baseEntityService;

    @Autowired
    private LdmReadService readService;

    private LdmChangeRequest request;
    private Long requestId;
    private Long entityId;
    private LdmEntity entity;
    private Namespace namespace;

    @BeforeEach
    void setUp() {
        namespace = namespace();
        namespace = namespaceService.create(namespace);

        entity = ldmEntityEmpty(namespace.getId());
        entity = entityService.create(entity);
        entityId = entity.getId();

        request = TestModelUtils.ldmChangeRequest(entityId);
        request = service.create(request);
        requestId = request.getId();
    }

    @Test
    void approve_LdmViewUpdate() {
        var approvedRequest = service.approve(requestId);
        assertThat(approvedRequest.getStatus()).isEqualTo(ChangeRequestStatus.APPROVED);

        LdmEntity updatedEntity = readService.getByIdWithAssociationsCurrentVersion(entityId);
        assertThat(updatedEntity.getId()).isNotNull();
        assertThat(updatedEntity.getRequestId()).isEqualTo(requestId);
        assertThat(updatedEntity.getFields().size()).isEqualTo(3);
    }

    @Test
    void approve_LdmViewCreate() {
        var baseEntity = TestModelUtils.ldmBaseEntity(namespace.getId());
        baseEntity = baseEntityService.create(baseEntity);

        LdmChangeRequest request = TestModelUtils.ldmViewCreateChangeRequest(baseEntity.getId());
        request = service.create(request);

        var approvedRequest = service.approve(request.getId());
        assertThat(approvedRequest.getStatus()).isEqualTo(ChangeRequestStatus.APPROVED);

        var created = readService.searchByNameAndNamespace(EntityUtils.getLdmName(baseEntity.getName(), ViewType.RAW), "RAW", namespace.getName());
        var createdId = created.get(0).getId();
        var createdInfo = readService.getByIdWithAssociationsCurrentVersion(createdId);

        assertThat(createdInfo.getBaseEntityId()).isEqualTo(baseEntity.getId());
        assertThat(createdInfo.getRequestId()).isEqualTo(request.getId());
        assertThat(createdInfo.getFields().size()).isEqualTo(3);
        var itemIdField = createdInfo.getFields().stream().filter(f -> "item_id".equals(f.getName())).findFirst();
        assertThat(itemIdField).isPresent();
        assertThat(itemIdField.get().getOrdinal()).isEqualTo(1);
        var auctTitleField = createdInfo.getFields().stream().filter(f -> "auct_title".equals(f.getName())).findFirst();
        assertThat(auctTitleField).isPresent();
        assertThat(auctTitleField.get().getOrdinal()).isEqualTo(2);
        var galleryGuidField = createdInfo.getFields().stream().filter(f -> "gallery_guid".equals(f.getName())).findFirst();
        assertThat(galleryGuidField).isPresent();
        assertThat(galleryGuidField.get().getOrdinal()).isEqualTo(3);
    }

    @Test
    void approve_Namespace() {
        LdmChangeRequest request2 = TestModelUtils.namespaceChangeRequest();
        request2 = service.create(request2);

        var approvedRequest = service.approve(request2.getId());
        assertThat(approvedRequest.getStatus()).isEqualTo(ChangeRequestStatus.APPROVED);

        List<Namespace> namespaces = namespaceService.getAll();
        assertThat(namespaces.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void approve_InvalidStatus() {
        LdmChangeRequest request2 = TestModelUtils.namespaceChangeRequest();
        request2 = service.create(request2);
        var requestId2 = request2.getId();
        service.approve(requestId2);

        assertThatThrownBy(() -> service.approve(requestId2)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reject() {
        LdmChangeRequestLogRecord comment = TestModelUtils.ldmChangeRequestLogEntryReject();
        var rejectedRequest = service.reject(requestId, comment);
        assertThat(rejectedRequest.getStatus()).isEqualTo(ChangeRequestStatus.REJECTED);
    }

    @Test
    void reject_InvalidStatus() {
        LdmChangeRequestLogRecord comment = TestModelUtils.ldmChangeRequestLogEntryReject();
        service.reject(requestId, comment);

        assertThatThrownBy(() -> service.reject(requestId, comment)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateApprovalLog() {
        LdmChangeRequestLogRecord comment = TestModelUtils.ldmChangeRequestLogEntry();
        service.updateLogRecord(request, comment);
        assertThat(request.getLogRecords()).isNotEmpty();
    }

    @Test
    void getByIdWithAssociations() {
        assertThatThrownBy(() -> service.getByIdWithAssociations(requestId))
                .isInstanceOf(NotImplementedException.class);
    }

    @Test
    void getAll_Search() {
        assertThatThrownBy(() -> service.getAll(new Search("by", "term", CONTAINS, PageRequest.of(0, 10))))
                .isInstanceOf(NotImplementedException.class);
    }

    @Test
    void create_LdmViewWithoutBaseEntity() {
        LdmChangeRequest request = TestModelUtils.ldmViewCreateChangeRequest(null);
        assertThatThrownBy(() -> service.create(request)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_BaseEntityIdChange() {
        LdmChangeRequest request = TestModelUtils.ldmChangeRequest(entityId, getRandomLong(), entity.getViewType());
        assertThatThrownBy(() -> service.create(request)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_ViewTypeChange() {
        LdmChangeRequest request = TestModelUtils.ldmChangeRequest(entityId, entity.getBaseEntityId(), ViewType.NONE);
        assertThatThrownBy(() -> service.create(request)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_NameChange() {
        LdmChangeRequest request = TestModelUtils.ldmChangeRequest(entityId, entity.getBaseEntityId(), ViewType.NONE);
        assertThatThrownBy(() -> service.create(request)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_NamespaceChange() {
        LdmChangeRequest request = TestModelUtils.ldmChangeRequest(entityId, entity.getBaseEntityId(), ViewType.NONE);
        assertThatThrownBy(() -> service.create(request)).isInstanceOf(IllegalArgumentException.class);
    }
}
