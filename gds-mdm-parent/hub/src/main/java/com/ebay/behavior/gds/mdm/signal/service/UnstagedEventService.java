package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.model.ExpressionType;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AuditService;
import com.ebay.behavior.gds.mdm.commonSvc.service.CrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.SearchService;
import com.ebay.behavior.gds.mdm.commonSvc.util.JexlValidator;
import com.ebay.behavior.gds.mdm.signal.common.model.Attribute;
import com.ebay.behavior.gds.mdm.signal.common.model.EvaluateEventExpressionResponse;
import com.ebay.behavior.gds.mdm.signal.common.model.FieldGroup;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedAttribute;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.UpdateUnstagedEventRequest;
import com.ebay.behavior.gds.mdm.signal.common.model.audit.UnstagedEventHistory;
import com.ebay.behavior.gds.mdm.signal.common.model.search.EventSearchBy;
import com.ebay.behavior.gds.mdm.signal.common.service.pmsvc.PmsvcService;
import com.ebay.behavior.gds.mdm.signal.model.manyToMany.UnstagedFieldAttributeMapping;
import com.ebay.behavior.gds.mdm.signal.repository.UnstagedAttributeRepository;
import com.ebay.behavior.gds.mdm.signal.repository.UnstagedEventRepository;
import com.ebay.behavior.gds.mdm.signal.repository.audit.UnstagedEventHistoryRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.UnstagedFieldAttributeMappingRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.UnstagedSignalEventMappingRepository;
import com.ebay.behavior.gds.mdm.signal.util.FieldGroupUtils;
import com.ebay.behavior.gds.mdm.signal.util.ServiceUtils;

import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import static com.ebay.behavior.gds.mdm.common.model.ExpressionType.JEXL;
import static com.ebay.behavior.gds.mdm.common.model.Model.COMMA;
import static java.util.Comparator.comparing;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toSet;

