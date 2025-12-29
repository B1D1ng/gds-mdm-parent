package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstageRequest;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.model.UpdateUnstagedEventRequest;
import com.ebay.behavior.gds.mdm.signal.service.AttributeTemplateService;
import com.ebay.behavior.gds.mdm.signal.service.EventTemplateService;
import com.ebay.behavior.gds.mdm.signal.service.FieldTemplateService;
import com.ebay.behavior.gds.mdm.signal.service.PlanService;
import com.ebay.behavior.gds.mdm.signal.service.SignalTemplateService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedEventService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedSignalService;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;
import com.ebay.behavior.gds.mdm.commonTestUtil.TestRequestContextUtils;
import com.ebay.behavior.gds.mdm.signal.usecase.UnstageUsecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.CompletionStatus.COMPLETED;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestAuthFilter.IT_TEST_USER;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.attributeTemplate;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.eventTemplate;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.fieldTemplate;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.signalTemplate;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.DEFINITION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;

class UnstageResourceIT extends AbstractResourceTest {

    @Autowired
    private PlanService planService;

    @Autowired
    private UnstageUsecase usecase;

    @Autowired
    private UnstagedSignalService unstagedSignalService;

    @Autowired
    private UnstagedEventService unstagedEventService;

    @Autowired
    private SignalTemplateService signalTemplateService;

    @Autowired
    private FieldTemplateService fieldTemplateService;

    @Autowired
    private EventTemplateService eventTemplateService;

    @Autowired
    private AttributeTemplateService attributeTemplateService;

    private Long planId;
    private Long signalTemplateId;
    private Long eventTemplateId;
    private final String name = getRandomSmallString();
    private final String desc = getRandomSmallString();
    private String url;

    @BeforeEach
    void setUp() {
        var plan = TestModelUtils.plan();
        planId = planService.create(plan).getId();
        var eventTemplate = eventTemplate();
        eventTemplate = eventTemplateService.create(eventTemplate);
        eventTemplateId = eventTemplate.getId();

        var attributeTemplate = attributeTemplate(eventTemplateId);
        attributeTemplate = attributeTemplateService.create(attributeTemplate);
        var attributeTemplateId = attributeTemplate.getId();

        var signalTemplate = signalTemplate().setCompletionStatus(COMPLETED);
        signalTemplateId = signalTemplateService.create(signalTemplate).getId();

        var fieldTemplate = fieldTemplate(signalTemplateId);
        fieldTemplateService.create(fieldTemplate, Set.of(attributeTemplateId), null);

        TestRequestContextUtils.setUser(IT_TEST_USER);
        url = getBaseUrl() + V1 + DEFINITION;
    }

    @Test
    void signal_createFromTemplate() {
        var request = UnstageRequest.ofTemplate(planId, signalTemplateId, name, desc, null);

        requestSpecWithBody(request)
                .when().post(url + "/signal/from-template/" + signalTemplateId)
                .then().statusCode(CREATED.value())
                .extract().body().jsonPath().getObject(".", UnstagedSignal.class);
    }

    @Test
    void event_createFromTemplate() {
        var signalRequest = UnstageRequest.ofTemplate(planId, signalTemplateId, name, desc, null);

        var signal = requestSpecWithBody(signalRequest)
                .when().post(url + "/signal/from-template/" + signalTemplateId)
                .then().statusCode(CREATED.value())
                .extract().body().jsonPath().getObject(".", UnstagedSignal.class);

        var signalId = signal.getSignalId();
        var eventName = getRandomSmallString();
        var eventDesc = getRandomSmallString();
        var eventRequest = UnstageRequest.ofTemplate(signalId.getId(), eventTemplateId, eventName, eventDesc, null);

        var created = requestSpecWithBody(eventRequest)
                .when().post(url + "/event/from-template/" + eventTemplateId)
                .then().statusCode(CREATED.value())
                .extract().body().jsonPath().getObject(".", UnstagedEvent.class);
        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo(eventName);
        assertThat(created.getDescription()).isEqualTo(eventDesc);

        var updatedSignal = unstagedSignalService.getByIdWithAssociationsRecursive(signalId);
        assertThat(updatedSignal.getEvents().size()).isEqualTo(2);

        var matches = updatedSignal.getEvents().stream().filter(event -> event.getName().equals(eventName)).toList();
        assertThat(matches.size()).isEqualTo(1);
        var matched = matches.get(0);
        assertThat(matched.getId()).isEqualTo(created.getId());
    }

    @Test
    void signal_createFromSignal() {
        var srcSignalId = createUnstagedSetup(planId, signalTemplateId);
        var request = UnstageRequest.ofUnstaged(planId, srcSignalId.getId(), srcSignalId.getVersion(), name, desc, null);

        requestSpecWithBody(request)
                .when().post(url + "/signal/from-signal/" + srcSignalId.getId() + "/version/" + srcSignalId.getVersion())
                .then().statusCode(CREATED.value())
                .extract().body().jsonPath().getObject(".", UnstagedSignal.class);
    }

    private VersionedId createUnstagedSetup(long planId, long signalTemplateId) {
        // create a signal setup by copy the template setup
        var params = UnstageRequest.ofTemplate(planId, signalTemplateId, name, desc, null);
        var srcSignal = usecase.copySignalFromTemplate(params);
        var srcSignalId = srcSignal.getSignalId();
        srcSignal = unstagedSignalService.getByIdWithAssociationsRecursive(srcSignalId);

        var srcEvent = unstagedSignalService.getEvents(srcSignalId).iterator().next();
        var updateEventRequest = new UpdateUnstagedEventRequest()
                .withId(srcEvent.getId())
                .withRevision(srcEvent.getRevision());
        unstagedEventService.update(updateEventRequest);

        return srcSignal.getSignalId();
    }
}
