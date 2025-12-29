package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion;
import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.common.model.AttributeTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.search.AttributeSearchBy;
import com.ebay.behavior.gds.mdm.signal.service.AttributeTemplateService;
import com.ebay.behavior.gds.mdm.signal.service.EventTemplateService;
import com.ebay.behavior.gds.mdm.commonTestUtil.PageHelper;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_BY;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_CRITERION;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_TERM;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.attributeTemplate;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.TEMPLATE;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.WITH_ASSOCIATIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

class AttributeTemplateResourceIT extends AbstractResourceTest {

    @Autowired
    protected AttributeTemplateService attributeService;

    @Autowired
    private EventTemplateService eventService;

    private long eventId;
    private AttributeTemplate attribute;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + TEMPLATE + "/attribute";

        var event = TestModelUtils.eventTemplate();
        event = eventService.create(event);
        eventId = event.getId();

        attribute = attributeTemplate(eventId);
    }

    @Test
    void getById_notFound() {
        requestSpec()
                .when().get(url + '/' + getRandomLong())
                .then().statusCode(HttpStatus.EXPECTATION_FAILED.value());
    }

    @Test
    void getById_withAssociations() {
        var id = attributeService.create(attribute).getId();

        var persisted = requestSpec()
                .queryParam(WITH_ASSOCIATIONS, true)
                .when().get(url + '/' + id)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", AttributeTemplate.class);

        assertThat(persisted.getId()).isEqualTo(id);
        assertThat(persisted.getEventTemplateId()).isEqualTo(eventId);
        assertThat(persisted.getEvent()).isNull(); // since excluded by @JsonBackReference
    }

    @Test
    void create() {
        var created = requestSpecWithBody(attribute)
                .when().post(url)
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getObject(".", AttributeTemplate.class);

        assertThat(created.getId()).isNotNull();
    }

    @Test
    void create_badEventId_error() {
        attribute.setEventTemplateId(getRandomLong());

        requestSpecWithBody(attribute)
                .when().post(url)
                .then().statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void delete() {
        var created = attributeService.create(attribute);

        requestSpec().when().delete(url + '/' + created.getId())
                .then().statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void getAll() throws JsonProcessingException {
        var prefix = getRandomSmallString();
        var attribute1 = attributeTemplate(eventId).setDescription(prefix + "testAttribute1");
        var attribute2 = attributeTemplate(eventId).setDescription(prefix + "attribute2");
        attributeService.create(attribute1);
        attributeService.create(attribute2);

        var json = requestSpec()
                .queryParam(SEARCH_TERM, prefix)
                .queryParam(SEARCH_BY, AttributeSearchBy.DESCRIPTION.name())
                .queryParam(SEARCH_CRITERION, SearchCriterion.CONTAINS)
                .queryParam(Search.PAGE_NUMBER, 0)
                .queryParam(Search.PAGE_SIZE, 10)
                .when().get(url)
                .then().statusCode(OK.value())
                .extract().body().asString();

        var page = objectMapper.readValue(json, new TypeReference<PageHelper<AttributeTemplate>>() {
        });

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().size()).isEqualTo(2);
        assertThat(page.getContent().get(0).getDescription()).startsWith(prefix);
    }
}