package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.model.audit.AuditRecord;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion;
import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.common.model.EventTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.FieldTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.TemplateQuestion;
import com.ebay.behavior.gds.mdm.signal.common.model.audit.SignalTemplateHistory;
import com.ebay.behavior.gds.mdm.signal.common.model.search.SignalSearchBy;
import com.ebay.behavior.gds.mdm.signal.service.AttributeTemplateService;
import com.ebay.behavior.gds.mdm.signal.service.EventTemplateService;
import com.ebay.behavior.gds.mdm.signal.service.FieldTemplateService;
import com.ebay.behavior.gds.mdm.signal.service.SignalTemplateService;
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

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.model.audit.AuditLogParams.MODE;
import static com.ebay.behavior.gds.mdm.common.model.audit.AuditMode.BASIC;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.PLATFORM;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_BY;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_CRITERION;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_TERM;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.CJS_PLATFORM_ID;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.attributeTemplate;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.fieldTemplate;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.signalTemplate;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomString;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.TEMPLATE;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.WITH_ASSOCIATIONS;
import static java.util.Locale.US;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.EXPECTATION_FAILED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class SignalTemplateResourceIT extends AbstractResourceTest {

    @Autowired
    private SignalTemplateService signalService;

    @Autowired
    private EventTemplateService eventService;

    @Autowired
    private AttributeTemplateService attributeService;

    @Autowired
    private FieldTemplateService fieldService;

    private SignalTemplate signal;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + TEMPLATE + "/signal";
        signal = signalTemplate();
    }

    @Test
    void create() {
        var created = requestSpecWithBody(signal)
                .when().post(url)
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getObject(".", SignalTemplate.class);

        assertThat(created.getId()).isNotNull();
    }

    @Test
    void update() {
        var created = signalService.create(signal);

        created.setName(getRandomString());
        var updated = requestSpecWithBody(created)
                .when().put(url + '/' + created.getId())
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", SignalTemplate.class);

        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getName()).isEqualTo(created.getName());
    }

    @Test
    void update_notFound() {
        signal = signal.withId(getRandomLong()).withRevision(0);

        requestSpecWithBody(signal)
                .when().put(url + '/' + signal.getId())
                .then().statusCode(HttpStatus.EXPECTATION_FAILED.value());
    }

    @Test
    void delete() {
        var created = signalService.create(signal);

        requestSpec().when().delete(url + '/' + created.getId())
                .then().statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void getAll() throws JsonProcessingException {
        var prefix = getRandomSmallString().toLowerCase(US);
        var signal1 = signalTemplate().setName(prefix + "testSignal1");
        var signal2 = signalTemplate().setName(prefix + "signal2");
        signalService.create(signal1);
        signalService.create(signal2);

        var json = requestSpec()
                .queryParam(SEARCH_TERM, prefix)
                .queryParam(SEARCH_BY, SignalSearchBy.NAME.name())
                .queryParam(SEARCH_CRITERION, SearchCriterion.CONTAINS)
                .queryParam(Search.PAGE_NUMBER, 0)
                .queryParam(Search.PAGE_SIZE, 10)
                .when().get(url)
                .then().statusCode(OK.value())
                .extract().body().asString();

        var page = objectMapper.readValue(json, new TypeReference<PageHelper<SignalTemplate>>() {
        });

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().size()).isEqualTo(2);
        assertThat(page.getContent().get(0).getName()).startsWith(prefix);
    }

    @Test
    void getById_withAssociations() {
        // Given
        var signalId = requestSpecWithBody(signal)
                .when().post(url)
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getLong(ID);

        var event = TestModelUtils.eventTemplate();
        var eventId = eventService.create(event).getId();

        var attribute = attributeTemplate(eventId);
        var attributeId = attributeService.create(attribute).getId();

        var field = fieldTemplate(signalId);
        fieldService.create(field, Set.of(attributeId), null);

        // When
        var persisted = requestSpec()
                .queryParam(WITH_ASSOCIATIONS, true)
                .when().get(url + '/' + signalId)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", SignalTemplate.class);

        // Then
        assertThat(persisted.getId()).isEqualTo(signalId);
        assertThat(persisted.getEvents().size()).isEqualTo(1);
        assertThat(persisted.getFields().size()).isEqualTo(1);
    }

    @Test
    void getById_notFound() {
        requestSpec()
                .when().get(url + '/' + getRandomLong())
                .then().statusCode(HttpStatus.EXPECTATION_FAILED.value());
    }

    @Test
    void getFields() {
        var signalId = signalService.create(signal).getId();

        var field1 = fieldTemplate(signalId);
        var field2 = fieldTemplate(signalId);
        fieldService.create(field1, Set.of(), null);
        fieldService.create(field2, Set.of(), null);

        var fields = requestSpec()
                .when().get(url + '/' + signalId + "/fields")
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", FieldTemplate.class);

        assertThat(fields).hasSize(2);
    }

    @Test
    void getEvents() {
        var signalId = signalService.create(signal).getId();

        var events = requestSpec()
                .when().get(url + '/' + signalId + "/events")
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", EventTemplate.class);

        assertThat(events).isEmpty();
    }

    @Test
    void getTypes() {
        signalService.create(signal);
        var types = requestSpec()
                .queryParam(PLATFORM, String.valueOf(CJS_PLATFORM_ID))
                .when().get(url + "/types")
                .then().log().all().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", String.class);
        assertThat(types).isNotEmpty();
        assertThat(types).contains(signal.getType());
    }

    @Test
    void getQuestions() {
        var signalId = signalService.create(signal).getId();

        var questions = requestSpec()
                .when().get(url + '/' + signalId + "/questions")
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", TemplateQuestion.class);

        assertThat(questions).isEmpty();
    }

    @Test
    void getAuditLog() throws JsonProcessingException {
        var created = requestSpecWithBody(signal)
                .when().post(url)
                .then().statusCode(CREATED.value())
                .extract().body().jsonPath().getObject(".", SignalTemplate.class);

        var json = requestSpec()
                .queryParam(MODE, BASIC)
                .when().get(url + '/' + created.getId() + "/auditLog")
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().asString();

        var auditLog = objectMapper.readValue(json, new TypeReference<List<AuditRecord<SignalTemplateHistory>>>() {
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