@Service
@Validated
@SuppressWarnings("PMD.GodClass")
public class UnstagedEventService
        extends AbstractCrudAndAuditService<UnstagedEvent, UnstagedEventHistory>
        implements CrudService<UnstagedEvent>, SearchService<UnstagedEvent>, AuditService<UnstagedEventHistory> {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UnstagedFieldService fieldService;

    @Autowired
    private UnstagedAttributeService attributeService;

    @Lazy
    @Autowired
    private UnstagedSignalService signalService;

    @Autowired
    private PmsvcService pmsvcService;

    @Lazy
    @Autowired
    private BusinessFieldService businessFieldService;

    @Lazy
    @Autowired
    private UnstagedFieldGroupService fieldGroupService;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private UnstagedEventRepository repository;

    @Autowired
    private UnstagedAttributeRepository attributeRepository;

    @Autowired
    private UnstagedFieldAttributeMappingRepository fieldAttributeMappingRepository;

    @Autowired
    private UnstagedSignalEventMappingRepository signalEventMappingRepository;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private UnstagedEventHistoryRepository historyRepository;

    @Getter(AccessLevel.PROTECTED)
    private final Class<UnstagedEvent> modelType = UnstagedEvent.class;

    @Getter(AccessLevel.PROTECTED)
    private final Class<UnstagedEventHistory> historyModelType = UnstagedEventHistory.class;

    // regex to extract id list from a string like: "[123,456].contains(event.context.pageInteractionContext.pageId)"
    private final String pageIdRegex = "\\[\\s*(\\d+(?:\\s*,\\s*\\d+)*)\\s*\\]\\.contains\\([^)]*pageId\\)";
    private final String moduleIdRegex = "\\[\\s*(\\d+(?:\\s*,\\s*\\d+)*)\\s*\\]\\.contains\\([^)]*moduleId\\)";
    private final String clickIdRegex = "\\[\\s*(\\d+(?:\\s*,\\s*\\d+)*)\\s*\\]\\.contains\\([^)]*clickId\\)";
    protected final Pattern pageIdPattern = Pattern.compile(pageIdRegex);
    protected final Pattern moduleIdPattern = Pattern.compile(moduleIdRegex);
    protected final Pattern clickIdPattern = Pattern.compile(clickIdRegex);

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UnstagedEvent update(@NotNull @Valid UpdateUnstagedEventRequest request) {
        val eventId = request.getId();
        val expression = request.getExpression();
        var event = getById(eventId);
        ServiceUtils.copyModelProperties(request, event);
        validateExpression(request, event);

        // expression change logic includes removing old business fields and creating new ones, as per the asset ids under the expression
        if (Objects.nonNull(expression)) {
            val signalId = getSignalId(eventId).orElseThrow(() -> new IllegalStateException(String.format("Signal id for event id %d not found", eventId)));
            val oldBusinessFields = businessFieldService.simulateBusinessFields(signalId, event, true); // a dry run - no fields created
            boolean hasAssetIdsUpdated = updateEventAssetsFromExpression(event);
            super.update(event); // update must happen before creating business fields

            if (hasAssetIdsUpdated) { // recreate business fields
                deleteBusinessFields(signalId, oldBusinessFields);
                businessFieldService.createBusinessFields(signalId, eventId, true);
            }
        } else {
            super.update(event); // must stay under "else" and cannot be moved outside since the timing of the update is important for "if" flow
        }

        return getById(eventId);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UnstagedEvent update(@NotNull @Valid UnstagedEvent event) {
        throw new NotImplementedException("UnstagedEvent can be updated only with update(UpdateUnstagedEventRequest request) method");
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(long eventId) {
        val event = getById(eventId);
        val maybeSignalId = getSignalId(eventId);

        maybeSignalId.ifPresent(signalId -> deleteSignalFields(signalId, event));

        val attributeIds = event.getAttributes().stream()
                .map(Attribute::getId)
                .collect(toSet());

        val attributeFields = fieldAttributeMappingRepository.getFields(attributeIds).stream()
                .map(UnstagedFieldAttributeMapping::getField)
                .collect(toSet());

        val fieldIds = attributeFields.stream()
                .filter(field -> field.getAttributes().stream()
                        .allMatch(attr -> attributeIds.contains(attr.getId())))
                .map(UnstagedField::getId)
                .collect(toSet());

        maybeSignalId.ifPresent(signalId -> signalService.deleteEventMapping(signalId, eventId));
        fieldService.delete(fieldIds);
        attributeIds.forEach(attributeService::forceDelete);
        super.delete(eventId);
    }

    /**
     * Evaluates the new expression and returns two lists of field groups:
     * - current signal field groups
     * - next signal field groups (after the expression is updated)
     *
     * @param eventId An event id to evaluate the expression update for
     * @param expression The JEXL expression to evaluate
     * @param type The expression type, must be JEXL
     * @return EvaluateEventExpressionResponse containing current and next signal field groups
     */
    @Transactional(readOnly = true)
    public EvaluateEventExpressionResponse evaluateExpressionUpdate(long eventId, @NotBlank String expression, @NotNull ExpressionType type) {
        if (type != JEXL) {
            throw new IllegalArgumentException("Only JEXL expression type is supported for evaluation");
        }

        JexlValidator.isValidExpression(expression);
        var event = getById(eventId);
        val signalId = getSignalId(eventId).orElseThrow(() -> new IllegalStateException(String.format("Signal id for event id %d not found", eventId)));
        entityManager.detach(event);

        val currSignalFieldsWithoutEventId = signalService.getFields(signalId).stream()
                .filter(not(UnstagedField::getIsMandatory))
                .filter(not(field -> field.getAttributes().stream().anyMatch(attr -> attr.getEventId().equals(eventId))))
                .collect(toSet());

        event.setExpression(expression);
        updateEventAssetsFromExpression(event);
        val nextEventFields = businessFieldService.simulateBusinessFields(signalId, event, true);

        // there is no db id/revision for simulated fields, but it required during validation
        nextEventFields.forEach(field -> {
            field.setId(0L);
            field.setRevision(0);
        });

        val nextSignalFields = new HashSet<>(currSignalFieldsWithoutEventId);
        nextSignalFields.addAll(nextEventFields);

        // nextSignalFieldGroups consist of all signal business fields minus current event business fields plus simulated business fields
        val nextSignalFieldGroups = FieldGroupUtils.getAllFieldGroups(nextSignalFields).stream()
                .sorted(comparing(FieldGroup::getGroupKey))
                .toList();

        val currFieldGroups = fieldGroupService.getAll(signalId).stream()
                .filter(not(FieldGroup::getIsMandatory))
                .sorted(comparing(FieldGroup::getGroupKey))
                .toList();

        return new EvaluateEventExpressionResponse(currFieldGroups, nextSignalFieldGroups);
    }

    private Optional<VersionedId> getSignalId(long eventId) {
        return signalEventMappingRepository.findByEventId(eventId)
                .map(mapping -> mapping.getSignal().getSignalId());
    }

    private Set<UnstagedField> getMatchedFields(UnstagedField field, Collection<UnstagedField> fields) {
        return fields.stream()
                .filter(f -> f.getName().equals(field.getName()))
                .filter(field1 -> field1.getTag().equals(field.getTag()))
                .filter(f -> f.getEventTypes().equals(field.getEventTypes()))
                .collect(toSet());
    }

    private void validateExpression(UpdateUnstagedEventRequest request, UnstagedEvent event) {
        val expression = request.getExpression();
        val type = request.getExpressionType();

        if (StringUtils.isBlank(expression) && Objects.isNull(type)) {
            return;
        }

        if (JEXL.equals(type)) {
            JexlValidator.isValidExpression(event.getExpression());
        }
    }

    private boolean updateEventAssetsFromExpression(UnstagedEvent event) {
        val expression = event.getExpression();
        if (StringUtils.isBlank(expression)) {
            return false;
        }

        if (!JEXL.equals(event.getExpressionType())) {
            return false;
        }

        val pageIds = getAssetIds(pageIdPattern, pmsvcService::getPageById, "Page", expression);
        val moduleIds = getAssetIds(moduleIdPattern, pmsvcService::getModuleById, "Module", expression);
        val clickIds = getAssetIds(clickIdPattern, pmsvcService::getClickById, "Click", expression);

        boolean isPageIdChanged = !event.getPageIds().equals(pageIds);
        boolean isModuleIdChanged = !event.getModuleIds().equals(moduleIds);
        boolean isClickIdChanged = !event.getClickIds().equals(clickIds);

        if (isPageIdChanged && isModuleIdChanged && isClickIdChanged) {
            return false;
        }

        event.setPageIds(pageIds);
        event.setModuleIds(moduleIds);
        event.setClickIds(clickIds);
        return true;
    }

    private void deleteBusinessFields(VersionedId signalId, Set<UnstagedField> oldBusinessFields) {
        // delete old business fields
        val businessFields = signalService.getFields(signalId).stream()
                .filter(field -> !field.getIsMandatory())
                .collect(toSet());
        val oldMatchedBusinessFields = oldBusinessFields.stream()
                .flatMap(oldField -> getMatchedFields(oldField, businessFields).stream())
                .collect(toSet());
        val oldMatchedAttributeIds = oldMatchedBusinessFields.stream()
                .flatMap(field -> field.getAttributes().stream())
                .map(UnstagedAttribute::getId)
                .collect(toSet());
        val oldMatchedBusinessFieldIds = oldMatchedBusinessFields.stream()
                .map(UnstagedField::getId)
                .collect(toSet());

        fieldService.delete(oldMatchedBusinessFieldIds);
        attributeService.delete(oldMatchedAttributeIds);
    }

    @VisibleForTesting
    protected Set<Long> getAssetIds(Pattern pattern, Function<Long, Object> idGetter, String assetType, String expression) {
        val matcher = pattern.matcher(expression);
        if (!matcher.find()) {
            return new HashSet<>();
        }

        val strIds = matcher.group(1);
        val ids = Arrays.stream(strIds.split("\\s*,\\s*")).map(Long::valueOf).collect(toSet());
        Validate.notEmpty(ids, assetType + " ids must not be empty");

        for (val id : ids) {
            val asset = idGetter.apply(id);
            Validate.isTrue(Objects.nonNull(asset), String.format("%s id: %d not found", assetType, id));
        }

        return ids;
    }

    private void deleteSignalFields(VersionedId signalId, UnstagedEvent event) {
        val eventType = event.getType();
        val numEventsOfSameType = getNumEventsOfSameType(signalId, eventType);

        // we must delete signal fields of the same event type that not associated with any attribute,
        // if and only if there is only one event of the same type is associated with the signal (since this event type is removed)
        if (numEventsOfSameType != 1) {
            return;
        }

        val signalFieldsWithSameType = signalService.getFields(signalId).stream()
                .filter(field -> field.getAttributes().isEmpty())
                .filter(field -> field.getEventTypes().contains(eventType))
                .toList();

        var unusedFields = new ArrayList<UnstagedField>();

        for (var field : signalFieldsWithSameType) {
            var eventTypes = field.toList(field.getEventTypes());
            eventTypes.remove(eventType);

            if (eventTypes.isEmpty()) {
                unusedFields.add(field);
            } else {
                field.setEventTypes(String.join(COMMA, eventTypes));
                fieldService.update(field);
            }
        }

        if (!unusedFields.isEmpty()) {
            fieldService.delete(unusedFields.stream().map(UnstagedField::getId).collect(toSet()));
        }

        val eventFieldsOfSameType = signalService.getFields(signalId).stream()
                .filter(field -> !field.getAttributes().isEmpty())
                .filter(field -> field.getEventTypes().contains(eventType))
                .toList();

        for (var field : eventFieldsOfSameType) {
            var eventTypes = field.toList(field.getEventTypes());
            eventTypes.remove(eventType);

            // if it is empty, the field be deleted by the caller method flow anyway, so we must skip the update with empty event types
            if (!eventTypes.isEmpty()) {
                field.setEventTypes(String.join(COMMA, eventTypes));
                fieldService.update(field);
            }
        }
    }

    private long getNumEventsOfSameType(VersionedId signalId, String eventType) {
        return signalService.getEvents(signalId).stream()
                .filter(evt -> evt.getType().equals(eventType))
                .count();
    }

    @Override
    @Transactional(readOnly = true)
    public UnstagedEvent getByIdWithAssociations(long id) {
        val event = getById(id);
        Hibernate.initialize(event.getAttributes());
        return event;
    }

    @Transactional(readOnly = true)
    public List<UnstagedAttribute> getAttributes(long id) {
        getById(id); // Ensure the event exists
        return attributeRepository.findByEventId(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UnstagedEvent> getAll(@Valid @NotNull Search search) {
        val searchBy = EventSearchBy.valueOf(search.getSearchBy());

        return switch (searchBy) {
            case NAME -> findByName(search);
            case TYPE -> findByType(search);
            case DESCRIPTION -> findByDescription(search);
            case PAGE_ID -> findByPageId(search);
            case MODULE_ID -> findByModuleId(search);
            case CLICK_ID -> findByClickId(search);
        };
    }

    private Page<UnstagedEvent> findByName(Search search) {
        return findByTerm(search, repository::findByName, repository::findByNameStartingWith, repository::findByNameContaining);
    }

    private Page<UnstagedEvent> findByType(Search search) {
        return findByTerm(search, repository::findByType, repository::findByTypeStartingWith, repository::findByTypeContaining);
    }

    private Page<UnstagedEvent> findByDescription(Search search) {
        return findByTerm(search, repository::findByDescription, repository::findByDescriptionStartingWith, repository::findByDescriptionContaining);
    }

    private Page<UnstagedEvent> findByPageId(Search search) {
        return findByIdTerm(search, repository::findByPageId);
    }

    private Page<UnstagedEvent> findByModuleId(Search search) {
        return findByIdTerm(search, repository::findByModuleId);
    }

    private Page<UnstagedEvent> findByClickId(Search search) {
        return findByIdTerm(search, repository::findByClickId);
    }
}