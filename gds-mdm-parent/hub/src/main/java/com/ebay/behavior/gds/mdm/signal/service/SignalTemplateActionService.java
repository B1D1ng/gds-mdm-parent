package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.model.ExpressionType;
import com.ebay.behavior.gds.mdm.common.model.JavaType;
import com.ebay.behavior.gds.mdm.signal.common.model.AnswerPropertyPlaceholder;
import com.ebay.behavior.gds.mdm.signal.common.model.AttributeTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.EventSource;
import com.ebay.behavior.gds.mdm.signal.common.model.EventTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.FieldTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.SojPlatformTag;
import com.ebay.behavior.gds.mdm.signal.common.model.TemplateQuestion;
import com.ebay.behavior.gds.mdm.signal.repository.SojPlatformTagRepository;
import com.ebay.behavior.gds.mdm.signal.service.userAnswer.UserAnswerExpressionSetter;
import com.ebay.behavior.gds.mdm.signal.service.userAnswer.UserAnswerMetadataSetter;
import com.ebay.behavior.gds.mdm.signal.service.userAnswer.UserAnswerPropertySetter;
import com.ebay.behavior.gds.mdm.signal.service.userAnswer.UserAnswerSetter;
import com.ebay.behavior.gds.mdm.signal.common.model.TemplateFieldDefinition;

import jakarta.inject.Inject;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.ebay.behavior.gds.mdm.common.model.CompletionStatus.COMPLETED;
import static com.ebay.behavior.gds.mdm.common.model.ExpressionType.JEXL;
import static com.ebay.behavior.gds.mdm.common.model.ExpressionType.LITERAL;
import static com.ebay.behavior.gds.mdm.common.model.JavaType.BOOLEAN;
import static com.ebay.behavior.gds.mdm.common.model.JavaType.INTEGER;
import static com.ebay.behavior.gds.mdm.common.model.JavaType.LONG;
import static com.ebay.behavior.gds.mdm.common.model.JavaType.STRING;
import static com.ebay.behavior.gds.mdm.signal.common.model.AnswerPropertyPlaceholder.BUSINESS_OUTCOME_ACTION;
import static com.ebay.behavior.gds.mdm.signal.common.model.AnswerPropertyPlaceholder.CHANNEL_ID;
import static com.ebay.behavior.gds.mdm.signal.common.model.AnswerPropertyPlaceholder.CLICK_IDS;
import static com.ebay.behavior.gds.mdm.signal.common.model.AnswerPropertyPlaceholder.CS_APP_NAME;
import static com.ebay.behavior.gds.mdm.signal.common.model.AnswerPropertyPlaceholder.CS_EVENT_IDS;
import static com.ebay.behavior.gds.mdm.signal.common.model.AnswerPropertyPlaceholder.CS_EVENT_NAME;
import static com.ebay.behavior.gds.mdm.signal.common.model.AnswerPropertyPlaceholder.EVENT_TYPE;
import static com.ebay.behavior.gds.mdm.signal.common.model.AnswerPropertyPlaceholder.ITEM_SOURCE_TYPE;
import static com.ebay.behavior.gds.mdm.signal.common.model.AnswerPropertyPlaceholder.MODULE_IDS;
import static com.ebay.behavior.gds.mdm.signal.common.model.AnswerPropertyPlaceholder.PAGE_IDS;
import static com.ebay.behavior.gds.mdm.signal.common.model.AnswerPropertyPlaceholder.SURFACE_TYPE;
import static com.ebay.behavior.gds.mdm.signal.common.model.AnswerPropertyPlaceholder.TRANSACTION_SOURCE_TYPE;
import static com.ebay.behavior.gds.mdm.signal.common.model.EventSource.CSTECH;
import static com.ebay.behavior.gds.mdm.signal.common.model.EventSource.EBAY_ITEMS;
import static com.ebay.behavior.gds.mdm.signal.common.model.EventSource.OFFSITE;
import static com.ebay.behavior.gds.mdm.signal.common.model.EventSource.ROI;
import static com.ebay.behavior.gds.mdm.signal.common.model.EventSource.SOJ;
import static com.ebay.behavior.gds.mdm.signal.common.model.EventSource.ST;
import static com.ebay.behavior.gds.mdm.signal.common.model.EventSource.TRANSACTION_EBAYLIVE_ATTRIBUTED_STREAM;
import static com.ebay.behavior.gds.mdm.signal.util.ImportUtils.CJS;

@Service
@Validated
@SuppressWarnings("PMD.UnusedFormalParameter")
public class SignalTemplateActionService {

    private static final Class<? extends UserAnswerSetter> PROPERTY_SETTER_TYPE = UserAnswerPropertySetter.class;
    private static final Class<? extends UserAnswerSetter> METADATA_SETTER_TYPE = UserAnswerMetadataSetter.class;
    private static final Class<? extends UserAnswerSetter> EXPRESSION_SETTER_TYPE = UserAnswerExpressionSetter.class;

