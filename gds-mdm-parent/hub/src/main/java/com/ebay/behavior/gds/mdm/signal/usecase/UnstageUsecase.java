package com.ebay.behavior.gds.mdm.signal.usecase;

import com.ebay.behavior.gds.mdm.common.config.UdcConfiguration;
import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.model.ExpressionType;
import com.ebay.behavior.gds.mdm.common.model.JavaType;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.signal.common.model.Attribute;
import com.ebay.behavior.gds.mdm.signal.common.model.AttributeTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.Event;
import com.ebay.behavior.gds.mdm.signal.common.model.EventTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.Field;
import com.ebay.behavior.gds.mdm.signal.common.model.Signal;
import com.ebay.behavior.gds.mdm.signal.common.model.TemplateQuestion;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstageRequest;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedAttribute;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.repository.UnstagedEventRepository;
import com.ebay.behavior.gds.mdm.signal.service.BusinessFieldService;
import com.ebay.behavior.gds.mdm.signal.service.EventTemplateService;
import com.ebay.behavior.gds.mdm.signal.service.PlanService;
import com.ebay.behavior.gds.mdm.signal.service.SignalTemplateService;
import com.ebay.behavior.gds.mdm.signal.service.TemplateQuestionService;
import com.ebay.behavior.gds.mdm.signal.service.TemplateService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedAttributeService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedEventService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedFieldService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedSignalService;
import com.ebay.behavior.gds.mdm.signal.service.userAnswer.UserAnswerPropertySetter;
import com.ebay.behavior.gds.mdm.signal.service.userAnswer.UserAnswerSetter;
import com.ebay.behavior.gds.mdm.signal.util.ValidationUtils;
import com.ebay.com.google.common.annotations.VisibleForTesting;

import io.vavr.Tuple;
import io.vavr.Tuple3;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Equator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.ebay.behavior.gds.mdm.common.model.Model.COMMA;
import static com.ebay.behavior.gds.mdm.common.util.ServiceConstants.DEFAULT_RETRY_BACKOFF;
import static com.ebay.behavior.gds.mdm.common.util.ServiceConstants.UNDERSCORE;
import static com.ebay.behavior.gds.mdm.signal.util.ServiceUtils.copyAttributeProperties;
import static com.ebay.behavior.gds.mdm.signal.util.ServiceUtils.copyEventProperties;
import static com.ebay.behavior.gds.mdm.signal.util.ServiceUtils.copyFieldProperties;
import static com.ebay.behavior.gds.mdm.signal.util.ServiceUtils.copySignalProperties;
import static java.util.Collections.emptyMap;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

/**
 * This usecase creates an Unstaged Signal, with all its related events, attributes and fields.
 */
@Service
@Validated
@Slf4j
@SuppressWarnings({"PMD.TooManyMethods", "PMD.CognitiveComplexity", "PMD.GodClass"})
public class UnstageUsecase {

    private static final Pattern[] FIELD_PATTERNS = {
            Pattern.compile("event\\.eventPayload\\.([a-zA-Z0-9_]+)"),
            Pattern.compile("event\\.eventPayload\\.eventProperties\\.([a-zA-Z0-9_]+)"),
            Pattern.compile("event\\.getEventPayload\\(\\).get([a-zA-Z0-9_]+)\\(\\)"),
            Pattern.compile("event\\.getEventPayload\\(\\).getEventProperties\\(\\)\\.get\\(\"([a-zA-Z0-9_]+)\"\\)"),
            Pattern.compile("event\\.eventPayload\\.([a-zA-Z0-9_]+)\\s+[\\-]\\s+viewedTs")
    };

    private final Equator<TemplateQuestion> questionEquator = new Equator<>() {
        @Override
        public boolean equate(TemplateQuestion q1, TemplateQuestion q2) {
            return Objects.nonNull(q1.getQuestion()) && q1.getQuestion().equals(q2.getQuestion())
                    && Objects.nonNull(q1.getId()) && q1.getId().equals(q2.getId());
        }

        @Override
        public int hash(TemplateQuestion question) {
            return (question.getQuestion() + '_'
                    + Optional.ofNullable(question.getAnswerJavaType())
                    .map(JavaType::getValue)
                    .orElse("null"))
                    .hashCode();
        }
    };

    @Autowired
    private UdcConfiguration udcConfiguration;

    @Autowired
    private PlanService planService;

    @Autowired
    private UnstagedSignalService signalService;

