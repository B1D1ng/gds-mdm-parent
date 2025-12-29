package com.ebay.behavior.gds.mdm.signal.usecase;

import com.ebay.behavior.gds.mdm.signal.common.model.EventTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstageRequest;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedAttribute;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.repository.SojPlatformTagRepository;
import com.ebay.behavior.gds.mdm.signal.service.AttributeTemplateService;
import com.ebay.behavior.gds.mdm.signal.service.EventTemplateService;
import com.ebay.behavior.gds.mdm.signal.service.EventTypeLookupService;
import com.ebay.behavior.gds.mdm.signal.service.FieldTemplateService;
import com.ebay.behavior.gds.mdm.signal.service.PlanService;
import com.ebay.behavior.gds.mdm.signal.service.SignalImportService;
import com.ebay.behavior.gds.mdm.signal.service.SignalTemplateService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedEventService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedFieldGroupService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedSignalService;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ebay.behavior.gds.mdm.common.model.ExpressionType.JEXL;
import static com.ebay.behavior.gds.mdm.common.model.ExpressionType.LITERAL;
import static com.ebay.behavior.gds.mdm.common.model.JavaType.BOOLEAN;
import static com.ebay.behavior.gds.mdm.common.model.JavaType.LONG;
import static com.ebay.behavior.gds.mdm.common.model.JavaType.STRING;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.common.model.EventSource.SOJ;
import static com.ebay.behavior.gds.mdm.signal.common.model.EventSource.ST;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.CLIENT_PAGE_VIEW;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.NON_SOJ_PLATFORM_FIELDS;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.PAGE_IMPRESSION_SIGNAL;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.PAGE_SERVE;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.PAGE_VIEW_ENTRY;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.PAGE_VIEW_EXIT;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.attribute;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.eventTemplate;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.field;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.signalTemplate;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.sojPlatformField;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.CJS_PLATFORM_ID;
import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * This is a specific page impression template test.
 * Needed since the complexity of testing the page impression template with add/remove event scenario.
 */
@Slf4j
@ActiveProfiles(IT)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@SuppressWarnings({"PMD.JUnit5TestShouldBePackagePrivate", "PMD.DetachedTestCase", "PMD.SingularField"})
public class UnstageUsecaseComplexScenariosIT {

    public static final String TIMESTAMP = "timestamp";
    private static final String SIGNAL_TEMPLATE = " signal template";

    @Autowired
    private SignalTemplateService signalTemplateService;

    @Autowired
    private EventTemplateService eventTemplateService;

    @Autowired
    private FieldTemplateService fieldTemplateService;

    @Autowired
    private AttributeTemplateService attributeTemplateService;

    @Autowired
    private UnstagedSignalService unstagedSignalService;

    @Autowired
    private UnstagedEventService unstagedEventService;

    @Autowired
    private UnstagedFieldGroupService unstagedFieldGroupService;

    @Autowired
    private UnstageUsecase usecase;

    @Autowired
    private SignalImportService importService;

    @Autowired
    private PlanService planService;

    @Autowired
    private EventTypeLookupService eventTypeLookupService;

    @Autowired
    private SojPlatformTagRepository sojPlatformTagRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private Environment env;

    private long planId;
    private long signalTemplateId;
    private long pageViewEntryId;
    private long pageViewExitId;
    private long pageServeId;
    private long clientPageViewId;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        if (env.matchesProfiles(IT)) { // We must not run this script if we run this class with dev/staging DB
            jdbcTemplate.execute("RUNSCRIPT FROM 'classpath:/dml/lookup.sql'");
        }

