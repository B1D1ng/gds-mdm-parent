package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AuditService;
import com.ebay.behavior.gds.mdm.commonSvc.service.CrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.SearchService;
import com.ebay.behavior.gds.mdm.signal.common.model.AttributeTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.EventSource;
import com.ebay.behavior.gds.mdm.signal.common.model.EventTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.FieldTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.TemplateQuestion;
import com.ebay.behavior.gds.mdm.signal.common.model.audit.EventTemplateHistory;
import com.ebay.behavior.gds.mdm.signal.common.model.search.EventSearchBy;
import com.ebay.behavior.gds.mdm.signal.model.manyToMany.FieldAttributeTemplateMapping;
import com.ebay.behavior.gds.mdm.signal.model.manyToMany.TemplateQuestionEventMapping;
import com.ebay.behavior.gds.mdm.signal.repository.AttributeTemplateRepository;
import com.ebay.behavior.gds.mdm.signal.repository.EventTemplateRepository;
import com.ebay.behavior.gds.mdm.signal.repository.audit.EventTemplateHistoryRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.FieldAttributeTemplateMappingRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.TemplateQuestionEventMappingRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Service
@Validated
public class EventTemplateService
        extends AbstractCrudAndAuditService<EventTemplate, EventTemplateHistory>
        implements CrudService<EventTemplate>, SearchService<EventTemplate>, AuditService<EventTemplateHistory>, TemplateService {

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private EventTemplateRepository repository;

    @Autowired
    private AttributeTemplateRepository attributeRepository;

    @Autowired
    private TemplateQuestionEventMappingRepository mappingRepository;

    @Autowired
    private FieldAttributeTemplateMappingRepository fieldAttributeMappingRepository;

    @Autowired
    private EventTypeLookupService eventTypeService;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private EventTemplateHistoryRepository historyRepository;

    @Getter(AccessLevel.PROTECTED)
    private final Class<EventTemplate> modelType = EventTemplate.class;

    @Getter(AccessLevel.PROTECTED)
    private final Class<EventTemplateHistory> historyModelType = EventTemplateHistory.class;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public EventTemplate create(@NotNull @Valid EventTemplate event) {
        eventTypeService.getByName(event.getType()); // Ensure the event type exists
        return super.create(event);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public EventTemplate update(@NotNull @Valid EventTemplate event) {
        eventTypeService.getByName(event.getType()); // Ensure the event type exists
        return super.update(event);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(long id) {
        getById(id); // Ensure the event exists
        if (!getAttributes(id).isEmpty()) {
            throw new IllegalArgumentException("Event template has attributes and cannot be deleted. Please delete all attributes first");
        }

        super.delete(id);
    }

    @Override
    @Transactional(readOnly = true)
    public EventTemplate getByIdWithAssociations(long id) {
        val event = getById(id);
        Hibernate.initialize(event.getAttributes());
        return event;
    }

    @Transactional(readOnly = true)
    public Set<AttributeTemplate> getAttributes(long id) {
        getById(id); // Ensure the event exists
        return attributeRepository.findByEventTemplateId(id);
    }

    @Transactional(readOnly = true)
    public Set<FieldTemplate> getFields(EventTemplate eventTemplate) {
        getById(eventTemplate.getId()); // Ensure the event exists
        if (CollectionUtils.isEmpty(eventTemplate.getAttributes())) {
            return Set.of();
        }

        val attributeIds = eventTemplate.getAttributes().stream()
                .map(AttributeTemplate::getId)
                .collect(toSet());

        return fieldAttributeMappingRepository.getFields(attributeIds).stream()
                .map(FieldAttributeTemplateMapping::getField)
                .collect(toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<TemplateQuestion> getQuestions(long id) {
        getById(id); // Ensure the event exists
        return mappingRepository.findByEventTemplateId(id).stream()
                .map(TemplateQuestionEventMapping::getQuestion)
                .collect(toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventTemplate> getAll(@Valid @NotNull Search search) {
        val searchBy = EventSearchBy.valueOf(search.getSearchBy());

        return switch (searchBy) {
            case NAME -> findByName(search);
            case TYPE -> findByType(search);
            case DESCRIPTION -> findByDescription(search);
            default -> throw new IllegalArgumentException("Invalid Event template searchBy: " + search.getSearchBy());
        };
    }

    private Page<EventTemplate> findByName(Search search) {
        return findByTerm(search, repository::findByName, repository::findByNameStartingWith, repository::findByNameContaining);
    }

    private Page<EventTemplate> findByType(Search search) {
        return findByTerm(search, repository::findByType, repository::findByTypeStartingWith, repository::findByTypeContaining);
    }

    private Page<EventTemplate> findByDescription(Search search) {
        return findByTerm(search, repository::findByDescription, repository::findByDescriptionStartingWith, repository::findByDescriptionContaining);
    }

    public Set<EventTemplate> findBySource(EventSource source) {
        return repository.findBySource(source);
    }
}