    @Autowired
    private UnstagedEventService eventService;

    @Autowired
    private UnstagedAttributeService attributeService;

    @Autowired
    private UnstagedFieldService fieldService;

    @Autowired
    private TemplateQuestionService questionService;

    @Autowired
    private BusinessFieldService businessFieldService;

    @Autowired
    private SignalTemplateService signalTemplateService;

    @Autowired
    private EventTemplateService eventTemplateService;

    @Autowired
    private UnstagedEventRepository unstagedEventRepository;

    @Autowired
    private List<UserAnswerSetter> userAnswerSetters;

    private Map<String, UserAnswerSetter> userAnswerSettersPerType;

    private static Collection<TemplateQuestion> filterEventQuestions(UnstagedEvent event, Collection<TemplateQuestion> questions) {
        return questions.stream()
                .filter(question -> question.getEvents().stream()
                        .map(EventTemplate::getId)
                        .collect(toSet())
                        .contains(event.getEventTemplateSourceId()))
                .toList();
    }

    @PostConstruct
    private void initUserAnswerSetterMap() {
        userAnswerSettersPerType = userAnswerSetters.stream()
                .collect(Collectors.collectingAndThen(
                        toMap(setter -> setter.getClass().getSimpleName(), Function.identity()),
                        Map::copyOf));
    }