        var plan = TestModelUtils.plan().setDomain("SEARCH1");
        planId = planService.create(plan).getId();
        signalTemplateId = pageImpressionSignal();
    }

    @Test
    void deleteAllEvents_addPageViewEntryBack() {
        // create signal from template
        var request = UnstageRequest.ofTemplate(planId, signalTemplateId, "name", "desc", Set.of());
        var srcSignal = usecase.copySignalFromTemplate(request);
        var srcSignalId = srcSignal.getSignalId();

        // delete all events
        unstagedSignalService.getEvents(srcSignalId).forEach(event -> unstagedEventService.delete(event.getId()));

        // validate field groups after deleting all events
        var fieldGroupsBefore = unstagedFieldGroupService.getAll(srcSignalId);
        assertThat(fieldGroupsBefore).isEmpty();
        assertThat(unstagedSignalService.getFields(srcSignalId)).isEmpty();

        // add a single event back
        var eventRequest = UnstageRequest.ofTemplate(srcSignalId.getId(), pageViewEntryId, "new_event_added_back", "new_event_desc", Set.of());
        usecase.copyEventFromTemplate(eventRequest);

        // validate field groups after adding new event back
        var fieldGroupsAfterAdded = unstagedFieldGroupService.getAll(srcSignalId);
        assertThat(fieldGroupsAfterAdded.size()).isEqualTo(3);
        assertThat(fieldGroupsAfterAdded.stream().flatMap(group -> group.getEventTypesAsList().stream())).containsOnly(PAGE_VIEW_ENTRY);
    }

    @Test
    void deleteAllEvents_addPageServeBack() {
        // create signal from template
        var request = UnstageRequest.ofTemplate(planId, signalTemplateId, "name", "desc", Set.of());
        var srcSignal = usecase.copySignalFromTemplate(request);
        var srcSignalId = srcSignal.getSignalId();

        // delete all events
        unstagedSignalService.getEvents(srcSignalId).forEach(event -> unstagedEventService.delete(event.getId()));

        // validate field groups after deleting all events
        var fieldGroupsBefore = unstagedFieldGroupService.getAll(srcSignalId);
        assertThat(fieldGroupsBefore).isEmpty();
        assertThat(unstagedSignalService.getFields(srcSignalId)).isEmpty();

        // add a single event back
        var eventRequest = UnstageRequest.ofTemplate(srcSignalId.getId(), pageServeId, "new_event_added_back", "new_event_desc", Set.of());
        usecase.copyEventFromTemplate(eventRequest);

        // validate field groups after adding new event back
        var fieldGroupsAfterAdded = unstagedFieldGroupService.getAll(srcSignalId);
        assertThat(fieldGroupsAfterAdded.size()).isEqualTo(3);
        assertThat(fieldGroupsAfterAdded.stream().flatMap(group -> group.getEventTypesAsList().stream())).containsOnly(PAGE_SERVE);
    }

    @Test
    void deleteEvent_addEventBack() {
        // create signal from template
        var eventType = PAGE_SERVE;
        var request = UnstageRequest.ofTemplate(planId, signalTemplateId, "name", "desc", Set.of());
        var srcSignal = usecase.copySignalFromTemplate(request);
        var srcSignalId = srcSignal.getSignalId();

        // validate field groups before adding new event
        var fieldGroupsBefore = unstagedFieldGroupService.getAll(srcSignalId);
        var eventFieldGroupsBefore = fieldGroupsBefore.stream()
                .filter(grp -> grp.getEventTypesAsList().contains(eventType))
                .toList();
        assertThat(eventFieldGroupsBefore.size()).isEqualTo(3);

        // validate attributes of the event to be deleted and added back
        var event = unstagedSignalService.getEvents(srcSignalId).stream()
                .filter(evt -> evt.getType().equals(eventType))
                .findFirst()
                .orElseThrow();
        var attributesBefore = unstagedEventService.getAttributes(event.getId());
        assertThat(attributesBefore).hasSize(2);

        // delete event
        unstagedEventService.delete(event.getId());

        // validate field groups after deleting an event
        var fieldGroupsAfter = unstagedFieldGroupService.getAll(srcSignalId);
        var eventFieldGroupsAfter = fieldGroupsAfter.stream()
                .filter(grp -> grp.getEventTypesAsList().contains(eventType))
                .toList();
        assertThat(eventFieldGroupsAfter).isEmpty();

        // add deleted event back
        var eventRequest = UnstageRequest.ofTemplate(srcSignalId.getId(), pageServeId, "new_event_added_back", "new_event_desc", Set.of());
        usecase.copyEventFromTemplate(eventRequest);

        // validate field groups after adding new event back
        var fieldGroupsAfterAdded = unstagedFieldGroupService.getAll(srcSignalId);
        var eventFieldGroupsAfterAdded = fieldGroupsAfterAdded.stream()
                .filter(grp -> grp.getEventTypesAsList().contains(eventType))
                .toList();
        assertThat(eventFieldGroupsAfterAdded.size()).isEqualTo(3);

        // validate attributes of the event to be deleted and added back
        event = unstagedSignalService.getEvents(srcSignalId).stream()
                .filter(evt -> evt.getType().equals(eventType))
                .findFirst()
                .orElseThrow();
        var attributesAfter = unstagedEventService.getAttributes(event.getId());
        assertThat(attributesAfter).hasSize(2);
    }

    @Test
    void copyEventFromTemplate_clientPageView() {
        // create signal from template
        var request = UnstageRequest.ofTemplate(planId, signalTemplateId, "name", "desc", Set.of());
        var srcSignal = usecase.copySignalFromTemplate(request);
        var srcSignalId = srcSignal.getSignalId();

        // validate signal before adding new event
        var srcSignalBefore = unstagedSignalService.getByIdWithAssociations(srcSignalId);
        var eventsBefore = unstagedSignalService.getEvents(srcSignalId);
        var connectedEventIdsBefore = unstagedSignalService.getConnectedEventIds(srcSignalId);
        assertThat(srcSignalBefore.getEvents().size()).isEqualTo(1); // Only 1 mandatory event
        assertThat(eventsBefore.size()).isEqualTo(1);
        assertThat(connectedEventIdsBefore.size()).isEqualTo(1);

        // add new event
        var eventRequest = UnstageRequest.ofTemplate(srcSignalId.getId(), clientPageViewId, "new_event", "new_event_desc", Set.of());
        usecase.copyEventFromTemplate(eventRequest);

        // validate signal after adding new event
        var srcSignalAfter = unstagedSignalService.getByIdWithAssociations(srcSignalId);
        var eventsAfter = unstagedSignalService.getEvents(srcSignalId);
        var connectedEventIdsAfter = unstagedSignalService.getConnectedEventIds(srcSignalId);
        assertThat(srcSignalAfter.getEvents().size()).isEqualTo(2);
        assertThat(eventsAfter.size()).isEqualTo(2);
        assertThat(connectedEventIdsAfter.size()).isEqualTo(2);
    }

    @Test
    @Transactional
    void importUnstagedSignal_compareOriginalWithClone() {
        // create signal from template
        var request = UnstageRequest.ofTemplate(planId, signalTemplateId, "name", "desc", Set.of());
        var srcSignal = usecase.copySignalFromTemplate(request);
        var srcSignalId = srcSignal.getSignalId();

        // clone it by using importSignal
        var original = unstagedSignalService.getByIdWithAssociationsRecursive(srcSignalId);
        entityManager.detach(original);

        var cloneId = original.getId() + 1;
        original.setId(cloneId);
        var cloneSignalId = importService.importUnstagedSignal(original);

        // validate original equal to clone
        var clone = unstagedSignalService.getByIdWithAssociationsRecursive(cloneSignalId);

        var excludedProperties = List.of("id", "createDate", "updateDate", "fields", "events", "attributes",
                "event", "signal", "signalId", "eventId", "eventSourceId", "pageIds", "moduleIds", "clickIds").toArray(new String[0]);
        assertThat(clone)
                .usingRecursiveComparison()
                .ignoringFields(excludedProperties)
                .isEqualTo(original);

        var origEvents = original.getEvents().stream().sorted(comparing(UnstagedEvent::getName)).toList();
        var cloneEvents = clone.getEvents().stream().sorted(comparing(UnstagedEvent::getName)).toList();
        assertThat(cloneEvents)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields(excludedProperties)
                .isEqualTo(origEvents);

        var origFields = original.getFields().stream().sorted(comparing(UnstagedField::getTag)).toList();
        var cloneFields = clone.getFields().stream().sorted(comparing(UnstagedField::getTag)).toList();
        assertThat(cloneFields)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields(excludedProperties)
                .isEqualTo(origFields);

        var origAttributes = origFields.stream().flatMap(field -> field.getAttributes().stream().sorted(comparing(UnstagedAttribute::getTag))).toList();
        var cloneAttributes = cloneFields.stream().flatMap(field -> field.getAttributes().stream().sorted(comparing(UnstagedAttribute::getTag))).toList();
        assertThat(cloneAttributes)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields(excludedProperties)
                .isEqualTo(origAttributes);

        origAttributes = origEvents.stream().flatMap(field -> field.getAttributes().stream().sorted(comparing(UnstagedAttribute::getTag))).toList();
        cloneAttributes = cloneEvents.stream().flatMap(field -> field.getAttributes().stream().sorted(comparing(UnstagedAttribute::getTag))).toList();
        for (int i = 0; i < origAttributes.size(); i++) {
            var origAttr = origAttributes.get(i);
            var cloneAttr = cloneAttributes.get(i);
            assertThat(cloneAttr)
                    .usingRecursiveComparison()
                    .ignoringFields(excludedProperties)
                    .isEqualTo(origAttr);
        }
    }

    private long pageImpressionSignal() throws JsonProcessingException {
        // events
        val signalName = "Page Impression";
        var pageViewEntry = eventTemplate(PAGE_VIEW_ENTRY, "Page view entry", "Page view entry event", ST,
                "[\"VIEW.PAGE_LOAD\", \"VIEW.PAGE_RELOAD\", \"VIEW.TAB_VIEW\", \"VIEW.TAB_SWITCH\", \"VIEW.BACK_FORWARD\"]"
                        + ".contains(event.eventPayload.activityType) and [${PAGE_IDS}].contains(event.context.pageInteractionContext.pageId)", false);
        pageViewEntry = eventTemplateService.create(pageViewEntry);
        pageViewEntryId = pageViewEntry.getId();

        var pageViewExit = eventTemplate(PAGE_VIEW_EXIT, "Page view exit", "Page view exit event", ST,
                "[\"EXIT.PAGE_UNLOAD\", \"EXIT.TAB_HIDE\"].contains(event.eventPayload.activityType) and "
                        + "[${PAGE_IDS}].contains(event.context.pageInteractionContext.pageId)", false);
        pageViewExit = eventTemplateService.create(pageViewExit);
        pageViewExitId = pageViewExit.getId();

        var pageServe = eventTemplate(PAGE_SERVE, "Page experience event", "Page experience event", SOJ,
                "[\"EXPC\"].contains(event.eventPayload.eventType) and [${PAGE_IDS}].contains(event.context.pageInteractionContext.pageId)",
                true);
        pageServe = eventTemplateService.create(pageServe);
        pageServeId = pageServe.getId();

        var clientPageView = eventTemplate(CLIENT_PAGE_VIEW, "Client page view", "Client page view event", SOJ,
                "[\"CLIENT_PAGE_VIEW\"].contains(event.eventPayload.eventType) and [${PAGE_IDS}].contains(event.context.pageInteractionContext.pageId)",
                false);
        clientPageView = eventTemplateService.create(clientPageView);
        clientPageViewId = clientPageView.getId();

        // attributes
        var attr1 = attribute(pageViewEntryId, TIMESTAMP, "event.eventPayload.timestamp", STRING);
        var attr2 = attribute(pageViewExitId, TIMESTAMP, "event.eventPayload.timestamp", STRING);
        var attr3 = attribute(pageServeId, TIMESTAMP, "event.eventPayload.timestamp", STRING);
        var attr4 = attribute(clientPageViewId, TIMESTAMP, "event.eventPayload.timestamp", STRING);
        var attrId1 = attributeTemplateService.create(attr1).getId();
        var attrId2 = attributeTemplateService.create(attr2).getId();
        var attrId3 = attributeTemplateService.create(attr3).getId();
        var attrId4 = attributeTemplateService.create(attr4).getId();

        var signal = signalTemplate(PAGE_IMPRESSION_SIGNAL, signalName, signalName + SIGNAL_TEMPLATE, CJS_PLATFORM_ID);
        var signalId = signalTemplateService.create(signal).getId();

        // non-soj platform fields
        var isViewed = field(signalId, "isViewed", "Is view event from client tracking or not.", BOOLEAN, "true", LITERAL, true);
        var viewedTs = field(signalId, "viewedTs", "Viewed timestamp, derived from domain client tracking or Surface Tracking view events.",
                LONG, "event.eventPayload.timestamp", JEXL, true).setIsCached(true);
        var exitExitedTs = field(signalId, "exitedTs", "Exit timestamp, derived from Surface Tracking page view exit events.", LONG,
                "event.eventPayload.timestamp", JEXL, true);
        var exitDwell = field(signalId, "dwell", "dwell time between view and exit time.", LONG, "event.eventPayload.timestamp",
                JEXL, true);
        var serveServedTs = field(signalId, "servedTs", "Served timestamp, derived from Soj's server tracking events.", LONG,
                "event.eventPayload.timestamp", JEXL, true);
        var serveIsServed = field(signalId, "isServed", "Is serve event from SOJ server tracking or not.", BOOLEAN, "true", LITERAL, true);

        var entryTypeId = eventTypeLookupService.getByName(PAGE_VIEW_ENTRY).getId();
        var serveTypeId = eventTypeLookupService.getByName(PAGE_SERVE).getId();
        var clientViewTypeId = eventTypeLookupService.getByName(CLIENT_PAGE_VIEW).getId();

        fieldTemplateService.create(isViewed, Set.of(), Set.of(entryTypeId, clientViewTypeId));
        fieldTemplateService.create(viewedTs, Set.of(attrId1, attrId4), null); // attrId1 - PAGE_VIEW_ENTRY, attrId4 - CLIENT_PAGE_VIEW
        fieldTemplateService.create(exitExitedTs, Set.of(attrId2), null); // attrId2 - PAGE_VIEW_EXIT
        fieldTemplateService.create(exitDwell, Set.of(attrId2), null); // attrId2 - PAGE_VIEW_EXIT
        fieldTemplateService.create(serveServedTs, Set.of(attrId3), null); // attrId1 - PAGE_SERVE
        fieldTemplateService.create(serveIsServed, Set.of(), Set.of(serveTypeId));

        // soj platform fields
        createPlatformTags(signalId, List.of(pageViewEntry, pageViewExit, clientPageView, pageServe));

        signal = signalTemplateService.getByIdWithAssociationsRecursive(signalId);
        val strSignal = objectMapper.writeValueAsString(signal);
        log.info(strSignal);

        // test it
        assertThat(signal.getType()).isEqualTo(PAGE_IMPRESSION_SIGNAL);
        assertThat(signal.getEvents().size()).isEqualTo(4);

        val events = signal.getEvents();
        assertThat(events).extracting("type").containsExactlyInAnyOrder(PAGE_VIEW_ENTRY, PAGE_VIEW_EXIT, PAGE_SERVE, CLIENT_PAGE_VIEW);

        return signalId;
    }

    private void createPlatformTags(long signalId, List<EventTemplate> events) {
        val tags = sojPlatformTagRepository.findAll();

        for (val tag : tags) {
            if (!NON_SOJ_PLATFORM_FIELDS.contains(tag.getSojName())) {
                // Only create soj platform fields, it doesn't make sense to have viewedTs, exitedTs and servedTs in single page impression event.
                // But we still keep those non-soj platform fields in the soj_platform_tag table for signal migration.
                var attrIds = events.stream()
                        .map(evt -> attribute(evt.getId(), tag))
                        .map(attr -> attributeTemplateService.create(attr).getId())
                        .collect(Collectors.toSet());
                val field = sojPlatformField(signalId, tag);
                fieldTemplateService.create(field, attrIds, null);
            }
        }
    }
}
