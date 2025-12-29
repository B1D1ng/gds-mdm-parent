package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.audit.AuditLogParams;
import com.ebay.behavior.gds.mdm.common.model.audit.AuditRecord;
import com.ebay.behavior.gds.mdm.commonSvc.service.AuditService;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.model.UpdateUnstagedSignalRequest;
import com.ebay.behavior.gds.mdm.signal.common.model.audit.UnstagedSignalHistory;
import com.ebay.behavior.gds.mdm.signal.model.manyToMany.UnstagedSignalEventMapping;
import com.ebay.behavior.gds.mdm.signal.repository.UnstagedFieldRepository;
import com.ebay.behavior.gds.mdm.signal.repository.UnstagedSignalRepository;
import com.ebay.behavior.gds.mdm.signal.repository.audit.UnstagedSignalHistoryRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.UnstagedSignalEventMappingRepository;
import com.ebay.behavior.gds.mdm.signal.util.AuditUtils;
import com.ebay.behavior.gds.mdm.signal.util.ServiceUtils;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.val;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.CREATED;
import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.UPDATED;
import static com.ebay.behavior.gds.mdm.common.util.CommonValidationUtils.validateForUpdate;
import static com.ebay.behavior.gds.mdm.signal.util.AuditUtils.saveAndAudit;
import static java.util.stream.Collectors.toSet;
import static org.hibernate.Hibernate.initialize;
import static org.hibernate.Hibernate.unproxy;

@Service
@Validated
public class UnstagedSignalService implements AuditService<UnstagedSignalHistory> {

    @Autowired
    private UnstagedSignalRepository repository;

    @Autowired
    private UnstagedFieldRepository fieldRepository;

    @Autowired
    private UnstagedSignalEventMappingRepository mappingRepository;

    @Autowired
    private UnstagedSignalHistoryRepository historyRepository;

    @Autowired
    private UnstagedEventService eventService;

    @Autowired
    private UnstagedFieldService fieldService;

    @Transactional(readOnly = true)
    public Optional<UnstagedSignal> findById(@NotNull @Valid VersionedId signalId) {
        return repository.findById(signalId);
    }

    @Transactional(readOnly = true)
    public UnstagedSignal getById(@NotNull @Valid VersionedId signalId) {
        return repository.findByIdAndVersion(signalId.getId(), signalId.getVersion())
                .orElseThrow(() -> new DataNotFoundException(UnstagedSignal.class, signalId.toString()));
    }

    @Transactional(readOnly = true)
    public UnstagedSignal getByIdAndLatestVersion(long id) {
        return repository.findByIdAndLatestVersion(id)
                .orElseThrow(() -> new DataNotFoundException(UnstagedSignal.class, id));
    }

    @Transactional(readOnly = true)
    public List<UnstagedSignal> findAllByIdInAndLatestVersion(Set<Long> ids) {
        return repository.findAllByIdInAndLatestVersion(ids);
    }

    @Transactional(readOnly = true)
    public UnstagedSignal getByIdWithAssociations(VersionedId signalId) {
        val signal = getById(signalId);
        initialize(signal.getFields());

        val events = getEvents(signalId);
        signal.setEvents(events);

        return signal;
    }

    @Transactional(readOnly = true)
    public UnstagedSignal getByIdWithAssociationsRecursive(VersionedId signalId) {
        val signal = getById(signalId);
        initialize(signal.getFields());
        signal.getFields().forEach(field -> {
            initialize(field.getAttributes());
            for (var attribute : field.getAttributes()) {
                initialize(attribute);
                attribute.setEvent(unproxy(attribute.getEvent(), UnstagedEvent.class));
            }
        });

        val events = getEvents(signalId);
        events.forEach(event -> initialize(event.getAttributes()));

        signal.setEvents(events);
        return signal;
    }