    /**
     * Create an Unstaged Signal from a signalTemplate. Copy all the related events, attributes and fields.
     *
     * @param request An UnstageRequest encapsulating method parameters.
     * @return The created Signal.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UnstagedSignal copySignalFromTemplate(@NotNull @Valid UnstageRequest request) {
        // validate
        val enrichedAnsweredQuestions = validateUserAnswers(signalTemplateService, request); // enriched question has events and a user answer
        val srcSignalId = request.getSrcEntityId();
        validateTemplateSignal(srcSignalId, signalTemplateService);

        // Get signal with all associations. So no DB reads from this point on.
        val srcSignal = signalTemplateService.getByIdWithAssociationsRecursive(srcSignalId);
        val dstSignalId = copySignal(request, srcSignal, UnstagedSignal::assignTemplateSource,
                UnstagedEvent::setEventTemplateSourceId, false, enrichedAnsweredQuestions);

        return signalService.getById(dstSignalId);
    }

    /**
     * Create an UnstagedEvent from a eventTemplate and associate it with corresponding signal.
     * Copy all the related attributes.
     *
     * @param request An UnstageRequest encapsulating method parameters.
     * @return The created UnstagedEvent.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UnstagedEvent copyEventFromTemplate(@NotNull @Valid UnstageRequest request) {
        val signalId = request.getParentId();
        val signal = signalService.getByIdAndLatestVersion(signalId);

        // validate
        planService.getById(signal.getPlanId());
        Validate.isTrue(signal.getEnvironment().equals(Environment.UNSTAGED), "Associated Signal must be UNSTAGED");
        val enrichedAnsweredQuestions = validateUserAnswers(eventTemplateService, request); // enriched question has events and a user answer

        val eventTemplateId = request.getSrcEntityId();
        val eventTemplate = eventTemplateService.getByIdWithAssociations(eventTemplateId);
        val fieldTemplates = eventTemplateService.getFields(eventTemplate);

        // we also need a field templates that are not associated with any (a.k.a. signal field templates), but still associated with the event type
        if (!fieldTemplates.isEmpty()) {
            val signalTemplateId = fieldTemplates.iterator().next().getSignalTemplateId();
            val signalFieldTemplates = signalTemplateService.getFields(signalTemplateId).stream()
                    .filter(field -> field.getAttributes().isEmpty())
                    .filter(field -> field.getEventTypes().stream()
                            .anyMatch(lookup -> lookup.getName().equals(eventTemplate.getType())))
                    .collect(toSet());
            fieldTemplates.addAll(signalFieldTemplates);
        }

        val event = copyEvent(request, eventTemplate, fieldTemplates, signal.getSignalId(), UnstagedEvent::setEventTemplateSourceId, enrichedAnsweredQuestions);
        return eventService.getById(event.getId());
    }

    /**
     * Create an UnstagedSignal from another UnstagedSignal.
     * Copy all the related events, attributes and fields.
     * Increment the original signal version, keeping the original signal id.
     *
     * @param request An UnstageRequest encapsulating method parameters.
     * @return The created UnstagedSignal.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UnstagedSignal copySignalFromUnstaged(@NotNull @Valid UnstageRequest request) {
        // validate
        Validate.isTrue(Objects.nonNull(request.getSrcVersion()), "Version cannot be null for copy from an unstaged Signal");
        val srcSignalId = VersionedId.of(request.getSrcEntityId(), request.getSrcVersion());
        validateUnstagedSignal(srcSignalId, signalService);

        // Get signal with all associations. So no DB reads from this point on.
        val srcSignal = signalService.getByIdWithAssociationsRecursive(srcSignalId);
        val dstSignalId = copySignal(request, srcSignal, UnstagedSignal::assignUnstagedSource, UnstagedEvent::setEventSourceId, true, List.of());

        return signalService.getById(dstSignalId);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @Retryable(retryFor = CannotAcquireLockException.class, backoff = @Backoff(delay = DEFAULT_RETRY_BACKOFF))
    public UnstagedSignal createWithAssociations(@NotNull UnstagedSignal srcSignal) {
        Validate.notEmpty(srcSignal.getEvents(), "Signal events can not be empty");
        Validate.notEmpty(srcSignal.getFields(), "Signal fields can not be empty");
        Validate.isTrue(Objects.nonNull(srcSignal.getVersion()), "Signal version can not be null for creating an unstaged signal with associations");
        Validate.isTrue(Objects.nonNull(srcSignal.getLegacyId()), "Signal legacyId can not be null for creating an unstaged signal with associations");

        val maybeSignalDetails = getSignalIdFromUnstaged(srcSignal);
        val srcEvents = new HashSet<>(srcSignal.getEvents());
        val srcFields = new HashSet<>(srcSignal.getFields());

        srcSignal.getFields().clear();
        srcSignal.getEvents().clear();

        // create signal if not present
        maybeSignalDetails.ifPresent(tuple -> {
            srcSignal.setId(tuple._1);
            srcSignal.setRevision(tuple._3);
        });

        val maybeVersionedId = maybeSignalDetails.flatMap(Tuple3::_2);
        val signalVersion = maybeVersionedId.orElseGet(() -> signalService.create(srcSignal).getSignalId());
        val dstSignal = signalService.getByIdWithAssociations(signalVersion);

        // create events and store the events into map with eventType as key
        val eventTypeToIds = createMigrationEvents(dstSignal, srcEvents);
        createMigrationAttributesAndFields(dstSignal, srcFields, eventTypeToIds);

        return dstSignal;
    }

    /**
     * @return A Tuple with signalId, version and latest revision.
     */
    private Optional<Tuple3<Long, Optional<VersionedId>, Integer>> getSignalIdFromUnstaged(UnstagedSignal srcSignal) {
        val signals = signalService.getByLegacyId(srcSignal.getLegacyId());
        if (CollectionUtils.isEmpty(signals)) {
            return Optional.empty();
        }

        val id = signals.get(0).getId();
        val versionToSignals = signals.stream().collect(toMap(this::getVersionKey, Function.identity()));
        val currVersion = getVersionKey(srcSignal);

        val latestRevision = signals.stream()
                .map(UnstagedSignal::getRevision)
                .max(Integer::compare)
                .get();

        val maybeSignalId = Optional.ofNullable(versionToSignals.get(currVersion))
                .map(signal -> VersionedId.of(id, signal.getVersion()));

        return Optional.of(Tuple.of(id, maybeSignalId, latestRevision));
    }

    private String getVersionKey(UnstagedSignal signal) {
        return String.join("_", signal.getLegacyId(), signal.getVersion().toString());
    }

    private void createMigrationAttributesAndFields(UnstagedSignal destSignal, Set<UnstagedField> srcFields, Map<String, Set<Long>> eventTypeToIds) {
        if (CollectionUtils.isEmpty(srcFields)) {
            return;
        }
        val signalId = destSignal.getSignalId();

        val fieldNameToIds = CollectionUtils.isEmpty(destSignal.getFields()) ? emptyMap() :
                destSignal.getFields().stream().collect(toMap(UnstagedField::getName, UnstagedField::getId));

        srcFields.forEach(field -> {
            if (!fieldNameToIds.containsKey(field.getName())) {
                field.setSignalVersion(signalId.getVersion());
                field.setSignalId(signalId.getId());

                val tag = extractTag(field);
                field.setTag(tag);
                if (ExpressionType.LITERAL.equals(field.getExpressionType())) {
                    fieldService.create(field, Set.of());
                    return;
                }

                // create attributes from the field
                val attributeIds = new HashSet<Long>();
                val eventTypes = field.getEventTypes().split(COMMA);
                for (String eventType : eventTypes) {
                    if (!eventTypeToIds.containsKey(eventType)) {
                        continue;
                    }
                    for (val eventId : eventTypeToIds.get(eventType)) {
                        val attribute = toUnstagedAttribute(field, tag);
                        attribute.setEventId(eventId);
                        val createdAttribute = attributeService.create(attribute);
                        attributeIds.add(createdAttribute.getId());
                    }
                }
                fieldService.create(field, attributeIds);
            }
        });
    }

