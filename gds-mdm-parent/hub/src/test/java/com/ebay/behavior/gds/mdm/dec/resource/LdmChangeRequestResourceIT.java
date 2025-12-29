package com.ebay.behavior.gds.mdm.dec.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.dec.model.LdmChangeRequest;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmChangeRequestLogRecord;
import com.ebay.behavior.gds.mdm.dec.model.enums.ActionTarget;
import com.ebay.behavior.gds.mdm.dec.model.enums.ActionType;
import com.ebay.behavior.gds.mdm.dec.model.enums.ChangeRequestStatus;
import com.ebay.behavior.gds.mdm.dec.service.LdmChangeRequestService;
import com.ebay.behavior.gds.mdm.dec.service.LdmEntityService;
import com.ebay.behavior.gds.mdm.dec.service.LdmReadService;
import com.ebay.behavior.gds.mdm.dec.service.NamespaceService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.ldmChangeRequest;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.ldmChangeRequestLogEntryReject;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.ldmEntityEmpty;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.namespace;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.CHANGE_REQUEST_API;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.EXPECTATION_FAILED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class LdmChangeRequestResourceIT extends AbstractResourceTest {

    @Autowired
    private LdmEntityService entityService;

    @Autowired
    private LdmReadService readService;

    @Autowired
    private NamespaceService namespaceService;

    @Autowired
    private LdmChangeRequestService requestService;

    private Long requestId;
    private Long entityId;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + CHANGE_REQUEST_API;

        Namespace namespace = namespace();
        namespace = namespaceService.create(namespace);

        LdmEntity entity = ldmEntityEmpty(namespace.getId());
        entity = entityService.create(entity);
        entityId = entity.getId();

        LdmChangeRequest request = ldmChangeRequest(entityId);
        request = requestService.create(request);
        requestId = request.getId();
    }

    @Test
    void getById() {
        var persisted = requestSpec().when().get(url + '/' + requestId)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", LdmChangeRequest.class);

        assertThat(persisted.getId()).isEqualTo(requestId);
    }

    @Test
    void getById_notFound_417() {
        requestSpec()
                .when().get(url + '/' + getRandomLong())
                .then().statusCode(EXPECTATION_FAILED.value())
                .and().contentType(APPLICATION_JSON_VALUE);
    }

    @Test
    void create() {
        var newRequest = ldmChangeRequest(entityId);
        var created = requestSpecWithBody(newRequest)
                .when().post(url)
                .then().statusCode(CREATED.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", LdmChangeRequest.class);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getRevision()).isEqualTo(0);
    }

    @Test
    void update() {
        LdmChangeRequest request2 = LdmChangeRequest.builder()
                .actionType(ActionType.CREATE)
                .actionTarget(ActionTarget.NAMESPACE)
                .requestDetails("").build();
        request2 = requestService.create(request2);
        request2.setActionTarget(ActionTarget.LDM_VIEW);

        var updated = requestSpecWithBody(request2)
                .when().put(url + String.format("/%d", request2.getId()))
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", LdmChangeRequest.class);

        assertThat(updated.getId()).isEqualTo(request2.getId());
        assertThat(updated.getActionTarget()).isEqualTo(ActionTarget.LDM_VIEW);
    }

    @Test
    void delete() {
        LdmChangeRequest request3 = LdmChangeRequest.builder()
                .actionType(ActionType.CREATE)
                .actionTarget(ActionTarget.NAMESPACE)
                .requestDetails("").build();
        request3 = requestService.create(request3);

        requestSpec().when().delete(url + '/' + request3.getId())
                .then().statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void getAll() {
        var persisted = requestSpec().when().get(url)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getList(".", LdmChangeRequest.class);

        assertThat(persisted.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void approve() {
        var approved = requestSpec()
                .when().put(url + String.format("/%d", requestId) + "/approve")
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", LdmChangeRequest.class);
        assertThat(approved.getStatus()).isEqualTo(ChangeRequestStatus.APPROVED);

        LdmEntity updatedEntity = readService.getByIdWithAssociationsCurrentVersion(entityId);
        assertThat(updatedEntity.getId()).isNotNull();
    }

    @Test
    void reject() {
        LdmChangeRequestLogRecord rejectComment = ldmChangeRequestLogEntryReject();
        var rejected = requestSpecWithBody(rejectComment)
                .when().put(url + String.format("/%d", requestId) + "/reject")
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", LdmChangeRequest.class);
        assertThat(rejected.getStatus()).isEqualTo(ChangeRequestStatus.REJECTED);
    }
}
