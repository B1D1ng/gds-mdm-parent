package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.AbstractModel;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AuditService;
import com.ebay.behavior.gds.mdm.commonSvc.service.CrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.SearchService;
import com.ebay.behavior.gds.mdm.signal.common.model.EventTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.FieldTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.TemplateQuestion;
import com.ebay.behavior.gds.mdm.signal.common.model.audit.SignalTemplateHistory;
import com.ebay.behavior.gds.mdm.signal.common.model.search.SignalSearchBy;
import com.ebay.behavior.gds.mdm.signal.model.manyToMany.SignalEventTemplateMapping;
import com.ebay.behavior.gds.mdm.signal.model.manyToMany.TemplateQuestionEventMapping;
import com.ebay.behavior.gds.mdm.signal.repository.FieldTemplateRepository;
import com.ebay.behavior.gds.mdm.signal.repository.SignalTemplateRepository;
import com.ebay.behavior.gds.mdm.signal.repository.audit.SignalTemplateHistoryRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.SignalEventTemplateMappingRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.TemplateQuestionEventMappingRepository;

import com.google.common.collect.ImmutableList;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.hibernate.Hibernate.initialize;

@Service
@Validated
public class SignalTemplateService
        extends AbstractCrudAndAuditService<SignalTemplate, SignalTemplateHistory>
        implements CrudService<SignalTemplate>, SearchService<SignalTemplate>, AuditService<SignalTemplateHistory>, TemplateService {

    @Getter(AccessLevel.PROTECTED)
    private final Class<SignalTemplate> modelType = SignalTemplate.class;

    @Getter(AccessLevel.PROTECTED)
    private final Class<SignalTemplateHistory> historyModelType = SignalTemplateHistory.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private SignalTemplateRepository repository;

    @Autowired
    private FieldTemplateRepository fieldRepository;

    @Autowired
    private SignalEventTemplateMappingRepository signalEventMappingRepository;

    @Autowired
    private TemplateQuestionEventMappingRepository questionEventMappingRepository;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private SignalTemplateHistoryRepository historyRepository;

    @Autowired
    private EventTemplateService eventService;

    @Autowired
    private AttributeTemplateService attributeService;

    @Autowired
    private FieldTemplateService fieldService;

    @Autowired
    private TemplateQuestionService questionService;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(long signalId) {
        getById(signalId); // Ensure the signal exists

        val fieldIds = getFields(signalId).stream()
                .map(FieldTemplate::getId)
                .collect(toSet());

        fieldService.delete(signalId, fieldIds);
        super.delete(signalId);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteRecursive(long signalId) {
        getById(signalId); // Ensure the signal exists

        val fields = getFields(signalId);

        val attributeIds = fields.stream()
                .flatMap(field -> field.getAttributes().stream())
                .map(AbstractModel::getId)
                .collect(toSet());

        val eventIds = getEvents(signalId).stream()
                .map(EventTemplate::getId)
                .collect(toSet());

        val fieldIds = fields.stream()
                .map(FieldTemplate::getId)
                .collect(toSet());

        fieldService.delete(signalId, fieldIds);

        attributeIds.forEach(attributeService::delete);

        eventIds.forEach(eventId -> {
            eventService.getQuestions(eventId).forEach(question -> {
                val questionId = question.getId();
                questionService.delete(questionId);
            });
            eventService.delete(eventId);
        });

        super.delete(signalId);
    }

    @Override
    @Transactional(readOnly = true)
    public SignalTemplate getByIdWithAssociations(long id) {
        val signal = getById(id);
        initialize(signal.getEvents());
        initialize(signal.getFields());
        return signal;
    }

    @Transactional(readOnly = true)
    public SignalTemplate getByIdWithAssociationsRecursive(long id) {
        val signal = getById(id);

        initialize(signal.getEvents());
        signal.getEvents().forEach(event -> initialize(event.getAttributes()));

        initialize(signal.getFields());
        signal.getFields().forEach(field -> initialize(field.getAttributes()));

        return signal;
    }

    @Transactional(readOnly = true)
    public Set<FieldTemplate> getFields(long signalId) {
        getById(signalId); // Ensure the signal exists
        return fieldRepository.findBySignalTemplateId(signalId);
    }

    @Transactional(readOnly = true)
    public Set<EventTemplate> getEvents(long signalId) {
        getById(signalId); // Ensure the signal exists
        return signalEventMappingRepository.findBySignalId(signalId).stream()
                .map(SignalEventTemplateMapping::getEvent)
                .collect(toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public Set<TemplateQuestion> getQuestions(long id) {
        getById(id); // Ensure the signal exists

        val eventIds = getEvents(id).stream()
                .filter(EventTemplate::getIsMandatory)
                .map(EventTemplate::getId)
                .collect(toSet());

        return questionEventMappingRepository.findByEventTemplateIds(eventIds).stream()
                .map(TemplateQuestionEventMapping::getQuestion)
                .collect(toSet());
    }

    /**
     * Gets all event ids connected to the signal template.
     * This method goes the "hard way", joining the FieldTemplate, AttributeTemplate and UnstagedFieldAttributeMapping tables,
     * so it can find the "real" connected events from signal->field->attribute->event path.
     *
     * @param signalId A signal template id.
     * @return A set of event ids connected to the signal template.
     */
    @Transactional(readOnly = true)
    public Set<Long> getConnectedEventIds(long signalId) {
        return repository.getConnectedEventIds(signalId);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public SignalEventTemplateMapping createEventMapping(long signalId, long eventId) {
        val signal = getById(signalId);
        val event = eventService.getById(eventId);

        val mapping = new SignalEventTemplateMapping(signal, event);
        return signalEventMappingRepository.save(mapping);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteEventMapping(long signalId, long eventId) {
        val mapping = signalEventMappingRepository.findBySignalIdAndEventId(signalId, eventId)
                .orElseThrow(() -> new DataNotFoundException(SignalEventTemplateMapping.class, signalId, eventId));
        signalEventMappingRepository.deleteById(mapping.getId());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteEventMappings(long signalId, Set<Long> eventIds) {
        signalEventMappingRepository.deleteAllBySignalIdAndEventIds(signalId, eventIds);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SignalTemplate> getAll(@Valid @NotNull Search search) {
        val searchBy = SignalSearchBy.valueOf(search.getSearchBy());

        return switch (searchBy) {
            case NAME -> findByName(search);
            case TYPE -> findByType(search);
            case DESCRIPTION -> findByDescription(search);
            case DOMAIN -> findByDomain(search);
        };
    }

    @Transactional(readOnly = true)
    public List<String> getTypesByPlatformId(@PositiveOrZero long platformId) {
        return ImmutableList.copyOf(repository.getTypesByPlatform(platformId));
    }

    private Page<SignalTemplate> findByName(Search search) {
        return findByTerm(search, repository::findByName, repository::findByNameStartingWith, repository::findByNameContaining);
    }

    private Page<SignalTemplate> findByType(Search search) {
        return findByTerm(search, repository::findByType, repository::findByTypeStartingWith, repository::findByTypeContaining);
    }

    @Transactional(readOnly = true)
    public Optional<SignalTemplate> findByType(String type) {
        return getRepository().findByType(type);
    }

    private Page<SignalTemplate> findByDescription(Search search) {
        return findByTerm(search, repository::findByDescription, repository::findByDescriptionStartingWith, repository::findByDescriptionContaining);
    }

    private Page<SignalTemplate> findByDomain(Search search) {
        return findByTerm(search, repository::findByDomain, repository::findByDomainStartingWith, repository::findByDomainContaining);
    }
}