    @Transactional(readOnly = true)
    public int getLatestVersion(long id) {
        return repository.findLatestVersion(id)
                .orElseThrow(() -> new DataNotFoundException(UnstagedSignal.class, id));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UnstagedSignal create(@NotNull @Valid UnstagedSignal signal) {
        // validate
        val id = signal.getId();
        val version = signal.getVersion();

        if (Objects.nonNull(id)) {
            Validate.isTrue(Objects.nonNull(version), "Version must not be null if id is provided");
        }
        try {
            return saveAndAudit(signal, repository, historyRepository, CREATED, null, UnstagedSignalHistory.class);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException(ex); // DataIntegrityViolation means FK constraint violation because of bad parentId inside the model
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void createAll(@NotEmpty Set<@NotNull @Valid UnstagedSignal> signals) {
        signals.forEach(this::create);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UnstagedSignal update(@NotNull @Valid UnstagedSignal signal) {
        validateForUpdate(signal);
        getById(VersionedId.of(signal.getSignalId())); // Ensure signal exists before update
        return saveAndAudit(signal, repository, historyRepository, UPDATED, null, UnstagedSignalHistory.class);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateEnvironment(@NotNull @Valid VersionedId signalId, @NotNull Environment env) {
        var signal = getById(signalId);
        signal.setEnvironment(env);
        update(signal);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UnstagedSignal updateLatestVersion(@NotNull @Valid UpdateUnstagedSignalRequest request) {
        validateForUpdate(request);

        var signal = getByIdAndLatestVersion(request.getId());
        ServiceUtils.copyOverwriteAllProperties(request, signal);
        return saveAndAudit(signal, repository, historyRepository, UPDATED, null, UnstagedSignalHistory.class);
    }

    /**
     * Deletes the signals, all its fields, attributes and events.
     * The order of deletion is important, as attributes and events can be shared between different signals.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteLatestVersion(long id) {
        var signalId = getByIdAndLatestVersion(id).getSignalId();
        delete(signalId);
    }

    /**
     * Deletes the signals, all its fields, attributes and events.
     * The order of deletion is important, as attributes and events can be shared between different signals.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(VersionedId signalId) {
        getById(signalId);
        val eventIds = getConnectedEventIds(signalId);

        val fieldIds = getFields(signalId).stream()
                .map(UnstagedField::getId)
                .collect(toSet());

        // delete all fields
        fieldService.delete(fieldIds);

        // delete events with attributes
        eventIds.forEach(eventService::delete);

        // delete direct signal-event mappings, if any
        val mappings = mappingRepository.findBySignalIdAndSignalVersion(signalId.getId(), signalId.getVersion());
        mappings.forEach(mapping -> eventService.delete(mapping.getEvent().getId()));

        // Finally, delete the signal
        var signal = getById(signalId);
        AuditUtils.deleteAndAudit(signal, signalId, repository, historyRepository, UnstagedSignalHistory.class);
    }

    @Transactional(readOnly = true)
    public Set<UnstagedField> getFields(@NotNull @Valid VersionedId signalId) {
        getById(signalId); // Ensure the signal exists
        return Set.copyOf(fieldRepository.findBySignalIdAndSignalVersion(signalId.getId(), signalId.getVersion()));
    }

    @Transactional(readOnly = true)
    public Set<UnstagedEvent> getEvents(@NotNull @Valid VersionedId signalId) {
        getById(signalId); // Ensure the signal exists

        return mappingRepository.findBySignalIdAndSignalVersion(signalId.getId(), signalId.getVersion()).stream()
                .map(UnstagedSignalEventMapping::getEvent)
                .map(proxy -> (UnstagedEvent) unproxy(proxy)) // mapping repo respond with proxy objects
                .collect(toSet());
    }

    /**
     * Gets all event ids connected to the signal.
     * This method goes the "hard way", joining the UnstagedField, UnstagedAttribute and UnstagedFieldAttributeMapping tables,
     * so it can find the "real" connected events from signal->field->attribute->event path.
     *
     * @param signalId A signal id.
     * @return A set of event ids connected to the signal.
     */
    @Transactional(readOnly = true)
    public Set<Long> getConnectedEventIds(VersionedId signalId) {
        return repository.getConnectedEventIds(signalId.getId(), signalId.getVersion());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UnstagedSignalEventMapping createEventMapping(VersionedId signalId, long eventId) {
        val signal = getById(signalId);
        val event = eventService.getById(eventId);
        val mapping = new UnstagedSignalEventMapping(signal, event);
        return mappingRepository.save(mapping);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteEventMapping(VersionedId signalId, long eventId) {
        val mapping = mappingRepository.findBySignalIdAndSignalVersionAndEventId(signalId.getId(), signalId.getVersion(), eventId)
                .orElseThrow(() -> new DataNotFoundException(UnstagedSignalEventMapping.class, signalId.toString(), String.valueOf(eventId)));
        mappingRepository.deleteById(mapping.getId());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteEventMappings(VersionedId signalId, Set<Long> eventIds) {
        mappingRepository.deleteAllBySignalIdAndSignalVersionAndEventIds(signalId.getId(), signalId.getVersion(), eventIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditRecord<UnstagedSignalHistory>> getAuditLog(@Valid @NotNull AuditLogParams params) {
        Validate.isTrue(params.isVersioned(), "AuditLogParams must have a version to get an audit log");
        getById(VersionedId.of(params.getId(), params.getVersion())); // Ensure the signal exists
        return AuditUtils.getAuditLog(historyRepository, params);
    }

    @Transactional(readOnly = true)
    public List<UnstagedSignal> getByLegacyId(@NotBlank String legacyId) {
        return repository.findByLegacyId(legacyId);
    }
}