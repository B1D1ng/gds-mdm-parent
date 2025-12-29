package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.model.audit.AuditRecord;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion;
import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.common.model.AttributeTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.EvaluateEventExpressionRequest;
import com.ebay.behavior.gds.mdm.signal.common.model.EvaluateEventExpressionResponse;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.UpdateUnstagedEventRequest;
import com.ebay.behavior.gds.mdm.signal.common.model.audit.UnstagedEventHistory;
import com.ebay.behavior.gds.mdm.signal.common.model.search.EventSearchBy;
import com.ebay.behavior.gds.mdm.signal.service.PlanService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedAttributeService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedEventService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedFieldService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedSignalService;
import com.ebay.behavior.gds.mdm.commonTestUtil.PageHelper;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.ExpressionType.JEXL;
import static com.ebay.behavior.gds.mdm.common.model.audit.AuditLogParams.MODE;
import static com.ebay.behavior.gds.mdm.common.model.audit.AuditMode.BASIC;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_BY;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_CRITERION;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_TERM;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedAttribute;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedEvent;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedField;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomString;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.DEFINITION;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.WITH_ASSOCIATIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.EXPECTATION_FAILED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class UnstagedEventResourceIT extends AbstractResourceTest {

    private UnstagedEvent event;

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

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + DEFINITION + "/event";
        event = unstagedEvent();
    }

    @Test
    void update() {
        var name = getRandomString();
        var created = eventService.create(event);
        var eventId = created.getId();
        var request = UpdateUnstagedEventRequest.builder().id(eventId).name(name).build();

        var updated = requestSpecWithBody(request)
                .when().patch(url + '/' + eventId)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", UnstagedEvent.class);

        assertThat(updated.getId()).isEqualTo(eventId);
        assertThat(updated.getName()).isEqualTo(name);
    }

    @Test
    void update_notFound() {
        var request = UpdateUnstagedEventRequest.builder().id(getRandomLong()).build();

        requestSpecWithBody(request)
                .when().patch(url + '/' + request.getId())
                .then().statusCode(HttpStatus.EXPECTATION_FAILED.value());
    }

    @Test
    void delete() {
        var eventId = eventService.create(event).getId();

        requestSpec().when().delete(url + '/' + eventId)
                .then().statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void getById() {
        var eventId = eventService.create(event).getId();

        var attribute = unstagedAttribute(eventId);
        attributeService.create(attribute);

        var persisted = requestSpec()
                .queryParam(WITH_ASSOCIATIONS, true)
                .when().get(url + '/' + eventId)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", UnstagedEvent.class);

        assertThat(persisted.getId()).isEqualTo(eventId);
    }

    @Test
    void getById_notFound() {
        requestSpec()
                .when().get(url + '/' + getRandomLong())
                .then().statusCode(HttpStatus.EXPECTATION_FAILED.value());
    }

    @Test
    void getAll() throws JsonProcessingException {
        var prefix = getRandomSmallString();
        var event1 = unstagedEvent().toBuilder().name(prefix + "testEvent1").build();
        var event2 = unstagedEvent().toBuilder().name(prefix + "event2").build();
        eventService.create(event1);
        eventService.create(event2);

        var json = requestSpec()
                .queryParam(SEARCH_TERM, prefix)
                .queryParam(SEARCH_BY, EventSearchBy.NAME.name())
                .queryParam(SEARCH_CRITERION, SearchCriterion.CONTAINS)
                .queryParam(Search.PAGE_NUMBER, 0)
                .queryParam(Search.PAGE_SIZE, 10)
                .when().get(url)
                .then().statusCode(OK.value())
                .extract().body().asString();

        var page = objectMapper.readValue(json, new TypeReference<PageHelper<UnstagedEvent>>() {
        });

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().size()).isEqualTo(2);
        assertThat(page.getContent().get(0).getName()).startsWith(prefix);
    }

    @Test
    void getAttributes() {
        var eventId = eventService.create(event).getId();

        var attribute1 = unstagedAttribute(eventId);
        var attribute2 = unstagedAttribute(eventId);
        attributeService.create(attribute1);
        attributeService.create(attribute2);

        var attributes = requestSpec()
                .when().get(url + '/' + eventId + "/attributes")
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", AttributeTemplate.class);

        assertThat(attributes).hasSize(2);
    }

    @Test
    void evaluateExpressionUpdate() {
        var created = eventService.create(event);
        var eventId = created.getId();

        var plan = TestModelUtils.plan();
        var planId = planService.create(plan).getId();
        var signal = TestModelUtils.unstagedSignal(planId);
        var signalId = signalService.create(signal).getSignalId();
        var attribute = unstagedAttribute(eventId);
        var attributeId = attributeService.create(attribute).getId();
        var field = unstagedField(signalId);
        fieldService.create(field, Set.of(attributeId));

        var expression = "[456].contains(event.context.pageInteractionContext.moduleId)";
        var request = new EvaluateEventExpressionRequest(expression, JEXL);

        var response = requestSpecWithBody(request)
                .when().put(url + '/' + eventId + "/business-fields")
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", EvaluateEventExpressionResponse.class);

        assertThat(response.currentFields()).isNotEmpty();
        assertThat(response.nextFields()).isEmpty();
    }

    @Test
    void getAuditLog() throws JsonProcessingException {
        var created = eventService.create(event);

        var json = requestSpec()
                .queryParam(MODE, BASIC)
                .when().get(url + '/' + created.getId() + "/auditLog")
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().asString();

        var auditLog = objectMapper.readValue(json, new TypeReference<List<AuditRecord<UnstagedEventHistory>>>() {
        });

        assertThat(auditLog).hasSize(1);
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