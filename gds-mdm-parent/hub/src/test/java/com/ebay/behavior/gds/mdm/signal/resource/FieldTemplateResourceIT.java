package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion;
import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.common.model.AttributeTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.CreateFieldTemplateRequest;
import com.ebay.behavior.gds.mdm.signal.common.model.FieldTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.search.FieldSearchBy;
import com.ebay.behavior.gds.mdm.signal.service.AttributeTemplateService;
import com.ebay.behavior.gds.mdm.signal.service.EventTemplateService;
import com.ebay.behavior.gds.mdm.signal.service.SignalTemplateService;
import com.ebay.behavior.gds.mdm.commonTestUtil.PageHelper;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_BY;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_CRITERION;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_TERM;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.attributeTemplate;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.fieldTemplate;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.TEMPLATE;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.WITH_ASSOCIATIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

class FieldTemplateResourceIT extends AbstractResourceTest {

    @Autowired
    private SignalTemplateService signalService;

    @Autowired
    private AttributeTemplateService attributeService;

    @Autowired
    private EventTemplateService eventService;

    private long signalId;
    private long attributeId;
    private CreateFieldTemplateRequest createRequest;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + TEMPLATE + "/field";

        var event = TestModelUtils.eventTemplate();
        var eventId = eventService.create(event).getId();

        var attribute = attributeTemplate(eventId);
        attributeId = attributeService.create(attribute).getId();

        var signal = TestModelUtils.signalTemplate();
        signalId = signalService.create(signal).getId();

        var field = fieldTemplate(signalId);
        createRequest = new CreateFieldTemplateRequest(field, Set.of(attributeId), null);
    }

    @Test
    void getAttributes() {
        var id = requestSpecWithBody(createRequest)
                .when().post(url)
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getLong(ID);

        var attributes = requestSpec()
                .when().get(url + '/' + id + "/attributes")
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", AttributeTemplate.class);

        assertThat(attributes).hasSize(1);
        assertThat(attributes).extracting(AttributeTemplate::getId).contains(attributeId);
    }

    @Test
    void getById_withAssociations() {
        var id = requestSpecWithBody(createRequest)
                .when().post(url)
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getLong(ID);

        var persisted = requestSpec()
                .queryParam(WITH_ASSOCIATIONS, true)
                .when().get(url + '/' + id)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", FieldTemplate.class);

        assertThat(persisted.getId()).isEqualTo(id);
        assertThat(persisted.getSignal()).isNull(); // since excluded by @JsonBackReference
        assertThat(persisted.getSignalTemplateId()).isEqualTo(signalId);
        assertThat(persisted.getAttributes().size()).isEqualTo(1);
    }

    @Test
    void getById_notFound() {
        requestSpec()
                .when().get(url + '/' + getRandomLong())
                .then().statusCode(HttpStatus.EXPECTATION_FAILED.value());
    }

    @Test
    void create() {
        var created = requestSpecWithBody(createRequest)
                .when().post(url)
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getObject(".", FieldTemplate.class);

        assertThat(created.getId()).isNotNull();
    }

    @Test
    void delete() {
        var created = requestSpecWithBody(createRequest)
                .when().post(url)
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getObject(".", FieldTemplate.class);

        requestSpec().when().delete(url + '/' + created.getId())
                .then().statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void getAll() throws JsonProcessingException {
        var prefix = getRandomSmallString();
        var field1 = fieldTemplate(signalId).setName(prefix + "testField1");
        var field2 = fieldTemplate(signalId).setName(prefix + "field2");
        var createRequest1 = new CreateFieldTemplateRequest(field1, null, null);
        var createRequest2 = new CreateFieldTemplateRequest(field2, null, Set.of());
        requestSpecWithBody(createRequest1).when().post(url).then().statusCode(CREATED.value());
        requestSpecWithBody(createRequest2).when().post(url).then().statusCode(CREATED.value());

        var json = requestSpec()
                .queryParam(SEARCH_TERM, prefix)
                .queryParam(SEARCH_BY, FieldSearchBy.NAME.name())
                .queryParam(SEARCH_CRITERION, SearchCriterion.CONTAINS)
                .queryParam(Search.PAGE_NUMBER, 0)
                .queryParam(Search.PAGE_SIZE, 10)
                .when().get(url)
                .then().statusCode(OK.value())
                .extract().body().asString();

        var page = objectMapper.readValue(json, new TypeReference<PageHelper<FieldTemplate>>() {
        });

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().size()).isEqualTo(2);
        assertThat(page.getContent().get(0).getName()).startsWith(prefix);
    }
}