    private Map<String, Set<Long>> createMigrationEvents(UnstagedSignal dstSignal, Set<UnstagedEvent> srcEvents) {
        if (CollectionUtils.isEmpty(srcEvents)) {
            return Map.of();
        }

        Map<String, Set<Long>> eventTypeToIds = dstSignal.getEvents().stream()
                .collect(groupingBy(UnstagedEvent::getType, mapping(UnstagedEvent::getId, toCollection(HashSet::new))));
        val eventKeyToIds = dstSignal.getEvents().stream()
                .collect(toMap(this::getEventKey, UnstagedEvent::getId));

        srcEvents.forEach(event -> {
            val eventKey = getEventKey(event);
            if (!eventKeyToIds.containsKey(eventKey)) {
                val created = eventService.create(event);
                signalService.createEventMapping(dstSignal.getSignalId(), created.getId());
                val ids = eventTypeToIds.getOrDefault(event.getType(), new HashSet<>());
                ids.add(created.getId());
                eventTypeToIds.put(event.getType(), ids);
            }
        });

        return eventTypeToIds;
    }

    private String getEventKey(UnstagedEvent event) {
        return String.join(UNDERSCORE, event.getName(), event.getType());
    }

    private VersionedId copySignal(UnstageRequest request, Signal srcSignal,
                                   BiConsumer<UnstagedSignal, VersionedId> srcSignalSetter,
                                   BiConsumer<UnstagedEvent, Long> srcEventIdSetter,
                                   boolean keepId, Collection<TemplateQuestion> answeredQuestions) {
        // create a signal
        val planId = request.getParentId();
        val plan = planService.getById(planId);

        var dstSignal = toUnstagedSignal(srcSignal, planId).toBuilder()
                .name(request.getName())
                .description(request.getDescription())
                .domain(plan.getDomain())
                .owners(plan.getOwners())
                .environment(Environment.UNSTAGED)
                .dataSource(udcConfiguration.getDataSource())
                .build();

        val uuidGeneratorType = request.getUuidGeneratorType();
        if (uuidGeneratorType != null) {
            dstSignal.setUuidGeneratorType(uuidGeneratorType.name());
            dstSignal.setUuidGeneratorExpression(request.getUuidGeneratorExpression());
        }

        if (keepId) {
            dstSignal.setId(srcSignal.getId());
            dstSignal.setVersion(srcSignal.getSignalId().getVersion() + 1);
        }

        srcSignalSetter.accept(dstSignal, srcSignal.getSignalId());
        dstSignal = signalService.create(dstSignal);

        // create events
        var srcEvents = srcSignal.getEvents();
        if (!keepId) { // If create signal from template, only create mandatory events.
            srcEvents = srcEvents.stream()
                    .filter(event -> ((EventTemplate) event).getIsMandatory())
                    .collect(toSet());
        }
        val eventMap = createEventMappings(srcEvents, srcEventIdSetter); // eventMap: srcId -> dstId

        // create platform attributes
        val attributeMap = createAttributeMappings(srcEvents, eventMap); // attributeMap: srcId -> dstId

        // create platform fields
        var srcFields = srcSignal.getFields();
        val srcEventTypes = srcEvents.stream().map(Event::getType).collect(toSet());
        if (!keepId) { // If create signal from template, only create fields for mandatory events.
            srcFields = srcFields.stream()
                    .filter(field -> Set.of(field.getEventTypesAsString().split(COMMA)).stream().anyMatch(srcEventTypes::contains))
                    .collect(toSet());
        }
        val srcEventTypesString = srcEventTypes.stream().distinct().collect(Collectors.joining(COMMA));
        createFields(srcFields, dstSignal.getSignalId(), attributeMap, keepId, srcEventTypesString);

        // enrich the events with user answers
        val dstSignalId = dstSignal.getSignalId();
        if (!answeredQuestions.isEmpty()) {
            setUserAnswers(dstSignalId, answeredQuestions);
        }

        // create business fields and attributes when created from template (since there are no business fields in the template)
        if (!keepId) {
            val dstEvents = eventService.findAllById(new HashSet<>(eventMap.values()));
            for (val event : dstEvents) {
                businessFieldService.createBusinessFields(dstSignal.getSignalId(), event.getId(), false);
            }
        }

        return dstSignal.getSignalId();
    }