    public static final String TIMESTAMP = "timestamp";
    private static final String TIMESTAMP_EXPRESSION = "event.eventPayload.timestamp";
    private static final String EVENT_TIMESTAMP_EXPRESSION = "event.gdsSourceTs";
    private static final String SIGNAL_TEMPLATE = " signal template";
    private static final String TRACKING_MODERATOR = "trackingModerator";

    // signal types
    public static final String PAGE_IMPRESSION_SIGNAL = "PAGE_IMPRESSION";
    public static final String MODULE_IMPRESSION_SIGNAL = "MODULE_IMPRESSION";
    public static final String ONSITE_CLICK_SIGNAL = "ONSITE_CLICK";
    public static final String OFFSITE_CLICK_SIGNAL = "OFFSITE_CLICK";
    public static final String BUSINESS_OUTCOME_SIGNAL = "BUSINESS_OUTCOME";
    public static final String EJS_SIGNAL = "EJS_SIGNAL";
    public static final String ITEM = "ITEM";
    public static final String HIVE_AUTO_GENERATED = "HIVE_AUTO_GENERATED";
    public static final String TRANSACTION_SIGNAL = "TRANSACTION";

    // event types
    public static final String PAGE_VIEW_ENTRY = "PAGE_VIEW_ENTRY";
    public static final String PAGE_VIEW_EXIT = "PAGE_VIEW_EXIT";
    public static final String PAGE_SERVE = "PAGE_SERVE";
    public static final String CLIENT_PAGE_VIEW = "CLIENT_PAGE_VIEW";
    public static final String SOJ_CLICK = "SOJ_CLICK";
    public static final String MODULE_CLICK = "MODULE_CLICK";
    public static final String SERVICE_CALL = "SERVICE_CALL";
    public static final String OFFSITE_EVENT = "OFFSITE_EVENT";
    public static final String MODULE_VIEW = "MODULE_VIEW";
    public static final String ROI_EVENT = "ROI_EVENT";
    public static final String CSEVENT = "CSEVENT";
    public static final String ITEM_SERVE = "ITEM_SERVE";
    public static final String HIVE_EVENT = "HIVE_EVENT";
    public static final String TRANSACTION_EVENT = "TRANSACTION_EVENT";

    // signal names
    public static final String PAGE_IMPRESSION_SIGNAL_NAME = "Page Impression";
    public static final String ONSITE_CLICK_SIGNAL_NAME = "Onsite Click";
    public static final String OFFSITE_CLICK_SIGNAL_NAME = "Offsite Click";
    public static final String MODULE_IMPRESSION_SIGNAL_NAME = "Module Impression";
    public static final String BUSINESS_OUTCOME_SIGNAL_NAME = "Business Outcome";
    public static final String EJS_SIGNAL_NAME = "EJS Signal";
    public static final String ITEM_SIGNAL_NAME = "Item Signal";
    public static final String TRANSACTION_SIGNAL_NAME = "Transaction";

    // non-soj platform fields
    private static final String IS_VIEWED = "isViewed";
    private static final String VIEWED_TS = "viewedTs";
    private static final String EXITED_TS = "exitedTs";
    private static final String DWELL = "dwell";
    private static final String SERVED_TS = "servedTs";
    private static final String IS_SERVED = "isServed";
    private static final String CLICKED_TS = "clickedTs";
    private static final String RECORD_TS = "recordTs";
    private static final String CLICKED_TS_DESC = "Click Timestamp.";
    private static final String SIGNAL_GDS_SOURCE_TS = "signal.gdsSourceTs";
    // questions
    private static final String PROPERTY_GIT_REPO = "githubRepositoryUrl";
    private static final String PROPERTY_PAGE_IDS = "pageIds";
    private static final String PROPERTY_MODULE_IDS = "moduleIds";
    private static final String PROPERTY_SOURCE_TYPE = "itemSourceType";
    private static final String PROPERTY_CLICK_IDS = "clickIds";
    private static final String PROPERTY_SURFACE_TYPE = "surfaceType";
    private static final String QUESTION_CLIENT_PAGE = "What is your client page id(s)?";
    private static final String QUESTION_SERVER_PAGE = "What is your server page id(s)?";
    private static final String QUESTION_CLIENT_GIT = "What is your client project gitHub repository URL?";
    private static final String QUESTION_SERVER_GIT = "What is your server project gitHub repository URL?";
    private static final String QUESTION_SURFACE_TYPE = "What is your surface type?";
    private static final String QUESTION_MODULE = "What is your module id(s)?";
    private static final String QUESTION_CLICK = "What is your click id(s)?";
    private static final String QUESTION_EVENT_TYPE = "What is the event type for the service call event?";
    private static final String QUESTION_ITEM_SOURCE = "What is the source stream for this signal?";
    private static final String QUESTION_TRANSACTION_SOURCE = "What is the source stream for this transaction signal?";

    public static final Set<String> NON_SOJ_PLATFORM_FIELDS = Set.of(IS_VIEWED, VIEWED_TS, EXITED_TS, DWELL, SERVED_TS, IS_SERVED, CLICKED_TS, RECORD_TS);

