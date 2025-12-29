package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AbstractCrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.CrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.SearchService;
import com.ebay.behavior.gds.mdm.signal.common.model.FieldGroup;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedAttribute;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.search.FieldSearchBy;
import com.ebay.behavior.gds.mdm.signal.model.manyToMany.StagedFieldAttributeMapping;
import com.ebay.behavior.gds.mdm.signal.repository.StagedFieldRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.StagedFieldAttributeMappingRepository;
import com.ebay.behavior.gds.mdm.signal.util.FieldGroupUtils;
import com.ebay.com.google.common.collect.Sets;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.HashSet;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.util.CommonValidationUtils.validateForCreate;
import static java.util.stream.Collectors.toSet;

@Service
@Validated
public class StagedFieldService extends AbstractCrudService<StagedField>
        implements CrudService<StagedField>, SearchService<StagedField> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<StagedField> modelType = StagedField.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private StagedFieldRepository repository;

    @Autowired
    private StagedFieldAttributeMappingRepository mappingRepository;

    @Lazy
    @Autowired
    private StagedSignalService signalService;

    @Autowired
    private StagedAttributeService attributeService;

    /**
     * Creates a new StagedField and associates it with the given StagedAttributes.
     * Since each attribute is associated with an event, the field signal is also associated with the event.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public StagedField create(@NotNull @Valid StagedField field, @Nullable Set<Long> attributeIds) {
        validateForCreate(field);
        try {
            val created = repository.save(field);
            if (CollectionUtils.isEmpty(attributeIds)) {
                return created;
            }

            associateWithAttributes(attributeIds, created);
            return created;
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException(ex); // DataIntegrityViolation means FK constraint violation because of bad parentId inside the model
        }
    }

    @Override
    public StagedField create(@NotNull @Valid StagedField field) {
        throw new NotImplementedException("Use create(field, attributeIds) instead");
    }

    @Override
    public StagedField update(@NotNull @Valid StagedField field) {
        throw new NotImplementedException("Immutable staged field");
    }

    @Override
    public void delete(long id) {
        throw new NotImplementedException("Immutable staged field");
    }

    public void deleteMigrated(VersionedId signalId) {
        val fieldIds = signalService.getFields(signalId).stream()
                .map(StagedField::getId)
                .collect(toSet());
        repository.deleteAllById(fieldIds);
    }

    @Override
    @Transactional(readOnly = true)
    public StagedField getByIdWithAssociations(long id) {
        val field = getById(id);
        Hibernate.initialize(field.getSignal());
        Hibernate.initialize(field.getAttributes());
        return field;
    }

    @Transactional(readOnly = true)
    public Set<StagedField> getAllByIds(@NotNull Set<Long> ids) {
        return new HashSet<>(repository.findAllByIdIn(ids));
    }

    @Transactional(readOnly = true)
    public Set<StagedAttribute> getAttributes(long fieldId) {
        getById(fieldId); // Ensure the field exists

        return mappingRepository.findByFieldId(fieldId).stream()
                .map(StagedFieldAttributeMapping::getAttribute)
                .collect(toSet());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void createAttributeMapping(long fieldId, long attributeId) {
        val field = getById(fieldId);
        val attribute = attributeService.getById(attributeId);

        val mapping = new StagedFieldAttributeMapping(field, attribute);
        mappingRepository.save(mapping);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StagedField> getAll(@Valid @NotNull Search search) {
        val searchBy = FieldSearchBy.valueOf(search.getSearchBy());

        return switch (searchBy) {
            case NAME -> findByName(search);
            case TAG -> findByTag(search);
            case DESCRIPTION -> findByDescription(search);
        };
    }

    /**
     * Gets all Fields connected to the signal definition.
     * De-duplicate Fields by a tag.
     */
    @Transactional(readOnly = true)
    public Set<@Valid FieldGroup<StagedField>> getAllFieldGroups(@NotNull @Valid VersionedId signalId) {
        return FieldGroupUtils.getAllFieldGroups(signalService.getFields(signalId));
    }

    private void associateWithAttributes(Set<Long> attributeIds, StagedField field) {
        if (CollectionUtils.isEmpty(attributeIds)) {
            return;
        }

        // associate field with attributes
        attributeIds.forEach(attributeId -> createAttributeMapping(field.getId(), attributeId));

        // associate signal with events
        val currentEventIds = attributeService.findAllById(attributeIds).stream()
                .map(StagedAttribute::getEventId)
                .collect(toSet());

        // collect event ids from associated signals
        val signalId = VersionedId.of(field.getSignalId(), field.getSignalVersion());
        val previousEventIds = signalService.getEvents(signalId).stream()
                .map(StagedEvent::getId)
                .collect(toSet());

        val missingEventIds = Sets.difference(currentEventIds, previousEventIds);
        missingEventIds.forEach(eventId -> signalService.createEventMapping(signalId, eventId));
    }

    private Page<StagedField> findByName(Search search) {
        return findByTerm(search, repository::findByName, repository::findByNameStartingWith, repository::findByNameContaining);
    }

    private Page<StagedField> findByTag(Search search) {
        return findByTerm(search, repository::findByTag, repository::findByTagStartingWith, repository::findByTagContaining);
    }

    private Page<StagedField> findByDescription(Search search) {
        return findByTerm(search, repository::findByDescription, repository::findByDescriptionStartingWith, repository::findByDescriptionContaining);
    }
}