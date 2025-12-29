package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.common.model.CreateTemplateQuestionRequest;
import com.ebay.behavior.gds.mdm.signal.common.model.TemplateQuestion;
import com.ebay.behavior.gds.mdm.signal.service.EventTemplateService;
import com.ebay.behavior.gds.mdm.signal.service.TemplateQuestionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.eventTemplate;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.templateQuestion;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.TEMPLATE;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.WITH_ASSOCIATIONS;
import static org.assertj.core.api.Assertions.assertThat;

class TemplateQuestionResourceIT extends AbstractResourceTest {

    @Autowired
    protected TemplateQuestionService questionService;

    @Autowired
    private EventTemplateService eventService;

    private TemplateQuestion question;
    private Long eventId;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + TEMPLATE + "/question";

        var event = eventTemplate();
        event = eventService.create(event);
        eventId = event.getId();
        question = templateQuestion();
    }

    @Test
    void getById_notFound() {
        requestSpec()
                .when().get(url + '/' + getRandomLong())
                .then().statusCode(HttpStatus.EXPECTATION_FAILED.value());
    }

    @Test
    void getById_withAssociations() {
        var id = questionService.create(question, Set.of(eventId)).getId();

        var persisted = requestSpec()
                .queryParam(WITH_ASSOCIATIONS, true)
                .when().get(url + '/' + id)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", TemplateQuestion.class);

        assertThat(persisted.getId()).isEqualTo(id);
        assertThat(persisted.getEvents()).isNotEmpty();
    }

    @Test
    void create() {
        var request = new CreateTemplateQuestionRequest(question, Set.of(eventId));
        var created = requestSpecWithBody(request)
                .when().post(url)
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getObject(".", TemplateQuestion.class);

        assertThat(created.getId()).isNotNull();
    }

    @Test
    void update() {
        var created = questionService.create(question, Set.of());
        var updated = created.setQuestion("updated");

        var persisted = requestSpecWithBody(updated)
                .when().put(url + '/' + created.getId())
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", TemplateQuestion.class);

        assertThat(persisted.getQuestion()).isEqualTo("updated");
    }

    @Test
    void delete() {
        var created = questionService.create(question, Set.of());

        requestSpec().when().delete(url + '/' + created.getId())
                .then().statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void associate() {
        var created = questionService.create(question, Set.of());

        var persisted = requestSpec()
                .when().put(url + '/' + created.getId() + "/event/" + eventId)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", TemplateQuestion.class);

        assertThat(persisted.getId()).isEqualTo(created.getId());
    }
}