    @Autowired
    private SignalTemplateService signalService;

    @Autowired
    private EventTemplateService eventService;

    @Autowired
    private FieldTemplateService fieldService;

    @Autowired
    private AttributeTemplateService attributeService;

    @Autowired
    private TemplateQuestionService questionService;

    @Autowired
    private EventTypeLookupService eventTypeLookupService;

    @Autowired
    private SojPlatformTagRepository sojPlatformTagRepository;

    @Autowired
    private TransactionSignalEnrichmentService transactionSignalEnrichmentService;

    @Inject
    private PlatformLookupService platformService;

    private final Map<String, Function<Optional<List<TemplateFieldDefinition>>, SignalTemplate>> signalSuppliers = Map.of(
            PAGE_IMPRESSION_SIGNAL, this::pageImpressionSignal,
            ONSITE_CLICK_SIGNAL, this::onsiteClickSignal,
            OFFSITE_CLICK_SIGNAL, this::offsiteClickSignal,
            MODULE_IMPRESSION_SIGNAL, this::moduleImpressionSignal,
            BUSINESS_OUTCOME_SIGNAL, this::businessOutcomeSignal,
            EJS_SIGNAL, this::ejsSignal,
            ITEM, this::itemSignal,
            TRANSACTION_SIGNAL, this::transactionSignal
    );

    public SignalTemplate recreate(String type) {
        return recreate(type, Optional.empty());
    }

    public SignalTemplate recreate(String type, Optional<List<TemplateFieldDefinition>> maybeFieldsConfig) {
        val signal = signalService.findByType(type);
        // Delete the existing signal template
        signal.ifPresent(signalTemplate -> signalService.deleteRecursive(signalTemplate.getId()));

        Function<Optional<List<TemplateFieldDefinition>>, SignalTemplate> supplier = signalSuppliers.get(type);
        if (supplier == null) {
            throw new IllegalArgumentException("Unsupported signal type: " + type);
        }
        return supplier.apply(maybeFieldsConfig);
    }

    private SignalTemplate itemSignal(Optional<List<TemplateFieldDefinition>> maybeFieldsConfig) {
        // events
        var itemSource = eventTemplate(ITEM_SERVE, "Item serve", "Item serve event", EBAY_ITEMS,
                "event?.eventInfo.source == ${ITEM_SOURCE_TYPE}",
                true);
        itemSource = eventService.create(itemSource);
        var itemSourceId = itemSource.getId();

        // questions: event?.eventInfo.source == 'ITEM_ASPECT'
        var itemQuestion = expressionQuestion(QUESTION_ITEM_SOURCE, STRING, ITEM_SOURCE_TYPE);

        questionService.createAll(Set.of(itemQuestion),Set.of(itemSourceId));

        var signal = signalTemplate(ITEM, ITEM_SIGNAL_NAME, ITEM_SIGNAL_NAME + SIGNAL_TEMPLATE, platformService.getPlatformId(ITEM));
        var signalId = signalService.create(signal).getId();

        // attributes
        var attr1 = attribute(itemSourceId, TIMESTAMP, TIMESTAMP_EXPRESSION, STRING);
        var attrId1 = attributeService.create(attr1).getId();

        //fields
        var itemSourceTimestamp = field(signalId, SIGNAL_GDS_SOURCE_TS, "GDS source time stamp", LONG, SIGNAL_GDS_SOURCE_TS, JEXL, true);
        var itemTypeId = eventTypeLookupService.getByName(ITEM_SERVE).getId();
        fieldService.create(itemSourceTimestamp, Set.of(attrId1), Set.of(itemTypeId));

        return signalService.getByIdWithAssociationsRecursive(signalId);
    }

