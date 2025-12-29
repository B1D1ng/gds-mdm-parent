package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.audit.AuditRecord;
import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.common.model.FieldGroup;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.model.UpdateUnstagedSignalRequest;
import com.ebay.behavior.gds.mdm.signal.common.model.audit.UnstagedSignalHistory;
import com.ebay.behavior.gds.mdm.signal.service.PlanService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedFieldService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedSignalService;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.VersionedId.MIN_VERSION;
import static com.ebay.behavior.gds.mdm.common.model.audit.AuditLogParams.MODE;
import static com.ebay.behavior.gds.mdm.common.model.audit.AuditMode.BASIC;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedField;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedSignal;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomString;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.DEFINITION;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.WITH_ASSOCIATIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.EXPECTATION_FAILED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class UnstagedSignalLatestVersionResourceIT extends AbstractResourceTest {

    private UnstagedSignal signal;

    @Autowired
    private UnstagedSignalService signalService;

    @Autowired
    private UnstagedFieldService fieldService;

    @Autowired
    private PlanService planService;

    @BeforeEach
    void setUp() {
        var plan = TestModelUtils.plan();
        var planId = planService.create(plan).getId();

        url = getBaseUrl() + V1 + DEFINITION + "/signal";
        signal = unstagedSignal(planId);
    }

    @Test
    void create_noId() {
        var created = requestSpecWithBody(signal)
                .when().post(url)
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getObject(".", UnstagedSignal.class);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getVersion()).isNotNull();
    }

    @Test
    void create_withIdAndVersion() {
        var signalId = VersionedId.of(8L, MIN_VERSION);
        signalService.findById(signalId).ifPresent((id) -> signalService.deleteLatestVersion(id.getId()));
        signal.setSignalId(signalId);

        var created = requestSpecWithBody(signal)
                .when().post(url)
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getObject(".", UnstagedSignal.class);

        assertThat(created.getId()).isEqualTo(signalId.getId());
        assertThat(created.getVersion()).isEqualTo(signalId.getVersion());
    }

    @Test
    void updateLatestVersion() {
        var name = getRandomString();
        var description = getRandomString();
        var created = signalService.create(signal);
        var signalId = created.getId();
        var request = UpdateUnstagedSignalRequest.builder().id(signalId).revision(created.getRevision()).name(name).description(description).build();

        var updated = requestSpecWithBody(request)
                .when().patch(url + '/' + signalId)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", UnstagedSignal.class);

        assertThat(updated.getId()).isEqualTo(signalId);
        assertThat(updated.getName()).isEqualTo(name);
    }

    @Test
    void updateLatestVersion_notFound() {
        var request = UpdateUnstagedSignalRequest.builder().id(getRandomLong()).revision(1).build();

        requestSpecWithBody(request)
                .when().patch(url + '/' + request.getId())
                .then().statusCode(HttpStatus.EXPECTATION_FAILED.value());
    }

    @Test
    void deleteLatestVersion() {
        var created = signalService.create(signal);

        requestSpec().when().delete(url + '/' + created.getId())
                .then().statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void getByIdAndLatestVersion() {
        var signalId = signalService.create(signal).getId();

        var persisted = requestSpec()
                .queryParam(WITH_ASSOCIATIONS, true)
                .when().get(url + '/' + signalId)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", UnstagedSignal.class);

        assertThat(persisted.getId()).isEqualTo(signalId);
    }

    @Test
    void getById_AndLatestVersion_notFound() {
        requestSpec()
                .when().get(url + '/' + getRandomLong())
                .then().statusCode(HttpStatus.EXPECTATION_FAILED.value());
    }

    @Test
    void getFields() {
        var signalId = signalService.create(signal).getSignalId();

        var field1 = unstagedField(signalId);
        var field2 = unstagedField(signalId);
        fieldService.create(field1, Set.of());
        fieldService.create(field2, Set.of());

        var fields = requestSpec()
                .when().get(url + '/' + signalId.getId() + "/fields")
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", UnstagedField.class);

        assertThat(fields).hasSize(2);
    }

    @Test
    void getFieldGroups() {
        var signalId = signalService.create(signal).getSignalId();

        var field1 = unstagedField(signalId);
        var field2 = unstagedField(signalId);
        fieldService.create(field1, Set.of());
        fieldService.create(field2, Set.of());

        var fields = requestSpec()
                .when().get(url + '/' + signalId.getId() + "/field-groups")
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", FieldGroup.class);

        assertThat(fields).hasSize(2);
    }

    @Test
    void getEvents() {
        var id = signalService.create(signal).getId();

        var events = requestSpec()
                .when().get(url + '/' + id + "/events")
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", UnstagedEvent.class);

        assertThat(events).isEmpty();
    }

    @Test
    void getAuditLog() throws JsonProcessingException {
        var created = signalService.create(signal);

        var json = requestSpec()
                .queryParam(MODE, BASIC)
                .when().get(url + '/' + created.getId() + "/auditLog")
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().asString();

        var auditLog = objectMapper.readValue(json, new TypeReference<List<AuditRecord<UnstagedSignalHistory>>>() {
        });

        assertThat(auditLog).hasSize(1);
        assertThat(auditLog.get(0).getRight().getOriginalVersion()).isNotNull();
    }

    @Test
    void getAuditLog_notFound_417() {
        requestSpec()
                .queryParam(MODE, BASIC)
                .when().get(url + '/' + getRandomLong() + "/auditLog")
                .then().statusCode(EXPECTATION_FAILED.value())
                .and().contentType(APPLICATION_JSON_VALUE);
    }
}