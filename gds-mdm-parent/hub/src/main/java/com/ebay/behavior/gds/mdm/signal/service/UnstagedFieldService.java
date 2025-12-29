package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.commonSvc.service.AuditService;
import com.ebay.behavior.gds.mdm.commonSvc.service.CrudService;
import com.ebay.behavior.gds.mdm.commonSvc.service.SearchService;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedAttribute;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.UpdateUnstagedFieldRequest;
import com.ebay.behavior.gds.mdm.signal.common.model.audit.UnstagedFieldHistory;
import com.ebay.behavior.gds.mdm.signal.common.model.search.FieldSearchBy;
import com.ebay.behavior.gds.mdm.signal.model.manyToMany.UnstagedFieldAttributeMapping;
import com.ebay.behavior.gds.mdm.signal.repository.UnstagedFieldRepository;
import com.ebay.behavior.gds.mdm.signal.repository.audit.UnstagedFieldHistoryRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.UnstagedFieldAttributeMappingRepository;
import com.ebay.behavior.gds.mdm.signal.util.ServiceUtils;
import com.ebay.com.google.common.collect.Sets;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@Validated
public class UnstagedFieldService
        extends AbstractCrudAndAuditService<UnstagedField, UnstagedFieldHistory>
        implements CrudService<UnstagedField>, SearchService<UnstagedField>, AuditService<UnstagedFieldHistory> {

    @Getter(AccessLevel.PROTECTED)
    private final Class<UnstagedField> modelType = UnstagedField.class;

    @Getter(AccessLevel.PROTECTED)
    private final Class<UnstagedFieldHistory> historyModelType = UnstagedFieldHistory.class;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private UnstagedFieldRepository repository;

    @Autowired
    @Getter(AccessLevel.PROTECTED)
    private UnstagedFieldHistoryRepository historyRepository;

    @Autowired
    private UnstagedFieldAttributeMappingRepository mappingRepository;

    @Autowired
    private UnstagedAttributeService attributeService;

    @Lazy
    @Autowired
    private UnstagedSignalService signalService;

    /**
     * Creates a new UnstagedField and associates it with the given UnstagedAttribute.
     * Since each attribute is associated with an event, the field signal is also associated with the event.
     *
     * @param field UnstagedField to create.
     * @param attributeIds Set of Attribute IDs to associate with the field.
     * @return Created UnstagedField.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UnstagedField create(@NotNull @Valid UnstagedField field, @Nullable Set<Long> attributeIds) {
        val created = super.create(field);
        if (CollectionUtils.isEmpty(attributeIds)) {
            return created;
        }

        associateWithAttributes(attributeIds, created);
        return created;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UnstagedField create(@NotNull @Valid UnstagedField field) {
        throw new NotImplementedException("Use create(field, attributeIds) instead");
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UnstagedField update(@NotNull @Valid UpdateUnstagedFieldRequest request) {
        val fieldId = request.getId();
        val field = getById(fieldId);
        ServiceUtils.copyModelProperties(request, field);

        val updated = super.update(field);

        // null means user not interested to touch attributes,
        // but empty attributes means user want to delete all of them
        val attributeIds = request.getAttributeIds();
        if (null == attributeIds) {
            return updated;
        }

        // we need to recreate associations with attributes and events.
        // First, we delete old associations
        // Second, we create new associations
        getAttributes(fieldId).stream().map(UnstagedAttribute::getId)
                .forEach(attrId -> deleteAttributeMapping(fieldId, attrId));

        associateWithAttributes(attributeIds, updated);

        return updated;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(long fieldId) {
        val field = getById(fieldId);
        Validate.isTrue(!field.getIsMandatory(), "Cannot delete a mandatory field (%s, id: %d)", field.getTag(), field.getId());
        delete(Set.of(fieldId));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(@NotNull Set<Long> fieldIds) {
        if (CollectionUtils.isEmpty(fieldIds)) {
            return;
        }

        val sampleFieldId = fieldIds.iterator().next();
        val sampleField = getById(sampleFieldId);
        val signalId = sampleField.getSignalId();
        val signalVersion = sampleField.getSignalVersion();

        fieldIds.stream()
                .map(this::getById)
                .forEach(field -> {
                    Validate.isTrue(signalId.equals(field.getSignalId()) && signalVersion.equals(field.getSignalVersion()),
                            "UnstagedField id=%d does not belong to signal [id=%d, ver=%d]", field.getId(), signalId, signalVersion);
                });

        val associatedEventIds = fieldIds.stream()
                .flatMap(fieldId -> getAttributes(fieldId).stream())
                .map(UnstagedAttribute::getEventId)
                .collect(Collectors.toSet());

        // next line also removes associated Attribute mappings, while cascade property is not set explicitly
        repository.deleteAllById(fieldIds);

        // remove signal-event mappings
        val versionedId = VersionedId.of(signalId, signalVersion);
        val connectedEventIds = signalService.getConnectedEventIds(versionedId);
        val dissociateEventIds = Sets.difference(associatedEventIds, connectedEventIds);

        if (!dissociateEventIds.isEmpty()) {
            signalService.deleteEventMappings(versionedId, dissociateEventIds);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UnstagedField getByIdWithAssociations(long id) {
        val field = getById(id);
        Hibernate.initialize(field.getSignal());
        Hibernate.initialize(field.getAttributes());
        return field;
    }

    @Transactional(readOnly = true)
    public Set<UnstagedAttribute> getAttributes(long fieldId) {
        getById(fieldId); // Ensure the field exists

        return mappingRepository.findByFieldId(fieldId).stream()
                .map(UnstagedFieldAttributeMapping::getAttribute)
                .collect(Collectors.toSet());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void createAttributeMapping(long fieldId, long attributeId) {
        val field = getById(fieldId);
        val attribute = attributeService.getById(attributeId);

        val mapping = new UnstagedFieldAttributeMapping(field, attribute);
        mappingRepository.save(mapping);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteAttributeMapping(long fieldId, long attributeId) {
        val mapping = mappingRepository.findByFieldIdAndAttributeId(fieldId, attributeId)
                .orElseThrow(() -> new DataNotFoundException(UnstagedFieldAttributeMapping.class, fieldId, attributeId));
        mappingRepository.deleteById(mapping.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UnstagedField> getAll(@Valid @NotNull Search search) {
        val searchBy = FieldSearchBy.valueOf(search.getSearchBy());

        return switch (searchBy) {
            case NAME -> findByName(search);
            case TAG -> findByTag(search);
            case DESCRIPTION -> findByDescription(search);
        };
    }

    public void associateWithAttributes(Set<Long> attributeIds, UnstagedField field) {
        if (CollectionUtils.isEmpty(attributeIds)) {
            return;
        }

        // associate field with attributes
        attributeIds.forEach(attributeId -> createAttributeMapping(field.getId(), attributeId));

        // associate signal with events
        val currentEventIds = attributeService.findAllById(attributeIds).stream()
                .map(UnstagedAttribute::getEventId)
                .collect(Collectors.toSet());

        // collect event ids from associated signals
        val signalId = VersionedId.of(field.getSignalId(), field.getSignalVersion());
        val previousEventIds = signalService.getEvents(signalId).stream()
                .map(UnstagedEvent::getId)
                .collect(Collectors.toSet());

        val missingEventIds = Sets.difference(currentEventIds, previousEventIds);
        missingEventIds.forEach(eventId -> signalService.createEventMapping(signalId, eventId));
    }

    private Page<UnstagedField> findByName(Search search) {
        return findByTerm(search, repository::findByName, repository::findByNameStartingWith, repository::findByNameContaining);
    }

    private Page<UnstagedField> findByTag(Search search) {
        return findByTerm(search, repository::findByTag, repository::findByTagStartingWith, repository::findByTagContaining);
    }

    private Page<UnstagedField> findByDescription(Search search) {
        return findByTerm(search, repository::findByDescription, repository::findByDescriptionStartingWith, repository::findByDescriptionContaining);
    }
}