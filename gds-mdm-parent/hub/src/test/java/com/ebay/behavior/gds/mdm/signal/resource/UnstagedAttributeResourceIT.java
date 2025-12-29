package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion;
import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedAttribute;
import com.ebay.behavior.gds.mdm.signal.common.model.UpdateUnstagedAttributeRequest;
import com.ebay.behavior.gds.mdm.signal.common.model.search.AttributeSearchBy;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedAttributeService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedEventService;
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
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedAttribute;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomString;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.DEFINITION;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.WITH_ASSOCIATIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

class UnstagedAttributeResourceIT extends AbstractResourceTest {

    @Autowired
    protected UnstagedAttributeService attributeService;
    @Autowired
    private UnstagedEventService eventService;
    private long eventId;
    private UnstagedAttribute attribute;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + DEFINITION + "/attribute";

        var event = TestModelUtils.unstagedEvent();
        event = eventService.create(event);
        eventId = event.getId();

        attribute = unstagedAttribute(eventId);
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
                .extract().body().jsonPath().getObject(".", UnstagedAttribute.class);

        assertThat(persisted.getId()).isEqualTo(id);
        assertThat(persisted.getEventId()).isEqualTo(eventId);
        assertThat(persisted.getEvent()).isNull(); // since excluded by @JsonBackReference
    }

    @Test
    void create() {
        var created = requestSpecWithBody(attribute)
                .when().post(url)
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getObject(".", UnstagedAttribute.class);

        assertThat(created.getId()).isNotNull();
    }

    @Test
    void create_badEventId_error() {
        attribute.setEventId(getRandomLong());

        requestSpecWithBody(attribute)
                .when().post(url)
                .then().statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void update() {
        var desc = getRandomString();
        var created = attributeService.create(attribute);
        var eventId = created.getId();
        var request = UpdateUnstagedAttributeRequest.builder().id(eventId).description(desc).build();

        var updated = requestSpecWithBody(request)
                .when().patch(url + '/' + eventId)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", UnstagedAttribute.class);

        assertThat(updated.getId()).isEqualTo(eventId);
        assertThat(updated.getDescription()).isEqualTo(desc);
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
        var attribute1 = unstagedAttribute(eventId).toBuilder().description(prefix + "testAttribute1").build();
        var attribute2 = unstagedAttribute(eventId).toBuilder().description(prefix + "attribute2").build();
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

        var page = objectMapper.readValue(json, new TypeReference<PageHelper<UnstagedAttribute>>() {
        });

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().size()).isEqualTo(2);
        assertThat(page.getContent().get(0).getDescription()).startsWith(prefix);
    }
}