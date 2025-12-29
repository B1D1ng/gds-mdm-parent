package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.CrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.SearchService;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedAttribute;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.search.EventSearchBy;
import com.ebay.behavior.gds.mdm.signal.repository.StagedAttributeRepository;
import com.ebay.behavior.gds.mdm.signal.repository.StagedEventRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.StagedSignalEventMappingRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import org.apache.commons.lang3.NotImplementedException;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Service
@Validated
public class StagedEventService
        extends AbstractCrudService<StagedEvent>
        implements CrudService<StagedEvent>, SearchService<StagedEvent> {

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private StagedEventRepository repository;

    @Autowired
    private StagedAttributeRepository attributeRepository;

    @Autowired
    private StagedSignalEventMappingRepository signalMappingRepository;

    @Lazy
    @Autowired
    private StagedSignalService signalService;

    @Getter(AccessLevel.PROTECTED)
    private final Class<StagedEvent> modelType = StagedEvent.class;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public StagedEvent update(@NotNull @Valid StagedEvent event) {
        throw new NotImplementedException("Immutable StagedEvent");
    }

    @Override
    public void delete(long id) {
        throw new NotImplementedException("Immutable StagedEvent");
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteMigrated(VersionedId signalId) {
        signalService.getById(signalId);
        val eventIds = signalService.getEvents(signalId).stream().map(StagedEvent::getId).collect(toSet());

        // delete attributes
        val attributeIds = attributeRepository.findAllByEventIdIn(eventIds).stream().map(StagedAttribute::getId).collect(toSet());
        attributeRepository.deleteAllById(attributeIds);

        // delete events
        signalMappingRepository.deleteAllBySignalIdAndSignalVersionAndEventIds(signalId.getId(), signalId.getVersion(), eventIds);
        repository.deleteAllById(eventIds);
    }

    @Override
    @Transactional(readOnly = true)
    public StagedEvent getByIdWithAssociations(long id) {
        val event = getById(id);
        Hibernate.initialize(event.getAttributes());
        Hibernate.initialize(event.getPageIds());
        Hibernate.initialize(event.getModuleIds());
        Hibernate.initialize(event.getClickIds());
        return event;
    }

    @Transactional(readOnly = true)
    public List<StagedAttribute> getAttributes(long id) {
        getById(id); // Ensure the event exists
        return attributeRepository.findByEventId(id);
    }

    @Transactional(readOnly = true)
    public Set<StagedEvent> getAllByIds(@NotNull Set<Long> ids) {
        return new HashSet<>(repository.findAllByIdIn(ids));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StagedEvent> getAll(@Valid @NotNull Search search) {
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

    private Page<StagedEvent> findByName(Search search) {
        return findByTerm(search, repository::findByName, repository::findByNameStartingWith, repository::findByNameContaining);
    }

    private Page<StagedEvent> findByType(Search search) {
        return findByTerm(search, repository::findByType, repository::findByTypeStartingWith, repository::findByTypeContaining);
    }

    private Page<StagedEvent> findByDescription(Search search) {
        return findByTerm(search, repository::findByDescription, repository::findByDescriptionStartingWith, repository::findByDescriptionContaining);
    }

    private Page<StagedEvent> findByPageId(Search search) {
        return findByIdTerm(search, repository::findByPageId);
    }

    private Page<StagedEvent> findByModuleId(Search search) {
        return findByIdTerm(search, repository::findByModuleId);
    }

    private Page<StagedEvent> findByClickId(Search search) {
        return findByIdTerm(search, repository::findByClickId);
    }
}