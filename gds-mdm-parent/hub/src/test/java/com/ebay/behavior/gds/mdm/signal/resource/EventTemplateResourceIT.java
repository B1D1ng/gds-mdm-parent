package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.model.audit.AuditRecord;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion;
import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.common.model.AttributeTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.EventTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.TemplateQuestion;
import com.ebay.behavior.gds.mdm.signal.common.model.audit.EventTemplateHistory;
import com.ebay.behavior.gds.mdm.signal.common.model.search.EventSearchBy;
import com.ebay.behavior.gds.mdm.signal.service.AttributeTemplateService;
import com.ebay.behavior.gds.mdm.signal.service.EventTemplateService;
import com.ebay.behavior.gds.mdm.signal.service.TemplateQuestionService;
import com.ebay.behavior.gds.mdm.commonTestUtil.PageHelper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.audit.AuditLogParams.MODE;
import static com.ebay.behavior.gds.mdm.common.model.audit.AuditMode.BASIC;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_BY;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_CRITERION;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_TERM;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.attributeTemplate;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.eventTemplate;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.templateQuestion;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomString;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.TEMPLATE;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.WITH_ASSOCIATIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.EXPECTATION_FAILED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class EventTemplateResourceIT extends AbstractResourceTest {

    private EventTemplate event;

    @Autowired
    private AttributeTemplateService attributeService;

    @Autowired
    private EventTemplateService eventService;

    @Autowired
    private TemplateQuestionService questionService;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + TEMPLATE + "/event";
        event = eventTemplate();
    }

    @Test
    void create() {
        var created = requestSpecWithBody(event)
                .when().post(url)
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getObject(".", EventTemplate.class);

        assertThat(created.getId()).isNotNull();
    }

    @Test
    void update() {
        var created = eventService.create(event);

        created.setName(getRandomString());
        var updated = requestSpecWithBody(created)
                .when().put(url + '/' + created.getId())
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", EventTemplate.class);

        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getName()).isEqualTo(created.getName());
    }

    @Test
    void update_notFound() {
        event = event.withId(getRandomLong()).withRevision(0);

        requestSpecWithBody(event)
                .when().put(url + '/' + event.getId())
                .then().statusCode(HttpStatus.EXPECTATION_FAILED.value());
    }

    @Test
    void delete() {
        var created = eventService.create(event);

        requestSpec().when().delete(url + '/' + created.getId())
                .then().statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void getById() {
        var eventId = eventService.create(event).getId();

        var attribute = attributeTemplate(eventId);
        attributeService.create(attribute);

        var persisted = requestSpec()
                .queryParam(WITH_ASSOCIATIONS, true)
                .when().get(url + '/' + eventId)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", EventTemplate.class);

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
        var event1 = eventTemplate().setName(prefix + "testEvent1");
        var event2 = eventTemplate().setName(prefix + "event2");
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

        var page = objectMapper.readValue(json, new TypeReference<PageHelper<EventTemplate>>() {
        });

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().size()).isEqualTo(2);
        assertThat(page.getContent().get(0).getName()).startsWith(prefix);
    }

    @Test
    void getAttributes() {
        var eventId = eventService.create(event).getId();

        var attribute1 = attributeTemplate(eventId);
        var attribute2 = attributeTemplate(eventId);
        attributeService.create(attribute1);
        attributeService.create(attribute2);

        var attributes = requestSpec()
                .when().get(url + '/' + eventId + "/attributes")
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", AttributeTemplate.class);

        assertThat(attributes).hasSize(2);
    }

    @Test
    void getQuestions() {
        var eventId = eventService.create(event).getId();

        var question1 = templateQuestion();
        var question2 = templateQuestion();
        questionService.create(question1, Set.of(eventId));
        questionService.create(question2, Set.of(eventId));

        var questions = requestSpec()
                .when().get(url + '/' + eventId + "/questions")
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", TemplateQuestion.class);

        assertThat(questions).hasSize(2);
    }

    @Test
    void getAuditLog() throws JsonProcessingException {
        var created = requestSpecWithBody(event)
                .when().post(url)
                .then().statusCode(CREATED.value())
                .extract().body().jsonPath().getObject(".", EventTemplate.class);

        var json = requestSpec()
                .queryParam(MODE, BASIC)
                .when().get(url + '/' + created.getId() + "/auditLog")
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().asString();

        var auditLog = objectMapper.readValue(json, new TypeReference<List<AuditRecord<EventTemplateHistory>>>() {
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