    private SignalTemplate pageImpressionSignal(Optional<List<TemplateFieldDefinition>> maybeFieldsConfig) {
        // events
        var pageViewEntry = eventTemplate(PAGE_VIEW_ENTRY, "Page view entry", "Page view entry event", ST,
                "[\"VIEW.PAGE_LOAD\", \"VIEW.PAGE_RELOAD\", \"VIEW.TAB_VIEW\", \"VIEW.TAB_SWITCH\", \"VIEW.BACK_FORWARD\"]"
                        + ".contains(event.eventPayload.activityType) and [${PAGE_IDS}].contains(event.context.pageInteractionContext.pageId)", false);
        pageViewEntry = eventService.create(pageViewEntry);
        final var pageViewEntryId = pageViewEntry.getId();

        var pageViewExit = eventTemplate(PAGE_VIEW_EXIT, "Page view exit", "Page view exit event", ST,
                "[\"EXIT.PAGE_UNLOAD\", \"EXIT.TAB_HIDE\"].contains(event.eventPayload.activityType) and "
                        + "[${PAGE_IDS}].contains(event.context.pageInteractionContext.pageId)", false);
        pageViewExit = eventService.create(pageViewExit);
        final var pageViewExitId = pageViewExit.getId();

        var pageServe = eventTemplate(PAGE_SERVE, "Page experience event", "Page experience event", SOJ,
                "[\"EXPC\"].contains(event.eventPayload.eventType) and [${PAGE_IDS}].contains(event.context.pageInteractionContext.pageId)",
                true);
        pageServe = eventService.create(pageServe);
        var pageServeId = pageServe.getId();

        var clientPageView = eventTemplate(CLIENT_PAGE_VIEW, "Client page view", "Client page view event", SOJ,
                "[\"CLIENT_PAGE_VIEW\"].contains(event.eventPayload.eventType) and [${PAGE_IDS}].contains(event.context.pageInteractionContext.pageId)",
                false);
        clientPageView = eventService.create(clientPageView);
        var clientPageViewId = clientPageView.getId();

        // questions
        var clientSidePageQuestion = metadataQuestion(QUESTION_CLIENT_PAGE, PROPERTY_PAGE_IDS, PAGE_IDS);
        var serverSidePageQuestion = metadataQuestion(QUESTION_SERVER_PAGE, PROPERTY_PAGE_IDS, PAGE_IDS);
        var clientSideRepoQuestion = propertyQuestion(QUESTION_CLIENT_GIT, STRING, PROPERTY_GIT_REPO);
        var serverSideRepoQuestion = propertyQuestion(QUESTION_SERVER_GIT, STRING, PROPERTY_GIT_REPO);
        var surfaceQuestion = propertyQuestion(QUESTION_SURFACE_TYPE, STRING, PROPERTY_SURFACE_TYPE, SURFACE_TYPE);

        surfaceQuestion = questionService.create(surfaceQuestion, Set.of(pageServeId));
        questionService.createEventMapping(surfaceQuestion.getId(), clientPageViewId);
        questionService.createAll(Set.of(clientSidePageQuestion, clientSideRepoQuestion), Set.of(clientPageViewId));
        questionService.createAll(Set.of(serverSidePageQuestion, serverSideRepoQuestion), Set.of(pageViewEntryId, pageViewExitId, pageServeId));

        // attributes
        var attr1 = attribute(pageViewEntryId, TIMESTAMP, TIMESTAMP_EXPRESSION, STRING);
        var attr2 = attribute(pageViewExitId, TIMESTAMP, TIMESTAMP_EXPRESSION, STRING);
        var attr3 = attribute(pageServeId, TIMESTAMP, TIMESTAMP_EXPRESSION, STRING);
        var attr4 = attribute(clientPageViewId, TIMESTAMP, TIMESTAMP_EXPRESSION, STRING);
        var attrId1 = attributeService.create(attr1).getId();
        var attrId2 = attributeService.create(attr2).getId();
        var attrId3 = attributeService.create(attr3).getId();
        var attrId4 = attributeService.create(attr4).getId();

        val platformId = platformService.getPlatformId(CJS);

        var signal = signalTemplate(PAGE_IMPRESSION_SIGNAL, PAGE_IMPRESSION_SIGNAL_NAME, PAGE_IMPRESSION_SIGNAL_NAME + SIGNAL_TEMPLATE, platformId);
        var signalId = signalService.create(signal).getId();

        // non-soj platform fields
        var isViewed = field(signalId, IS_VIEWED, "Is view event from client tracking or not.", BOOLEAN, "true", LITERAL, true);
        var viewedTs = field(signalId, VIEWED_TS, "Viewed timestamp, derived from domain client tracking or Surface Tracking view events.",
                LONG, TIMESTAMP_EXPRESSION, JEXL, true).setIsCached(true);
        var exitExitedTs = field(signalId, EXITED_TS, "Exit timestamp, derived from Surface Tracking page view exit events.", LONG,
                TIMESTAMP_EXPRESSION, JEXL, true);
        var exitDwell = field(signalId, DWELL, "dwell time between view and exit time.", LONG, TIMESTAMP_EXPRESSION, JEXL, true);
        var serveServedTs = field(signalId, SERVED_TS, "Served timestamp, derived from Soj's server tracking events.", LONG,
                TIMESTAMP_EXPRESSION, JEXL, true);
        var serveIsServed = field(signalId, IS_SERVED, "Is serve event from SOJ server tracking or not.", BOOLEAN, "true", LITERAL, true);

        var entryTypeId = eventTypeLookupService.getByName(PAGE_VIEW_ENTRY).getId();
        var serveTypeId = eventTypeLookupService.getByName(PAGE_SERVE).getId();
        var clientViewTypeId = eventTypeLookupService.getByName(CLIENT_PAGE_VIEW).getId();

        fieldService.create(isViewed, Set.of(), Set.of(entryTypeId, clientViewTypeId));
        fieldService.create(viewedTs, Set.of(attrId1, attrId4), null); // attrId1 - PAGE_VIEW_ENTRY, attrId4 - CLIENT_PAGE_VIEW
        fieldService.create(exitExitedTs, Set.of(attrId2), null); // attrId2 - PAGE_VIEW_EXIT
        fieldService.create(exitDwell, Set.of(attrId2), null); // attrId2 - PAGE_VIEW_EXIT
        fieldService.create(serveServedTs, Set.of(attrId3), null); // attrId1 - PAGE_SERVE
        fieldService.create(serveIsServed, Set.of(), Set.of(serveTypeId));

        // soj platform fields
        createPlatformTags(signalId, List.of(pageViewEntry, pageViewExit, clientPageView, pageServe));

        return signalService.getByIdWithAssociationsRecursive(signalId);
    }