    // This method can be used only for copy event from template flow.
    private UnstagedEvent copyEvent(UnstageRequest request,
                                    Event srcEvent, Set<? extends Field> srcFields,
                                    VersionedId dstSignalId,
                                    BiConsumer<UnstagedEvent, Long> srcEventIdSetter,
                                    Collection<TemplateQuestion> answeredQuestions) {
        // create event
        val dstEvent = toUnstagedEvent(srcEvent);
        dstEvent.setName(request.getName());
        dstEvent.setDescription(request.getDescription());
        srcEventIdSetter.accept(dstEvent, srcEvent.getId());

        val dstEventId = eventService.create(dstEvent).getId();
        val eventMap = Map.of(srcEvent.getId(), dstEventId);

        // create attributes
        var attributeMap = createAttributeMappings(Set.of(srcEvent), eventMap); // attributeMap: srcId -> dstId

        // create fields from original attribute <-> field template mapping.
        createOrReuseFields(dstEvent.getType(), srcFields, dstSignalId, attributeMap);

        // enrich the events with user answers
        updateAnswersForEvent(dstEvent, answeredQuestions);

        // create business fields & attributes
        businessFieldService.createBusinessFields(dstSignalId, dstEvent.getId(), true);

        return eventService.getById(dstEventId);
    }

    private void validateUnstagedSignal(VersionedId signalId, UnstagedSignalService signalService) {
        var signal = signalService.getByIdWithAssociations(signalId);
        ValidationUtils.validateCompleted(signal);
    }

    private void validateTemplateSignal(long signalId, SignalTemplateService signalService) {
        var signal = signalService.getByIdWithAssociations(signalId);
        ValidationUtils.validateCompleted(signal);
    }

    private Map<Long, Long> createEventMappings(Collection<? extends Event> srcEvents, BiConsumer<UnstagedEvent, Long> dstIdSetter) {
        var eventMap = new HashMap<Long, Long>();
        srcEvents.forEach(srcEvent -> {
            val dstEvent = toUnstagedEvent(srcEvent);
            dstIdSetter.accept(dstEvent, srcEvent.getId());
            val dstEventId = eventService.create(dstEvent).getId();
            eventMap.put(srcEvent.getId(), dstEventId);
        });
        return eventMap;
    }

    private Map<Long, Long> createAttributeMappings(Collection<? extends Event> srcEvents, Map<Long, Long> eventMap) {
        var attributeMap = new HashMap<Long, Long>();
        srcEvents.stream().flatMap(srcEvent -> srcEvent.getAttributes().stream())
                .forEach(srcAttr -> {
                    val dstAttr = toUnstagedAttribute(srcAttr, eventMap);
                    val dstAttrId = attributeService.create(dstAttr).getId();
                    attributeMap.put(srcAttr.getId(), dstAttrId);
                });
        return attributeMap;
    }

