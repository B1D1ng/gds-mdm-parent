package com.ebay.behavior.gds.mdm.signal.usecase;

import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.model.JavaType;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.signal.common.model.Attribute;
import com.ebay.behavior.gds.mdm.signal.common.model.AttributeTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.Event;
import com.ebay.behavior.gds.mdm.signal.common.model.EventTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.Field;
import com.ebay.behavior.gds.mdm.signal.common.model.FieldTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.Plan;
import com.ebay.behavior.gds.mdm.signal.common.model.Signal;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.SojBusinessTag;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstageRequest;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedAttribute;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.model.manyToMany.SojEventTagMapping;
import com.ebay.behavior.gds.mdm.signal.repository.SojBusinessTagRepository;
import com.ebay.behavior.gds.mdm.signal.repository.SojEventRepository;
import com.ebay.behavior.gds.mdm.signal.repository.UnstagedEventRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.SojEventTagMappingRepository;
import com.ebay.behavior.gds.mdm.signal.service.AttributeTemplateService;
import com.ebay.behavior.gds.mdm.signal.service.EventTemplateService;
import com.ebay.behavior.gds.mdm.signal.service.FieldTemplateService;
import com.ebay.behavior.gds.mdm.signal.service.PlanService;
import com.ebay.behavior.gds.mdm.signal.service.SignalTemplateService;
import com.ebay.behavior.gds.mdm.signal.service.TemplateQuestionService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedEventService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedSignalService;
import com.ebay.behavior.gds.mdm.signal.service.userAnswer.UserAnswerMetadataSetter;
import com.ebay.behavior.gds.mdm.signal.service.userAnswer.UserAnswerSetter;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;
import com.ebay.behavior.gds.mdm.commonTestUtil.TestRequestContextUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.CompletionStatus.COMPLETED;
import static com.ebay.behavior.gds.mdm.common.model.ExpressionType.JEXL;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.common.model.AnswerPropertyPlaceholder.MODULE_IDS;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestAuthFilter.IT_TEST_USER;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.attributeTemplate;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.eventTemplate;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.fieldTemplate;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.signalTemplate;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.sojBusinessTag;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.sojEvent;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.templateQuestion;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UnstageUsecaseIT {

    @Autowired
    private UnstageUsecase usecase;

    @Autowired
    private PlanService planService;

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

    @Autowired
    private TemplateQuestionService templateQuestionService;

    @Autowired
    private UnstagedEventRepository eventRepository;

    @Autowired
    private SojEventRepository sojEventRepository;

    @Autowired
    private SojBusinessTagRepository sojBusinessTagRepository;

    @Autowired
    private SojEventTagMappingRepository sojTagMappingRepository;

    private final String name = getRandomSmallString();
    private final String desc = getRandomSmallString();
    private final String expression = "abc " + UserAnswerSetter.toPlaceholder(MODULE_IDS) + " def";
    private Plan plan;
    private long planId;
    private long signalTemplateId;
    private long eventTemplateId1;

    private SignalTemplate signalTemplate;
    private FieldTemplate fieldTemplate1;
    private EventTemplate eventTemplate1;
    private AttributeTemplate attributeTemplate1;
    private AttributeTemplate attributeTemplate2;
    private SojBusinessTag sojTag1;
    private SojBusinessTag sojTag2;
    private SojBusinessTag sojTag3;

    @BeforeEach
    void setUp() {
        TestRequestContextUtils.setUser(IT_TEST_USER);
        plan = TestModelUtils.plan();
        planId = planService.create(plan).getId();

        eventTemplate1 = eventTemplate().setName("name1").setExpression(expression).setExpressionType(JEXL);
        eventTemplate1 = eventTemplateService.create(eventTemplate1);
        eventTemplateId1 = eventTemplate1.getId();

        var eventTemplate2 = eventTemplate().setName("name2").setExpression(expression).setExpressionType(JEXL);
        eventTemplate2 = eventTemplateService.create(eventTemplate2);
        var eventTemplateId2 = eventTemplate2.getId();

        attributeTemplate1 = attributeTemplate(eventTemplateId1).setTag("attr1").setDescription("attr1");
        attributeTemplate1 = attributeTemplateService.create(attributeTemplate1);
        var attributeTemplateId1 = attributeTemplate1.getId();

        attributeTemplate2 = attributeTemplate(eventTemplateId1).setTag("attr2").setDescription("attr2");
        attributeTemplate2 = attributeTemplateService.create(attributeTemplate2);
        var attributeTemplateId2 = attributeTemplate2.getId();

        var attributeTemplate3 = attributeTemplate(eventTemplateId2).setTag("attr3").setDescription("attr3");
        attributeTemplate3 = attributeTemplateService.create(attributeTemplate3);
        var attributeTemplateId3 = attributeTemplate3.getId();

        signalTemplate = signalTemplate().setCompletionStatus(COMPLETED);
        signalTemplateId = signalTemplateService.create(signalTemplate).getId();
        signalTemplate = signalTemplateService.getById(signalTemplateId);

        fieldTemplate1 = fieldTemplate(signalTemplateId).setName("field1");
        fieldTemplate1 = fieldTemplateService.create(fieldTemplate1, Set.of(attributeTemplateId1, attributeTemplateId2, attributeTemplateId3), null);

        var fieldTemplate2 = fieldTemplate(signalTemplateId).setName("field2");
        fieldTemplateService.create(fieldTemplate2, Set.of(attributeTemplateId3), null);

        // All IDs always restarted from 1 in H2 db, so next line will increment ids, so we will be able to compare them later
        var request = UnstageRequest.ofTemplate(planId, signalTemplateId, name, desc, null);
        usecase.copySignalFromTemplate(request);
    }

    @Test
    void copySignalFromTemplate() {
        // given
        var question1 = templateQuestion()
                .setAnswerJavaType(JavaType.STRING)
                .setAnswerPropertyName("expression")
                .setIsList(false);
        var question2 = templateQuestion()
                .setAnswerJavaType(JavaType.LONG)
                .setAnswerPropertyName("moduleIds")
                .setIsList(true);

        templateQuestionService.create(question1, Set.of(eventTemplateId1));
        templateQuestionService.create(question2, Set.of(eventTemplateId1));

        var expression = "abc";
        var moduleIds = " 1, 2, 3 ";
        question1.setAnswer(expression);
        question2.setAnswer(moduleIds);
        var request = UnstageRequest.ofTemplate(planId, signalTemplateId, name, desc, Set.of(question1, question2));

        createSojEventAndTagSetup(List.of(1L, 2L, 3L));

        // when
        var dstSignal = usecase.copySignalFromTemplate(request);

        // then
        assertThat(dstSignal.getPlanId()).isEqualTo(planId);
        assertThat(dstSignal.getName()).isEqualTo(name);
        assertThat(dstSignal.getDescription()).isEqualTo(desc);
        assertThat(dstSignal.getOwners()).isEqualTo(plan.getOwners());
        assertThat(dstSignal.getDomain()).isEqualTo(plan.getDomain());
        assertThat(dstSignal.getSignalTemplateSourceId()).isEqualTo(signalTemplateId);
        assertThat(dstSignal.getSignalSourceId()).isNull();
        assertThat(dstSignal.getCompletionStatus()).isEqualTo(COMPLETED);
        assertThat(dstSignal.getEnvironment()).isEqualTo(Environment.UNSTAGED);
        assertThat(dstSignal.getCreateBy()).isEqualTo(IT_TEST_USER);
        assertThat(dstSignal.getUpdateBy()).isEqualTo(IT_TEST_USER);
        assertSignalEqualTo(dstSignal, signalTemplate);

        var dstEvents = unstagedSignalService.getEvents(dstSignal.getSignalId());

        assertThat(dstEvents.size()).isEqualTo(2);
        var dstEvent = dstEvents.stream()
                .filter(evt -> evt.getName().equals(eventTemplate1.getName()))
                .findFirst()
                .orElseThrow();
        assertThat(dstEvent.getId()).isNotEqualTo(eventTemplateId1);
        assertThat(dstEvent.getExpression()).isEqualTo(expression);
        assertThat(dstEvent.getExpressionType()).isEqualTo(JEXL);

        assertThat(dstEvent.getPageIds()).isEmpty();
        assertThat(dstEvent.getModuleIds()).hasSize(3).contains(1L, 2L, 3L);
        assertThat(dstEvent.getClickIds()).isEmpty();

        assertEventEqualTo(dstEvent, eventTemplate1);

        var dstAttributes = unstagedEventService.getAttributes(dstEvent.getId());
        dstAttributes.sort(Comparator.comparing(UnstagedAttribute::getTag));
        assertThat(dstAttributes.size()).isEqualTo(5);

        var dstEventId = dstEvent.getId();
        var dstAttribute1 = dstAttributes.get(0);
        assertThat(dstAttribute1.getEventId()).isEqualTo(dstEventId);
        assertPlatformAttributeEqualTo(dstAttribute1, attributeTemplate1);
        var dstAttribute2 = dstAttributes.get(1);
        assertThat(dstAttribute2.getEventId()).isEqualTo(dstEventId);
        assertPlatformAttributeEqualTo(dstAttribute2, attributeTemplate2);
        var dstAttribute3 = dstAttributes.get(2);
        assertBusinessAttributeEqualTo(dstAttribute3, sojTag1, dstEventId);
        var dstAttribute4 = dstAttributes.get(3);
        assertBusinessAttributeEqualTo(dstAttribute4, sojTag2, dstEventId);
        var dstAttribute5 = dstAttributes.get(4);
        assertBusinessAttributeEqualTo(dstAttribute5, sojTag3, dstEventId);

        // assert fields
        var dstFields = unstagedSignalService.getFields(dstSignal.getSignalId());
        var platformFields = dstFields.stream().filter(UnstagedField::getIsMandatory).toList();
        var businessFields = dstFields.stream().filter(field -> !field.getIsMandatory()).collect(toList());
        assertThat(platformFields.size()).isEqualTo(2);
        assertThat(businessFields.size()).isEqualTo(3);

        var dstSignalId = dstSignal.getSignalId().getId();
        var platformField = platformFields.stream()
                .filter(field -> field.getTag().equals(fieldTemplate1.getTag()))
                .findFirst()
                .orElseThrow();
        assertThat(platformField.getSignalId()).isEqualTo(dstSignalId);
        assertPlatformFieldEqualTo(platformField, fieldTemplate1);

        businessFields.sort(Comparator.comparing(UnstagedField::getTag));
        var businessField1 = businessFields.get(0);
        assertBusinessFieldEqualTo(businessField1, sojTag1, dstSignalId);
        var businessField2 = businessFields.get(1);
        assertBusinessFieldEqualTo(businessField2, sojTag2, dstSignalId);
        var businessField3 = businessFields.get(2);
        assertBusinessFieldEqualTo(businessField3, sojTag3, dstSignalId);

        // associations
        dstSignal = unstagedSignalService.getByIdWithAssociations(dstSignal.getSignalId());
        assertThat(dstSignal.getEvents().size()).isEqualTo(2);
        assertThat(dstSignal.getFields().size()).isEqualTo(5);
    }

    @Test
    void copyEventFromTemplate() {
        // given
        var question = templateQuestion()
                .setAnswerJavaType(JavaType.LONG)
                .setAnswerPropertyName("moduleIds")
                .setAnswerPropertyPlaceholder(MODULE_IDS)
                .setAnswerPropertySetterClass(UserAnswerMetadataSetter.class.getSimpleName())
                .setIsList(true);

        templateQuestionService.create(question, Set.of(eventTemplateId1));

        var moduleIds = "4, 5, 6";
        question.setAnswer(moduleIds);
        var request = UnstageRequest.ofTemplate(planId, signalTemplateId, name, desc, Set.of(question));

        // when
        // create signal from signal template
        var srcSignal = usecase.copySignalFromTemplate(request);
        var srcSignalId = srcSignal.getSignalId();

        var srcSignalBefore = unstagedSignalService.getByIdWithAssociations(srcSignalId);
        var eventsBefore = unstagedSignalService.getEvents(srcSignalId);
        assertThat(srcSignalBefore.getEvents().size()).isEqualTo(2);
        assertThat(eventsBefore.size()).isEqualTo(2);

        var eventName = "copy";
        var eventDesc = "copy";
        moduleIds = "7, 8, 9";
        question.setAnswer(moduleIds);

        createSojEventAndTagSetup(List.of(7L, 8L, 9L));

        // add new event
        var eventRequest = UnstageRequest.ofTemplate(srcSignalId.getId(), eventTemplateId1, eventName, eventDesc, Set.of(question));
        var dstEvent = usecase.copyEventFromTemplate(eventRequest);

        var srcSignalAfter = unstagedSignalService.getByIdWithAssociations(srcSignalId);
        var eventsAfter = unstagedSignalService.getEvents(srcSignalId);
        assertThat(srcSignalAfter.getEvents().size()).isEqualTo(3);
        assertThat(eventsAfter.size()).isEqualTo(3);

        // then
        // assert event
        assertThat(dstEvent.getId()).isNotEqualTo(eventTemplateId1);
        assertThat(dstEvent.getFsmOrder()).isEqualTo(eventTemplate1.getFsmOrder());
        assertThat(dstEvent.getCardinality()).isEqualTo(eventTemplate1.getCardinality());
        assertThat(dstEvent.getType()).isEqualTo(eventTemplate1.getType());
        assertThat(dstEvent.getSource()).isEqualTo(eventTemplate1.getSource());
        assertThat(dstEvent.getEventTemplateSourceId()).isEqualTo(eventTemplateId1);
        assertThat(dstEvent.getName()).isEqualTo(eventName);
        assertThat(dstEvent.getDescription()).isEqualTo(eventDesc);
        assertThat(dstEvent.getCreateBy()).isEqualTo(IT_TEST_USER);
        assertThat(dstEvent.getUpdateBy()).isEqualTo(IT_TEST_USER);
        assertThat(dstEvent.getExpression()).isEqualTo("abc 7, 8, 9 def");
        assertThat(dstEvent.getExpressionType()).isEqualTo(JEXL);

        assertThat(emptyIfNull(dstEvent.getPageIds())).isEmpty();
        assertThat(dstEvent.getModuleIds()).hasSize(3).contains(7L, 8L, 9L);
        assertThat(emptyIfNull(dstEvent.getClickIds())).isEmpty();

        // assert attributes
        var dstAttributes = unstagedEventService.getAttributes(dstEvent.getId());
        dstAttributes.sort(Comparator.comparing(UnstagedAttribute::getTag));
        assertThat(dstAttributes.size()).isEqualTo(5);

        var dstEventId = dstEvent.getId();
        var dstAttribute1 = dstAttributes.get(0);
        assertThat(dstAttribute1.getEventId()).isEqualTo(dstEventId);
        assertPlatformAttributeEqualTo(dstAttribute1, attributeTemplate1);
        var dstAttribute2 = dstAttributes.get(1);
        assertThat(dstAttribute2.getEventId()).isEqualTo(dstEventId);
        assertPlatformAttributeEqualTo(dstAttribute2, attributeTemplate2);
        var dstAttribute3 = dstAttributes.get(2);
        assertBusinessAttributeEqualTo(dstAttribute3, sojTag1, dstEventId);
        var dstAttribute4 = dstAttributes.get(3);
        assertBusinessAttributeEqualTo(dstAttribute4, sojTag2, dstEventId);
        var dstAttribute5 = dstAttributes.get(4);
        assertBusinessAttributeEqualTo(dstAttribute5, sojTag3, dstEventId);

        // associations
        var dstSignal = unstagedSignalService.getByIdWithAssociationsRecursive(srcSignalId);
        assertThat(dstSignal.getEvents().size()).isEqualTo(3);

        dstSignal.getEvents().forEach(event -> {
            if (event.getName().equals(eventName)) {
                assertEventEqualTo(event, dstEvent);
            }
        });

        // assert fields
        var dstFields = dstSignal.getFields();
        var platformFields = dstFields.stream().filter(UnstagedField::getIsMandatory).toList();
        var businessFields = dstFields.stream().filter(field -> !field.getIsMandatory()).collect(toList());
        assertThat(platformFields.size()).isEqualTo(2);
        assertThat(businessFields.size()).isEqualTo(3);

        var platformField = platformFields.stream()
                .filter(field -> field.getTag().equals(fieldTemplate1.getTag()))
                .findFirst()
                .orElseThrow();
        assertThat(platformField.getSignalId()).isEqualTo(srcSignalId.getId());
        assertPlatformFieldEqualTo(platformField, fieldTemplate1);

        businessFields.sort(Comparator.comparing(UnstagedField::getTag));
        var businessField1 = businessFields.get(0);
        assertBusinessFieldEqualTo(businessField1, sojTag1, srcSignalId.getId());
        var businessField2 = businessFields.get(1);
        assertBusinessFieldEqualTo(businessField2, sojTag2, srcSignalId.getId());
        var businessField3 = businessFields.get(2);
        assertBusinessFieldEqualTo(businessField3, sojTag3, srcSignalId.getId());
    }

    @Test
    void copySignalFromUnstaged() {
        // given
        var srcSignalId = createUnstagedSetup(planId, signalTemplateId);
        var srcSignal = unstagedSignalService.getById(srcSignalId);
        srcSignal.setEnvironment(Environment.PRODUCTION);
        unstagedSignalService.update(srcSignal);

        srcSignal = unstagedSignalService.getByIdWithAssociationsRecursive(srcSignalId);
        var request = UnstageRequest.ofUnstaged(planId, srcSignalId.getId(), srcSignalId.getVersion(), name, desc, null);

        // when
        var dstSignal = usecase.copySignalFromUnstaged(request);

        // then
        assertThat(dstSignal.getId()).isEqualTo(srcSignalId.getId());
        assertThat(dstSignal.getVersion()).isEqualTo(srcSignalId.getVersion() + 1);
        assertThat(dstSignal.getPlanId()).isEqualTo(planId);
        assertThat(dstSignal.getName()).isEqualTo(name);
        assertThat(dstSignal.getDescription()).isEqualTo(desc);
        assertThat(dstSignal.getOwners()).isEqualTo(plan.getOwners());
        assertThat(dstSignal.getDomain()).isEqualTo(plan.getDomain());
        assertThat(dstSignal.getSignalSourceId()).isEqualTo(srcSignalId.getId());
        assertThat(dstSignal.getSignalSourceVersion()).isEqualTo(srcSignalId.getVersion());
        assertThat(dstSignal.getSignalTemplateSourceId()).isEqualTo(srcSignal.getSignalTemplateSourceId());
        assertThat(dstSignal.getCompletionStatus()).isEqualTo(COMPLETED);
        assertThat(dstSignal.getEnvironment()).isEqualTo(Environment.UNSTAGED);
        assertThat(dstSignal.getCreateBy()).isEqualTo(IT_TEST_USER);
        assertThat(dstSignal.getUpdateBy()).isEqualTo(IT_TEST_USER);
        assertSignalEqualTo(dstSignal, srcSignal);

        var dstEvents = unstagedSignalService.getEvents(dstSignal.getSignalId());

        assertThat(dstEvents.size()).isEqualTo(2);
        var dstEvent = dstEvents.stream()
                .filter(evt -> evt.getName().equals(eventTemplate1.getName()))
                .findFirst()
                .orElseThrow();
        var srcEvent = srcSignal.getEvents().stream()
                .filter(evt -> evt.getName().equals(eventTemplate1.getName()))
                .findFirst()
                .orElseThrow();
        assertThat(dstEvent.getId()).isNotEqualTo(srcEvent.getId());

        assertThat(dstEvent.getPageIds()).hasSize(1);
        assertThat(dstEvent.getModuleIds()).hasSize(2);
        assertThat(dstEvent.getClickIds()).hasSize(3);

        assertEventEqualTo(dstEvent, srcEvent);

        var dstAttributes = unstagedEventService.getAttributes(dstEvent.getId());
        assertThat(dstAttributes.size()).isEqualTo(2);
        var dstAttribute1 = dstAttributes.get(0);
        var dstAttribute2 = dstAttributes.get(1);

        if ("attr2".equals(dstAttribute1.getDescription())) {
            dstAttribute1 = dstAttributes.get(1);
            dstAttribute2 = dstAttributes.get(0);
        }

        assertThat(dstAttribute1.getEventId()).isEqualTo(dstEvent.getId());
        assertPlatformAttributeEqualTo(dstAttribute1, attributeTemplate1);

        assertThat(dstAttribute2.getEventId()).isEqualTo(dstEvent.getId());
        assertPlatformAttributeEqualTo(dstAttribute2, attributeTemplate2);

        // assert fields
        var dstFields = unstagedSignalService.getFields(dstSignal.getSignalId());
        assertThat(dstFields.size()).isEqualTo(2);

        var dstField = dstFields.iterator().next();
        var srcField = srcSignal.getFields().stream()
                .filter(field -> field.getTag().equals(dstField.getTag()))
                .findFirst()
                .orElseThrow();
        assertThat(dstField.getSignalId()).isEqualTo(dstSignal.getId());
        assertPlatformFieldEqualTo(dstField, srcField);

        // associations
        dstSignal = unstagedSignalService.getByIdWithAssociations(dstSignal.getSignalId());
        assertThat(dstSignal.getEvents().size()).isEqualTo(2);
        assertThat(dstSignal.getFields().size()).isEqualTo(2);
    }

    private VersionedId createUnstagedSetup(long planId, long signalTemplateId) {
        // create an UnstagedSignal setup by copy the template setup
        var request = UnstageRequest.ofTemplate(planId, signalTemplateId, name, desc, null);
        var srcSignal = usecase.copySignalFromTemplate(request);
        var srcSignalId = srcSignal.getSignalId();
        srcSignal = unstagedSignalService.getByIdWithAssociationsRecursive(srcSignalId);

        var srcEvent = unstagedSignalService.getEvents(srcSignalId).stream()
                .filter(evt -> evt.getName().equals(eventTemplate1.getName()))
                .findFirst()
                .orElseThrow();

        srcEvent = unstagedEventService.getById(srcEvent.getId());
        srcEvent.setPageIds(Set.of(1L));
        srcEvent.setModuleIds(Set.of(2L, 3L));
        srcEvent.setClickIds(Set.of(4L, 5L, 6L));
        eventRepository.saveAndFlush(srcEvent);

        return srcSignal.getSignalId();
    }

    private void createSojEventAndTagSetup(List<Long> moduleIds) {
        var sojEvent1 = sojEvent("EXPC", 111L, moduleIds.get(0), null);
        var sojEvent2 = sojEvent("EXPC", 111L, moduleIds.get(1), null);
        sojEventRepository.saveAll(Set.of(sojEvent1, sojEvent2));

        sojTag1 = sojBusinessTagRepository.save(sojBusinessTag("tag1"));
        sojTag2 = sojBusinessTagRepository.save(sojBusinessTag("tag2"));
        sojTag3 = sojBusinessTagRepository.save(sojBusinessTag("tag3"));

        sojTagMappingRepository.saveAll(Set.of(
                new SojEventTagMapping(sojEvent1, sojTag1),
                new SojEventTagMapping(sojEvent2, sojTag2),
                new SojEventTagMapping(sojEvent2, sojTag3)
        ));
    }

    private void assertPlatformFieldEqualTo(Field field, Field expected) {
        assertThat(field.getTag()).isEqualTo(expected.getTag());
        assertThat(field.getName()).isEqualTo(expected.getName());
        assertThat(field.getDescription()).isEqualTo(expected.getDescription());
        assertThat(field.getJavaType()).isEqualTo(expected.getJavaType());
        assertThat(field.getAvroSchema()).isEqualTo(expected.getAvroSchema());
        assertThat(field.getExpression()).isEqualTo(expected.getExpression());
        assertThat(field.getExpressionType()).isEqualTo(expected.getExpressionType());
        assertThat(field.getIsMandatory()).isEqualTo(expected.getIsMandatory());
    }

    private void assertEventEqualTo(Event event, Event expected) {
        assertThat(event.getName()).isEqualTo(expected.getName());
        assertThat(event.getDescription()).isEqualTo(expected.getDescription());
        assertThat(event.getFsmOrder()).isEqualTo(expected.getFsmOrder());
        assertThat(event.getCardinality()).isEqualTo(expected.getCardinality());
        assertThat(event.getType()).isEqualTo(expected.getType());
        assertThat(event.getExpressionType()).isEqualTo(expected.getExpressionType());
    }

    private void assertSignalEqualTo(Signal signal, Signal expected) {
        assertThat(signal.getName()).isEqualTo(name);
        assertThat(signal.getDescription()).isEqualTo(desc);
        assertThat(signal.getDomain()).isEqualTo(expected.getDomain());
        assertThat(signal.getType()).isEqualTo(expected.getType());
        assertThat(signal.getRetentionPeriod()).isEqualTo(expected.getRetentionPeriod());
    }

    private void assertPlatformAttributeEqualTo(Attribute attribute, Attribute expected) {
        assertThat(attribute.getTag()).isEqualTo(expected.getTag());
        assertThat(attribute.getDescription()).isEqualTo(expected.getDescription());
        assertThat(attribute.getJavaType()).isEqualTo(expected.getJavaType());
        assertThat(attribute.getSchemaPath()).isEqualTo(expected.getSchemaPath());
        assertThat(attribute.getIsStoreInState()).isEqualTo(expected.getIsStoreInState());
    }

    private void assertBusinessFieldEqualTo(UnstagedField field, SojBusinessTag expected, long expectedSignalId) {
        assertThat(field.getSignalId()).isEqualTo(expectedSignalId);
        assertThat(field.getTag()).isEqualTo(expected.getSojName());
        assertThat(field.getName()).isEqualTo(expected.getSojName());
        assertThat(field.getDescription()).isEqualTo(expected.getDescription());
        assertThat(field.getJavaType()).isEqualTo(JavaType.fromValue(expected.getDataType()));
        assertThat(field.getExpression()).isEqualTo(expected.getSchemaPath());
        assertThat(field.getExpressionType()).isEqualTo(JEXL);
        assertThat(field.getIsMandatory()).isFalse();
    }

    private void assertBusinessAttributeEqualTo(UnstagedAttribute attribute, SojBusinessTag expected, long expectedEventId) {
        assertThat(attribute.getEventId()).isEqualTo(expectedEventId);
        assertThat(attribute.getTag()).isEqualTo(expected.getSojName());
        assertThat(attribute.getDescription()).isEqualTo(expected.getDescription());
        assertThat(attribute.getJavaType()).isEqualTo(JavaType.fromValue(expected.getDataType()));
        assertThat(attribute.getSchemaPath()).isEqualTo(expected.getSchemaPath());
    }
}