    private SignalTemplate onsiteClickSignal(Optional<List<TemplateFieldDefinition>> maybeFieldsConfig) {
        // events
        var moduleClick = eventTemplate(MODULE_CLICK, "Module click", "Module click event", ST,
                "event.eventPayload.activityType.startsWith(\"ACTN\") and [${MODULE_IDS}].contains(event.context.pageInteractionContext.moduleId)"
                        + " and [${CLICK_IDS}].contains(event.context.pageInteractionContext.linkId)", false);
        moduleClick = eventService.create(moduleClick);
        var moduleClickId = moduleClick.getId();

        var sojClick = eventTemplate(SOJ_CLICK, "SOJ click", "SOJ click event", SOJ,
                "[\"ACTN\"].contains(event.eventPayload.eventType) and [${MODULE_IDS}].contains(event.context.pageInteractionContext.moduleId)"
                        + " and [${CLICK_IDS}].contains(event.context.pageInteractionContext.linkId)", true);
        sojClick = eventService.create(sojClick);
        var sojClickId = sojClick.getId();

        var serviceCall = eventTemplate(SERVICE_CALL, "Service call", "Service call event", SOJ,
                "[${EVENT_TYPE}].contains(event.eventPayload.eventType) and [${PAGE_IDS}].contains(event.context.pageInteractionContext.pageId)", false);
        serviceCall = eventService.create(serviceCall);
        var serviceCallId = serviceCall.getId();

        // questions
        var moduleQuestion = metadataQuestion(QUESTION_MODULE, PROPERTY_MODULE_IDS, MODULE_IDS);
        var clickQuestion = metadataQuestion(QUESTION_CLICK, PROPERTY_CLICK_IDS, CLICK_IDS);
        var pageQuestion = metadataQuestion(QUESTION_SERVER_PAGE, PROPERTY_PAGE_IDS, PAGE_IDS);
        var repoQuestion = propertyQuestion(QUESTION_SERVER_GIT, STRING, PROPERTY_GIT_REPO);
        var surfaceQuestion = propertyQuestion(QUESTION_SURFACE_TYPE, STRING, PROPERTY_SURFACE_TYPE, SURFACE_TYPE);
        var eventTypeQuestion = expressionQuestion(QUESTION_EVENT_TYPE, STRING, EVENT_TYPE);

        questionService.createAll(Set.of(repoQuestion), Set.of(moduleClickId, sojClickId, serviceCallId));
        questionService.createAll(Set.of(moduleQuestion, clickQuestion, surfaceQuestion), Set.of(moduleClickId, sojClickId));
        questionService.createAll(Set.of(eventTypeQuestion, pageQuestion), Set.of(serviceCallId));

        val platformId = platformService.getPlatformId(CJS);

        var signal = signalTemplate(ONSITE_CLICK_SIGNAL, ONSITE_CLICK_SIGNAL_NAME, ONSITE_CLICK_SIGNAL_NAME + SIGNAL_TEMPLATE, platformId);
        var signalId = signalService.create(signal).getId();

        // attributes
        var attr1 = attribute(moduleClickId, TIMESTAMP, TIMESTAMP_EXPRESSION, LONG);
        var attr2 = attribute(sojClickId, TIMESTAMP, TIMESTAMP_EXPRESSION, LONG);
        var attr3 = attribute(serviceCallId, TIMESTAMP, TIMESTAMP_EXPRESSION, LONG);
        var attrId1 = attributeService.create(attr1).getId();
        var attrId2 = attributeService.create(attr2).getId();
        var attrId3 = attributeService.create(attr3).getId();

        // soj platform fields
        createPlatformTags(signalId, List.of(moduleClick, sojClick, serviceCall));

        // non-soj platform fields
        var clickedTs = field(signalId, CLICKED_TS, CLICKED_TS_DESC, LONG, TIMESTAMP_EXPRESSION, JEXL, true);
        fieldService.create(clickedTs, Set.of(attrId1, attrId2, attrId3), null);

        return signalService.getByIdWithAssociationsRecursive(signalId);
    }