    private void createFields(Collection<? extends Field> fields, VersionedId dstSignalId, Map<Long, Long> attributeMap, boolean keepId, String srcEventTypes) {
        fields.forEach(srcField -> {
            var srcAttributes = srcField.getAttributes();
            if (!keepId) { // If create signal from template, only create fields for mandatory events.
                srcAttributes = srcAttributes.stream()
                        .filter(attr -> ((AttributeTemplate) attr).getEvent().getIsMandatory())
                        .collect(toSet());
            }
            val dstAttributeIds = toUnstagedAttributeIds(srcAttributes, attributeMap, true);

            val dstField = toUnstagedField(srcField, dstSignalId);
            if (!keepId) { // If create signal from template, only add mandatory event types.
                dstField.setEventTypes(srcEventTypes);
            }
            fieldService.create(dstField, dstAttributeIds);
        });
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private void createOrReuseFields(String eventType, Collection<? extends Field> srcFields, VersionedId dstSignalId, Map<Long, Long> attributeMap) {
        for (Field srcField : srcFields) {
            val srcFieldAttributes = emptyIfNull(srcField.getAttributes());
            if (srcFieldAttributes.isEmpty()) {
                createOrReuseSignalField(eventType, dstSignalId, srcField);
                continue;
            }

            val dstAttributeIds = toUnstagedAttributeIds(srcFieldAttributes, attributeMap, false);
            if (srcFieldAttributes.size() == dstAttributeIds.size()) {
                val dstField = toUnstagedField(srcField, dstSignalId);
                dstField.setEventTypes(eventType); // If a new field is added during an event addition, that means the only event type is the one being added
                fieldService.create(dstField, dstAttributeIds);
                continue;
            }

            createOrReuseEventField(eventType, dstSignalId, srcField, dstAttributeIds);
        }
    }

    private void createOrReuseEventField(String eventType, VersionedId dstSignalId, Field srcField, Set<Long> dstAttributeIds) {
        val sameTagFields = emptyIfNull(signalService.getFields(dstSignalId)).stream()
                .filter(field -> field.getTag().equals(srcField.getTag()))
                .collect(toSet());

        if (sameTagFields.isEmpty()) {
            val dstField = toUnstagedField(srcField, dstSignalId);
            dstField.setEventTypes(eventType); // If a new field is added during an event addition, that means the only event type is the one being added
            fieldService.create(dstField, dstAttributeIds); // create since no field with the same tag was found
        } else {
            // associate field with attributes, since the field already exists
            val dstField = sameTagFields.iterator().next();
            fieldService.associateWithAttributes(dstAttributeIds, dstField);
            appendEventType(eventType, dstField);
        }
    }

    private void createOrReuseSignalField(String eventType, VersionedId dstSignalId, Field srcField) {
        val sameTagFields = signalService.getFields(dstSignalId).stream()
                .filter(field -> emptyIfNull(field.getAttributes()).isEmpty())
                .filter(field -> field.getTag().equals(srcField.getTag()))
                .collect(toSet());

        if (sameTagFields.isEmpty()) {
            val dstField = toUnstagedField(srcField, dstSignalId);
            dstField.setEventTypes(eventType); // If a new field is added during an event addition, that means the only event type is the one being added
            fieldService.create(dstField, Set.of());
        } else {
            val dstField = sameTagFields.iterator().next();
            appendEventType(eventType, dstField);
        }
    }

    private void appendEventType(String eventType, UnstagedField dstField) {
        var eventTypes = dstField.toList(dstField.getEventTypes());

        if (!eventTypes.contains(eventType)) {
            eventTypes.add(eventType);
            dstField.setEventTypes(String.join(COMMA, eventTypes));
            fieldService.update(dstField);
        }
    }

    private Set<Long> toUnstagedAttributeIds(Collection<? extends Attribute> fieldAttributes, Map<Long, Long> eventAttributeMap, boolean failIfAbsent) {
        return fieldAttributes.stream()
                .map(srcAttr -> {
                    val srcAttrId = srcAttr.getId();
                    val dstAttrId = eventAttributeMap.get(srcAttrId);
                    if (failIfAbsent) {
                        Validate.isTrue(Objects.nonNull(dstAttrId), "Dst attributeId cannot be found for src attributeId: " + srcAttrId);
                    }
                    return dstAttrId;
                }).filter(Objects::nonNull)
                .collect(toSet());
    }

    protected String extractTag(UnstagedField field) {
        for (Pattern pattern : FIELD_PATTERNS) {
            val matcher = pattern.matcher(field.getExpression());
            boolean isPayloadGetter = pattern.equals(FIELD_PATTERNS[2]);
            if (matcher.matches()) {
                val extracted = matcher.group(1);
                return isPayloadGetter ? extracted.substring(0, 1).toLowerCase(Locale.ENGLISH) + extracted.substring(1) : extracted;
            }
        }
        return field.getName(); // A bug fix for long expressions that cannot be parsed and so stored as is under the tag (failing during db persistence)
    }

    private UnstagedAttribute toUnstagedAttribute(UnstagedField field, String tagName) {
        val expression = field.getExpression();

        val attribute = new UnstagedAttribute();
        attribute.setTag(tagName);
        attribute.setDescription(tagName);
        attribute.setSchemaPath(expression);
        attribute.setJavaType(field.getJavaType());
        return attribute;
    }

    private UnstagedAttribute toUnstagedAttribute(Attribute src, Map<Long, Long> eventMap) {
        val srcEventId = src.getParentId();
        val dstEventId = eventMap.get(srcEventId);
        Validate.isTrue(Objects.nonNull(dstEventId), "Dst eventId cannot be found for src eventId: " + srcEventId);

        val attribute = new UnstagedAttribute();
        copyAttributeProperties(src, attribute);
        attribute.setEventId(dstEventId);
        return attribute;
    }

    private UnstagedEvent toUnstagedEvent(Event src) {
        val dst = new UnstagedEvent();
        copyEventProperties(src, dst);

        // copy metadata id lists, since hibernate objects cannot be reused (as copyEventProperties() does)
        if (src instanceof UnstagedEvent event) {
            dst.setPageIds(new HashSet<>(event.getPageIds()));
            dst.setModuleIds(new HashSet<>(event.getModuleIds()));
            dst.setClickIds(new HashSet<>(event.getClickIds()));
        }

        return dst;
    }

    private UnstagedSignal toUnstagedSignal(Signal src, Long planId) {
        Validate.isTrue(Objects.nonNull(planId), "planId cannot be null");
        val dst = UnstagedSignal.builder().build();
        copySignalProperties(src, dst);

        dst.setPlanId(planId);
        dst.setSignalTemplateSourceId(src.getSignalTemplateSourceId());
        return dst;
    }

    private UnstagedField toUnstagedField(Field src, VersionedId signalId) {
        Validate.isTrue(Objects.nonNull(signalId), "signalId cannot be null");
        val dst = new UnstagedField();
        copyFieldProperties(src, dst);
        dst.setSignalId(signalId.getId());
        dst.setSignalVersion(signalId.getVersion());
        dst.setEventTypes(src.getEventTypesAsString());
        return dst;
    }

    @VisibleForTesting
    protected Collection<TemplateQuestion> validateUserAnswers(TemplateService templateService, UnstageRequest request) {
        val templateId = request.getSrcEntityId();
        val answeredQuestions = emptyIfNull(request.getAnsweredQuestions()).stream()
                .sorted(comparing(TemplateQuestion::getId))
                .toList();
        val persistedQuestions = templateService.getQuestions(templateId).stream()
                .map(question -> questionService.getByIdWithAssociations(question.getId()))
                .sorted(comparing(TemplateQuestion::getId))
                .toList();

        // validate all persisted questions matches user questions with Equator using the question property
        Validate.isTrue(CollectionUtils.isEqualCollection(persistedQuestions, answeredQuestions, questionEquator),
                "Answered user questions do not match persisted questions for templateId: " + templateId);

        // validate all answers not blank for user questions
        Validate.isTrue(answeredQuestions.stream()
                        .filter(TemplateQuestion::getIsMandatory)
                        .allMatch(q -> StringUtils.isNotBlank(q.getAnswer())),
                "User answers cannot be blank for templateId: " + templateId);

        // validate answer can be converted to a specified Java type
        answeredQuestions.stream()
                .filter(q -> StringUtils.isNotBlank(q.getAnswer()))
                .forEach(question -> question.getAnswerJavaType().convert(question.getAnswer()));

        for (int i = 0; i < answeredQuestions.size(); i++) { // both lists are ordered
            val persistedQuestion = persistedQuestions.get(i);
            val answeredQuestion = answeredQuestions.get(i);
            persistedQuestion.setAnswer(answeredQuestion.getAnswer());
        }

        return persistedQuestions;
    }

    private void updateAnswersForEvent(UnstagedEvent event, Collection<TemplateQuestion> answeredQuestions) {
        filterEventQuestions(event, answeredQuestions)
                .forEach(question -> setUserAnswer(question, event));
        unstagedEventRepository.save(event);
    }

    private void setUserAnswers(VersionedId signalId, Collection<TemplateQuestion> answeredQuestions) {
        signalService.getEvents(signalId).forEach(event -> updateAnswersForEvent(event, answeredQuestions));
    }

    private void setUserAnswer(TemplateQuestion question, UnstagedEvent event) {
        val setterType = Optional.ofNullable(question.getAnswerPropertySetterClass())
                .orElse(UserAnswerPropertySetter.class.getSimpleName());
        val setter = userAnswerSettersPerType.get(setterType);

        Validate.isTrue(Objects.nonNull(setter), "UserAnswerSetter not found for type: " + setterType);
        setter.apply(question, event);
    }
}
