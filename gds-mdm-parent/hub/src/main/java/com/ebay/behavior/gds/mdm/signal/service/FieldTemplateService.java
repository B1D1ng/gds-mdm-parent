package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.CrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.SearchService;
import com.ebay.behavior.gds.mdm.signal.common.model.AttributeTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.EventTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.EventTypeLookup;
import com.ebay.behavior.gds.mdm.signal.common.model.FieldTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.search.FieldSearchBy;
import com.ebay.behavior.gds.mdm.signal.model.manyToMany.EventTypeFieldTemplateMapping;
import com.ebay.behavior.gds.mdm.signal.model.manyToMany.FieldAttributeTemplateMapping;
import com.ebay.behavior.gds.mdm.signal.repository.FieldTemplateRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.EventTypeFieldTemplateMappingRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.FieldAttributeTemplateMappingRepository;
import com.ebay.com.google.common.collect.Sets;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.hibernate.Hibernate.initialize;

@Slf4j
@Service
@Validated
public class FieldTemplateService
        extends AbstractCrudService<FieldTemplate>
        implements CrudService<FieldTemplate>, SearchService<FieldTemplate> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<FieldTemplate> modelType = FieldTemplate.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private FieldTemplateRepository repository;

    @Autowired
    private FieldAttributeTemplateMappingRepository attributeMappingRepository;

    @Autowired
    private EventTypeFieldTemplateMappingRepository eventTypeMappingRepository;

    @Autowired
    private EventTypeLookupService eventTypeService;

    @Autowired
    private AttributeTemplateService attributeService;

    @Lazy
    @Autowired
    private SignalTemplateService signalService;

    /**
     * Creates a new FieldTemplate and associates it with the given AttributeTemplates.
     * Since each attribute is associated with an event, the field signal is also associated with the event.
     *
     * @param field FieldTemplate to create.
     * @param attributeIds Set of AttributeTemplate IDs to associate with the field.
     * @return Created FieldTemplate.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public FieldTemplate create(@NotNull @Valid FieldTemplate field, @Nullable Set<Long> attributeIds, @Nullable Set<Long> eventTypeIds) {
        val created = super.create(field);

        // associate field with event types
        Set<Long> fixedEventTypeIds = eventTypeIds != null ? eventTypeIds : Collections.emptySet();
        fixedEventTypeIds.forEach(eventTypeId -> createEventTypeMapping(created.getId(), eventTypeId, true));

        if (CollectionUtils.isEmpty(attributeIds)) {
            return created;
        }

        // associate field with attributes
        attributeIds.forEach(attributeId -> createAttributeMapping(created.getId(), attributeId));

        // associate signal with events
        val currentEventIds = attributeIds.stream()
                .map(attributeService::getById)
                .map(AttributeTemplate::getEventTemplateId)
                .collect(toSet());

        // collect event ids from associated signal template
        val signalId = created.getSignalTemplateId();
        val previousEventIds = signalService.getEvents(signalId).stream()
                .map(EventTemplate::getId)
                .collect(toSet());

        val missingEventIds = Sets.difference(currentEventIds, previousEventIds);
        missingEventIds.forEach(eventId -> signalService.createEventMapping(signalId, eventId));

        return created;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public FieldTemplate create(@NotNull @Valid FieldTemplate field) {
        throw new NotImplementedException("Use create(FieldTemplate field, attributeIds) instead");
    }

    @Override
    public List<FieldTemplate> createAll(@NotEmpty Set<@Valid FieldTemplate> models) {
        throw new NotImplementedException("Not implemented by design");
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public FieldTemplate update(@NotNull @Valid FieldTemplate field) {
        throw new NotImplementedException("FieldTemplate cannot be updated by design. Delete and create new instead");
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(long fieldId) {
        val field = getById(fieldId);
        val signalId = field.getSignalTemplateId();
        delete(signalId, Set.of(fieldId));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(Long signalId, Set<Long> fieldIds) {
        fieldIds.stream().map(this::getById)
                .forEach(field -> Validate.isTrue(signalId.equals(field.getSignalTemplateId()),
                        "UnstagedField %d does not belong to signal %d", field.getId(), signalId));

        val associatedEventIds = fieldIds.stream()
                .flatMap(fieldId -> getAttributes(fieldId).stream())
                .map(AttributeTemplate::getEventTemplateId)
                .collect(toSet());

        // next line also removes associated AttributeTemplate mappings, while cascade property is not set explicitly
        repository.deleteAllById(fieldIds);

        // remove signal-event mappings
        val connectedEventIds = signalService.getConnectedEventIds(signalId);
        val dissociateEventIds = Sets.difference(associatedEventIds, connectedEventIds);

        if (!dissociateEventIds.isEmpty()) {
            signalService.deleteEventMappings(signalId, dissociateEventIds);
        }

        // remove field-event mappings
        fieldIds.forEach(fieldId -> eventTypeMappingRepository.deleteByFieldId(fieldId));
    }

    @Override
    @Transactional(readOnly = true)
    public FieldTemplate getByIdWithAssociations(long id) {
        val field = getById(id);
        initialize(field.getSignal());
        initialize(field.getAttributes());
        initialize(field.getEventTypes());
        return field;
    }

    @Transactional(readOnly = true)
    public Set<AttributeTemplate> getAttributes(long fieldId) {
        getById(fieldId); // Ensure the field exists

        return attributeMappingRepository.findByFieldId(fieldId).stream()
                .map(FieldAttributeTemplateMapping::getAttribute)
                .collect(toSet());
    }

    @Transactional(readOnly = true)
    public Set<EventTypeLookup> getEventTypes(long fieldId) {
        getById(fieldId); // Ensure the field exists

        return eventTypeMappingRepository.findByFieldId(fieldId).stream()
                .map(EventTypeFieldTemplateMapping::getEventType)
                .collect(toSet());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public FieldAttributeTemplateMapping createAttributeMapping(long fieldId, long attributeId) {
        val field = getById(fieldId);
        val attribute = attributeService.getById(attributeId);
        val eventType = attribute.getEvent().getType();

        val eventTypeId = eventTypeService.getByName(eventType).getId();
        createEventTypeMapping(fieldId, eventTypeId, false);

        val mapping = new FieldAttributeTemplateMapping(field, attribute);
        return attributeMappingRepository.save(mapping);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteAttributeMapping(long fieldId, long attributeId) {
        val mapping = attributeMappingRepository.findByFieldIdAndAttributeId(fieldId, attributeId)
                .orElseThrow(() -> new DataNotFoundException(FieldAttributeTemplateMapping.class, fieldId, attributeId));
        attributeMappingRepository.deleteById(mapping.getId());

        val attribute = attributeService.getById(attributeId);
        val eventType = attribute.getEvent().getType();
        val eventTypeId = eventTypeService.getByName(eventType).getId();
        deleteEventTypeMapping(fieldId, eventTypeId);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public EventTypeFieldTemplateMapping createEventTypeMapping(long fieldId, long eventTypeId, boolean isImmutable) {
        val maybePersisted = eventTypeMappingRepository.findByFieldIdAndEventTypeId(fieldId, eventTypeId);
        if (maybePersisted.isPresent()) {
            return maybePersisted.get();
        }

        val field = getById(fieldId);
        val eventType = eventTypeService.getById(eventTypeId);

        val mapping = new EventTypeFieldTemplateMapping()
                .setField(field)
                .setEventType(eventType)
                .setIsImmutable(isImmutable);
        return eventTypeMappingRepository.save(mapping);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteEventTypeMapping(long fieldId, long eventTypeId) {
        val mapping = eventTypeMappingRepository.findByFieldIdAndEventTypeId(fieldId, eventTypeId)
                .orElseThrow(() -> new DataNotFoundException(EventTypeFieldTemplateMapping.class, fieldId, eventTypeId));

        if (mapping.getIsImmutable()) {
            log.info("Cannot delete immutable mapping for fieldId: {} and eventTypeId: {}", fieldId, eventTypeId);
            return;
        }

        eventTypeMappingRepository.deleteById(mapping.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FieldTemplate> getAll(@Valid @NotNull Search search) {
        val searchBy = FieldSearchBy.valueOf(search.getSearchBy());

        return switch (searchBy) {
            case NAME -> findByName(search);
            case TAG -> findByTag(search);
            case DESCRIPTION -> findByDescription(search);
        };
    }

    private Page<FieldTemplate> findByName(Search search) {
        return findByTerm(search, repository::findByName, repository::findByNameStartingWith, repository::findByNameContaining);
    }

    private Page<FieldTemplate> findByTag(Search search) {
        return findByTerm(search, repository::findByTag, repository::findByTagStartingWith, repository::findByTagContaining);
    }

    private Page<FieldTemplate> findByDescription(Search search) {
        return findByTerm(search, repository::findByDescription, repository::findByDescriptionStartingWith, repository::findByDescriptionContaining);
    }
}