    private SignalTemplate offsiteClickSignal(Optional<List<TemplateFieldDefinition>> maybeFieldsConfig) {
        // events
        var offsiteEvent = eventTemplate(OFFSITE_EVENT, "Offsite event", "Offsite event", OFFSITE,
                "[2547208].contains(event.context.pageInteractionContext.pageId) and '${CHANNEL_ID}'.equals(event.eventPayload.eventProperties.chnl)",
                true);
        offsiteEvent = eventService.create(offsiteEvent);
        var offsiteEventId = offsiteEvent.getId();

        // questions
        var channelQuestion = expressionQuestion("What is the offsite channel id?", INTEGER, CHANNEL_ID);
        var repoQuestion = propertyQuestion(QUESTION_SERVER_GIT, STRING, PROPERTY_GIT_REPO);

        questionService.createAll(Set.of(channelQuestion, repoQuestion), Set.of(offsiteEventId));

        val platformId = platformService.getPlatformId(CJS);

        var signal = signalTemplate(OFFSITE_CLICK_SIGNAL, OFFSITE_CLICK_SIGNAL_NAME, OFFSITE_CLICK_SIGNAL_NAME + SIGNAL_TEMPLATE, platformId);
        var signalId = signalService.create(signal).getId();

        // attributes
        var attr1 = attribute(offsiteEventId, TIMESTAMP, TIMESTAMP_EXPRESSION, LONG);
        var attrId1 = attributeService.create(attr1).getId();

        // soj platform fields
        createPlatformTags(signalId, List.of(offsiteEvent));

        // non-soj platform fields
        var clickedTs = field(signalId, CLICKED_TS, CLICKED_TS_DESC, LONG, TIMESTAMP_EXPRESSION, JEXL, true);
        fieldService.create(clickedTs, Set.of(attrId1), null);

        return signalService.getByIdWithAssociationsRecursive(signalId);
    }

    private SignalTemplate moduleImpressionSignal(Optional<List<TemplateFieldDefinition>> maybeFieldsConfig) {
        // events
        var moduleView = eventTemplate(MODULE_VIEW, "Module view", "Module view event", ST,
                "[\"VIEW.MOD_VIEW\"].contains(event.eventPayload.activityType) and [${MODULE_IDS}].contains(event.context.pageInteractionContext.moduleId)",
                true);
        moduleView = eventService.create(moduleView);
        var moduleViewId = moduleView.getId();

        // questions
        var moduleQuestion = metadataQuestion(QUESTION_MODULE, PROPERTY_MODULE_IDS, MODULE_IDS);
        var repoQuestion = propertyQuestion(QUESTION_CLIENT_GIT, STRING, PROPERTY_GIT_REPO);
        var surfaceQuestion = propertyQuestion(QUESTION_SURFACE_TYPE, STRING, PROPERTY_SURFACE_TYPE, SURFACE_TYPE);

        questionService.createAll(Set.of(moduleQuestion, repoQuestion, surfaceQuestion), Set.of(moduleViewId));

        // attributes
        var attr1 = attribute(moduleViewId, "duration", "event.getEventPayload().getEventProperties().get(\"duration\")", STRING);
        var attr2 = attribute(moduleViewId, TIMESTAMP, TIMESTAMP_EXPRESSION, STRING);
        var attrId1 = attributeService.create(attr1).getId();
        var attrId2 = attributeService.create(attr2).getId();

        val platformId = platformService.getPlatformId(CJS);

        var signal = signalTemplate(MODULE_IMPRESSION_SIGNAL, MODULE_IMPRESSION_SIGNAL_NAME, MODULE_IMPRESSION_SIGNAL_NAME + SIGNAL_TEMPLATE, platformId);
        var signalId = signalService.create(signal).getId();

        // soj platform fields
        createPlatformTags(signalId, List.of(moduleView));

        // non-soj platform fields
        var dwell = field(signalId, DWELL, "dwell time between view and exit time.", LONG,
                "event.getEventPayload().getEventProperties().get(\"duration\")", JEXL, true);
        var viewedTs = field(signalId, VIEWED_TS, "Viewed timestamp, derived from domain client tracking or Surface Tracking view events.",
                LONG, TIMESTAMP_EXPRESSION, JEXL, true);

        fieldService.create(dwell, Set.of(attrId1), null);
        fieldService.create(viewedTs, Set.of(attrId2), null);

        return signalService.getByIdWithAssociationsRecursive(signalId);
    }

    private SignalTemplate businessOutcomeSignal(Optional<List<TemplateFieldDefinition>> maybeFieldsConfig) {
        // events
        var roi = eventTemplate(ROI_EVENT, "Business Outcome ROI event", "Business Outcome ROI event", ROI,
                "event.eventPayload.activityType.equals(\"${BUSINESS_OUTCOME_ACTION}\")", true);
        roi = eventService.create(roi);
        var roiId = roi.getId();

        // questions
        var actionQuestion = expressionQuestion("What is the business outcome action?", STRING, BUSINESS_OUTCOME_ACTION);
        var repoQuestion = propertyQuestion(QUESTION_CLIENT_GIT, STRING, PROPERTY_GIT_REPO);
        questionService.createAll(Set.of(actionQuestion, repoQuestion), Set.of(roiId));

        // attributes
        var attr1 = attribute(roiId, TIMESTAMP, TIMESTAMP_EXPRESSION, STRING);
        attr1 = attributeService.create(attr1);
        var attrId1 = attr1.getId();

        val platformId = platformService.getPlatformId(CJS);

        var signal = signalTemplate(BUSINESS_OUTCOME_SIGNAL, BUSINESS_OUTCOME_SIGNAL_NAME, BUSINESS_OUTCOME_SIGNAL_NAME + SIGNAL_TEMPLATE, platformId);
        var signalId = signalService.create(signal).getId();

        // fields
        var recordTs = field(signalId, RECORD_TS, "Record timestamp for ROI events.", LONG, TIMESTAMP_EXPRESSION, JEXL, true);
        fieldService.create(recordTs, Set.of(attrId1), null);

        return signalService.getByIdWithAssociationsRecursive(signalId);
    }

