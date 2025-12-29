package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.common.model.Plan;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.model.audit.PlanHistory;
import com.ebay.behavior.gds.mdm.signal.common.model.search.PlanSearchBy;
import com.ebay.behavior.gds.mdm.signal.service.PlanService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedSignalService;
import com.ebay.behavior.gds.mdm.commonTestUtil.PageHelper;
import com.ebay.behavior.gds.mdm.signal.util.AuditUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import static com.ebay.behavior.gds.mdm.common.model.audit.AuditLogParams.MODE;
import static com.ebay.behavior.gds.mdm.common.model.audit.AuditMode.BASIC;
import static com.ebay.behavior.gds.mdm.common.model.audit.AuditMode.FULL;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.DOMAIN;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.OWNED_BY_ME;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_BY;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_TERM;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.plan;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedSignal;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomString;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.DEFINITION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.EXPECTATION_FAILED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class PlanResourceIT extends AbstractResourceTest {

    @Autowired
    private PlanService planService;

    @Autowired
    private UnstagedSignalService signalService;

    private Plan plan;
    private long planId;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + DEFINITION + "/plan";
        plan = plan();
        plan = planService.create(plan);
        planId = plan.getId();
    }

    @Test
    void getById() {
        var persisted = requestSpec().when().get(url + '/' + planId)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", Plan.class);

        assertThat(persisted.getId()).isEqualTo(planId);
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
        var plan = plan();
        var created = requestSpecWithBody(plan)
                .when().post(url)
                .then().statusCode(CREATED.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", Plan.class);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getRevision()).isEqualTo(0);
        assertThat(created.getCreateBy()).isEqualTo(created.getUpdateBy());
        assertThat(created.getUpdateDate()).isEqualTo(created.getCreateDate());
    }

    @Test
    void update() {
        var updatedDesc = getRandomString();
        plan.setDescription(updatedDesc);

        var updated = requestSpecWithBody(plan)
                .when().put(url + String.format("/%d", planId))
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", Plan.class);

        assertThat(updated.getId()).isEqualTo(planId);
        assertThat(updated.getRevision()).isEqualTo(plan.getRevision() + 1);
    }

    @Test
    void update_notFound_417() {
        plan = plan().withId(getRandomLong()).withRevision(1);

        requestSpecWithBody(plan)
                .when().put(url + String.format("/%d", plan.getId()))
                .then().statusCode(EXPECTATION_FAILED.value())
                .and().contentType(APPLICATION_JSON_VALUE);
    }

    @Test
    void getAll_search() throws JsonProcessingException {
        // "not owned by me" scenario - should find since match by name
        var json = requestSpec()
                .queryParam(OWNED_BY_ME, false)
                .queryParam(SEARCH_BY, PlanSearchBy.PLAN.name())
                .queryParam(SEARCH_TERM, plan.getName()) // matches since PLAN search looking for name or description match
                .queryParam(DOMAIN, plan.getDomain())
                .when().get(url)
                .then().statusCode(HttpStatus.OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().asString();

        var page = objectMapper.readValue(json, new TypeReference<PageHelper<Plan>>() {
        });

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getTotalPages()).isEqualTo(1);
        assertThat(page.getContent().size()).isEqualTo(1);
        assertThat(page.getContent().get(0).getName()).isEqualTo(plan.getName());

        // not owned by me filter by different domain
        val domain = getRandomSmallString();
        json = requestSpec()
                .queryParam(OWNED_BY_ME, false)
                .queryParam(SEARCH_BY, PlanSearchBy.PLAN.name())
                .queryParam(SEARCH_TERM, plan.getName()) // matches since PLAN search looking for name or description match
                .queryParam(DOMAIN, domain)
                .when().get(url)
                .then().statusCode(HttpStatus.OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().asString();

        page = objectMapper.readValue(json, new TypeReference<>() {
        });
        assertThat(page.getTotalElements()).isEqualTo(0);
    }

    @Test
    void getAll() throws JsonProcessingException {
        var json = requestSpec()
                .when().get(url)
                .then().statusCode(HttpStatus.OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().asString();

        var page = objectMapper.readValue(json, new TypeReference<PageHelper<Plan>>() {
        });

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(page.getTotalPages()).isGreaterThanOrEqualTo(1);
        assertThat(page.getContent().size()).isGreaterThanOrEqualTo(1);
        assertThat(page.getContent()).extracting(Plan::getName).contains(plan.getName());
    }

    @Test
    void getSignals() {
        var signal = unstagedSignal(planId);
        var signalId = signalService.create(signal).getId();

        var signals = requestSpec()
                .when().get(url + '/' + planId + "/signals")
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", UnstagedSignal.class);

        assertThat(signals).hasSize(1);
        assertThat(signals.get(0).getId()).isEqualTo(signalId);
    }

    @Test
    void getAuditLog() {
        plan.setDescription("updated description");
        plan = planService.update(plan);
        plan.setName("updated name");
        plan = planService.update(plan);

        var json = requestSpec()
                .queryParam(MODE, FULL)
                .when().get(url + '/' + planId + "/auditLog")
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().asString();

        var auditRecords = AuditUtils.deserializeAuditRecords(json, objectMapper, PlanHistory.class);

        assertThat(auditRecords).hasSize(3);
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