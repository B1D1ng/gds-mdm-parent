package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.audit.AuditRecord;
import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.common.model.FieldGroup;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
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

class UnstagedSignalAnyVersionResourceIT extends AbstractResourceTest {

    @Autowired
    private UnstagedSignalService signalService;

    @Autowired
    private UnstagedFieldService fieldService;

    @Autowired
    private PlanService planService;

    private UnstagedSignal signal;
    private long planId;

    @BeforeEach
    void setUp() {
        var plan = TestModelUtils.plan();
        planId = planService.create(plan).getId();

        url = getBaseUrl() + V1 + DEFINITION + "/signal";
        signal = unstagedSignal(planId);
    }

    @Test
    void update() {
        var created = signalService.create(signal);
        var signalId = created.getSignalId();
        var name = getRandomString();
        created.setName(name);

        var updated = requestSpecWithBody(created)
                .when().put(url + '/' + signalId.getId() + "/version/" + signalId.getVersion())
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", UnstagedSignal.class);

        assertThat(updated.getId()).isEqualTo(signalId.getId());
        assertThat(updated.getVersion()).isEqualTo(signalId.getVersion());
        assertThat(updated.getName()).isEqualTo(name);
    }

    @Test
    void update_notFound() {
        var created = unstagedSignal(planId).setSignalId(VersionedId.of(getRandomLong(), MIN_VERSION));
        created.setRevision(1);

        requestSpecWithBody(created)
                .when().put(url + '/' + created.getId() + "/version/" + created.getVersion())
                .then().statusCode(HttpStatus.EXPECTATION_FAILED.value());
    }

    @Test
    void getById() {
        var signalId = signalService.create(signal).getSignalId();

        var persisted = requestSpec()
                .queryParam(WITH_ASSOCIATIONS, true)
                .when().get(url + '/' + signalId.getId() + "/version/" + signalId.getVersion())
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", UnstagedSignal.class);

        assertThat(persisted.getId()).isEqualTo(signalId.getId());
        assertThat(persisted.getVersion()).isEqualTo(signalId.getVersion());
    }

    @Test
    void getFields() {
        var signalId = signalService.create(signal).getSignalId();

        var field1 = unstagedField(signalId);
        var field2 = unstagedField(signalId);
        fieldService.create(field1, Set.of());
        fieldService.create(field2, Set.of());

        var fields = requestSpec()
                .when().get(url + '/' + signalId.getId() + "/version/" + signalId.getVersion() + "/fields")
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
                .when().get(url + '/' + signalId.getId() + "/version/" + signalId.getVersion() + "/field-groups")
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", FieldGroup.class);

        assertThat(fields).hasSize(2);
    }

    @Test
    void getEvents() {
        var signalId = signalService.create(signal).getSignalId();

        var events = requestSpec()
                .when().get(url + '/' + signalId.getId() + "/version/" + signalId.getVersion() + "/events")
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", UnstagedEvent.class);

        assertThat(events).isEmpty();
    }

    @Test
    void getAuditLog() throws JsonProcessingException {
        var created = signalService.create(signal);

        var json = requestSpec()
                .queryParam(MODE, BASIC)
                .when().get(url + '/' + created.getId() + "/version/" + created.getVersion() + "/auditLog")
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().asString();

        var auditLog = objectMapper.readValue(json, new TypeReference<List<AuditRecord<UnstagedSignalHistory>>>() {
        });

        assertThat(auditLog).hasSize(1);
    }

    @Test
    void getAuditLog_notFound_417() {
        requestSpec()
                .queryParam(MODE, BASIC)
                .when().get(url + '/' + getRandomLong() + "/version/1/auditLog")
                .then().statusCode(EXPECTATION_FAILED.value())
                .and().contentType(APPLICATION_JSON_VALUE);
    }
}