    private SignalTemplate ejsSignal(Optional<List<TemplateFieldDefinition>> maybeFieldsConfig) {
        // events
        var csEvent = eventTemplate(CSEVENT, "CS event", "CS event", CSTECH,
                "[${CS_EVENT_IDS}].contains(event.eventPayload.eventProperties.csEventId) "
                        + "and [${CS_EVENT_NAME}].contains(event.eventPayload.eventProperties.csEventName) "
                        + "and [${CS_APP_NAME}].contains(event.eventPayload.eventProperties.csAppName)", true);
        csEvent = eventService.create(csEvent);
        var csEventId = csEvent.getId();

        // questions
        var eventIdQuestion = expressionQuestion("What is the CS event id(s)?", STRING, CS_EVENT_IDS);
        var eventNameQuestion = expressionQuestion("What is the CS event name(s)?", STRING, CS_EVENT_NAME);
        var appNameQuestion = expressionQuestion("What is the CS app name(s)?", STRING, CS_APP_NAME);
        var repoQuestion = propertyQuestion(QUESTION_SERVER_GIT, STRING, PROPERTY_GIT_REPO);

        questionService.createAll(Set.of(eventIdQuestion, eventNameQuestion, appNameQuestion, repoQuestion), Set.of(csEventId));

        val platformId = platformService.getPlatformId(CJS);

        var signal = signalTemplate(EJS_SIGNAL, EJS_SIGNAL_NAME, EJS_SIGNAL_NAME + SIGNAL_TEMPLATE, platformId);
        var signalId = signalService.create(signal).getId();

        // attributes
        var attr = attribute(csEventId, TIMESTAMP, TIMESTAMP_EXPRESSION, STRING);
        var attrId = attributeService.create(attr).getId();

        // platform fields
        var clickedTs = field(signalId, CLICKED_TS, CLICKED_TS_DESC, LONG, TIMESTAMP_EXPRESSION, JEXL, true);
        var eventTypeId = eventTypeLookupService.getByName(CSEVENT).getId();
        fieldService.create(clickedTs, Set.of(attrId), Set.of(eventTypeId));

        return signalService.getByIdWithAssociationsRecursive(signalId);
    }

    private SignalTemplate transactionSignal(Optional<List<TemplateFieldDefinition>> maybeFieldsConfig) {
        var transactionSource = eventTemplate(TRANSACTION_EVENT, "Transaction event", "Transaction processing event",
                TRANSACTION_EBAYLIVE_ATTRIBUTED_STREAM, "event?.eventInfo.source == ${TRANSACTION_SOURCE_TYPE}", true);
        transactionSource = eventService.create(transactionSource);
        var transactionSourceId = transactionSource.getId();

        // Create template questions for transaction source: 'TRANSACTION_OMS_ORDER_CONFIRM' or 'TRANSACTION_EBAYLIVE_ATTRIBUTED_STREAM'
        var transactionSourceQuestion = expressionQuestion(QUESTION_TRANSACTION_SOURCE, STRING, TRANSACTION_SOURCE_TYPE);
        questionService.create(transactionSourceQuestion, Set.of(transactionSourceId));

        var signal = signalTemplate(TRANSACTION_SIGNAL, TRANSACTION_SIGNAL_NAME,
                TRANSACTION_SIGNAL_NAME + SIGNAL_TEMPLATE, platformService.getPlatformId(TRANSACTION_SIGNAL));
        var signalId = signalService.create(signal).getId();

        // attributes
        var attr = attribute(transactionSourceId, TIMESTAMP, EVENT_TIMESTAMP_EXPRESSION, STRING);
        var attrId = attributeService.create(attr).getId();

        //fields
        var sourceTimestamp = field(signalId, SIGNAL_GDS_SOURCE_TS, "GDS source time stamp", LONG, SIGNAL_GDS_SOURCE_TS, JEXL, true);
        var transactionTypeId = eventTypeLookupService.getByName(TRANSACTION_EVENT).getId();
        fieldService.create(sourceTimestamp, Set.of(attrId), Set.of(transactionTypeId));

        transactionSignalEnrichmentService.enrichSignal(signalId, transactionSourceId, transactionTypeId, maybeFieldsConfig);

        return signalService.getByIdWithAssociationsRecursive(signalId);
    }

    private void createPlatformTags(long signalId, List<EventTemplate> events) {
        val tags = sojPlatformTagRepository.findAll();

        for (val tag : tags) {
            if (!NON_SOJ_PLATFORM_FIELDS.contains(tag.getSojName())) {
                // Only create soj platform fields, it doesn't make sense to have viewedTs, exitedTs and servedTs in single page impression event.
                // But we still keep those non-soj platform fields in the soj_platform_tag table for signal migration.
                var attrIds = events.stream()
                        .map(evt -> attribute(evt.getId(), tag))
                        .map(attr -> attributeService.create(attr).getId())
                        .collect(Collectors.toSet());
                val field = sojPlatformField(signalId, tag);
                fieldService.create(field, attrIds, null);
            }
        }
    }

    public static SignalTemplate signalTemplate(String type, String name, String desc, Long platformId) {
        return SignalTemplate.builder()
                .name(name)
                .description(desc)
                .type(type)
                .platformId(platformId)
                .completionStatus(COMPLETED)
                .createBy(TRACKING_MODERATOR)
                .updateBy(TRACKING_MODERATOR)
                .build();
    }

    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    public static EventTemplate eventTemplate(String type, String name, String desc, EventSource source, String expression, boolean isMandatory) {
        return EventTemplate.builder()
                .name(name)
                .description(desc)
                .type(type)
                .source(source)
                .expression(expression)
                .expressionType(JEXL)
                .isMandatory(isMandatory)
                .createBy(TRACKING_MODERATOR)
                .updateBy(TRACKING_MODERATOR)
                .build();
    }

    public static AttributeTemplate attribute(long eventId, String tag, String schemaPath, JavaType javaType) {
        return AttributeTemplate.builder()
                .eventTemplateId(eventId)
                .tag(tag)
                .description(tag)
                .javaType(javaType)
                .schemaPath(schemaPath)
                .createBy(TRACKING_MODERATOR)
                .updateBy(TRACKING_MODERATOR)
                .build();
    }

    public static AttributeTemplate attribute(long eventId, SojPlatformTag tag) {
        return AttributeTemplate.builder()
                .eventTemplateId(eventId)
                .tag(tag.getSojName())
                .description(tag.getDescription())
                .javaType(JavaType.fromValue(tag.getDataType()))
                .schemaPath(tag.getSchemaPath())
                .createBy(TRACKING_MODERATOR)
                .updateBy(TRACKING_MODERATOR)
                .build();
    }

    public static FieldTemplate sojPlatformField(long signalId, SojPlatformTag tag) {
        val javaType = JavaType.fromValue(tag.getDataType());
        return FieldTemplate.builder()
                .signalTemplateId(signalId)
                .name(tag.getName())
                .tag(tag.getSojName())
                .description(tag.getDescription())
                .javaType(javaType)
                .avroSchema(javaType.toSchema())
                .isMandatory(true)
                .expression(tag.getSchemaPath())
                .expressionType(JEXL)
                .isCached(false)
                .createBy(TRACKING_MODERATOR)
                .updateBy(TRACKING_MODERATOR)
                .build();
    }

    public static FieldTemplate field(long signalId, String name, String description, JavaType javaType,
                                      String expression, ExpressionType expressionType, boolean isMandatory) {
        return FieldTemplate.builder()
                .signalTemplateId(signalId)
                .name(name)
                .tag(name)
                .description(description)
                .javaType(javaType)
                .avroSchema(javaType.toSchema())
                .expression(expression)
                .expressionType(expressionType)
                .isMandatory(isMandatory)
                .isCached(false)
                .createBy(TRACKING_MODERATOR)
                .updateBy(TRACKING_MODERATOR)
                .build();
    }

    private TemplateQuestion propertyQuestion(String question, JavaType answerJavaType, String answerPropertyName) {
        return templateQuestion(question, false, PROPERTY_SETTER_TYPE, answerJavaType, answerPropertyName, null);
    }

    private TemplateQuestion propertyQuestion(String question, JavaType answerJavaType, String answerPropertyName, AnswerPropertyPlaceholder placeholder) {
        return templateQuestion(question, false, PROPERTY_SETTER_TYPE, answerJavaType, answerPropertyName, placeholder);
    }

    private TemplateQuestion expressionQuestion(String question, JavaType answerJavaType, AnswerPropertyPlaceholder placeholder) {
        return templateQuestion(question, false, EXPRESSION_SETTER_TYPE, answerJavaType, null, placeholder);
    }

    private TemplateQuestion metadataQuestion(String question, String answerPropertyName, AnswerPropertyPlaceholder placeholder) {
        return templateQuestion(question, true, METADATA_SETTER_TYPE, LONG, answerPropertyName, placeholder);
    }

    private TemplateQuestion templateQuestion(String question, boolean isList, Class<? extends UserAnswerSetter> propertySetterClass,
                                              JavaType answerJavaType, String answerPropertyName, AnswerPropertyPlaceholder placeholder) {
        return TemplateQuestion.builder()
                .question(question)
                .description(question)
                .isList(isList)
                .isMandatory(true)
                .answerJavaType(answerJavaType)
                .answerPropertyName(answerPropertyName)
                .answerPropertyPlaceholder(placeholder)
                .answerPropertySetterClass(propertySetterClass.getSimpleName())
                .createBy(TRACKING_MODERATOR)
                .updateBy(TRACKING_MODERATOR)
                .build();
    }
}
