package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.Auditable;
import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.audit.AuditLogParams;
import com.ebay.behavior.gds.mdm.common.model.audit.AuditRecord;
import com.ebay.behavior.gds.mdm.common.model.search.RelationalSearchRequest;
import com.ebay.behavior.gds.mdm.common.model.search.SearchSpecification;
import com.ebay.behavior.gds.mdm.commonSvc.repository.SpecificationRepository;
import com.ebay.behavior.gds.mdm.signal.common.model.AbstractStagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.model.Plan;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.datatype.SignalDefinition;
import com.ebay.behavior.gds.mdm.signal.config.GovernanceConfiguration;
import com.ebay.behavior.gds.mdm.signal.model.SignalChildId;
import com.ebay.behavior.gds.mdm.signal.model.manyToMany.StagedSignalEventMapping;
import com.ebay.behavior.gds.mdm.signal.model.view.StagedSignalProductionView;
import com.ebay.behavior.gds.mdm.signal.model.view.StagedSignalStagingView;
import com.ebay.behavior.gds.mdm.signal.repository.StagedFieldRepository;
import com.ebay.behavior.gds.mdm.signal.repository.StagedSignalProductionViewRepository;
import com.ebay.behavior.gds.mdm.signal.repository.StagedSignalRepository;
import com.ebay.behavior.gds.mdm.signal.repository.StagedSignalStagingViewRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.StagedSignalEventMappingRepository;
import com.ebay.behavior.gds.mdm.signal.service.migration.LegacyMapper;
import com.ebay.behavior.gds.mdm.signal.util.AuditUtils;

import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.ws.rs.ForbiddenException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.ebay.behavior.gds.mdm.common.model.Environment.PRODUCTION;
import static com.ebay.behavior.gds.mdm.common.model.Environment.STAGING;
import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.CREATED;
import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.UPDATED;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.getRequestUser;
import static com.ebay.behavior.gds.mdm.signal.util.CacheConstants.ALL_PRODUCTION_SIGNALS_LATEST_VERSIONS_CACHE;
import static com.ebay.behavior.gds.mdm.signal.util.CacheConstants.ALL_STAGED_SIGNALS_CACHE;
import static com.ebay.behavior.gds.mdm.signal.util.CacheConstants.ALL_STAGING_SIGNALS_LATEST_VERSIONS_CACHE;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.Validate.isTrue;

@Slf4j
@Service
@Validated
@SuppressWarnings("PMD.GodClass")
public class StagedSignalService {

    @Autowired
    private StagedSignalRepository repository;

    @Autowired
    private StagedSignalProductionViewRepository productionViewRepository;

    @Autowired
    private StagedSignalStagingViewRepository stagingViewRepository;

    @Autowired
    private StagedSignalEventMappingRepository eventMappingRepository;

    @Autowired
    private StagedEventService eventService;

    @Autowired
    private StagedFieldRepository fieldRepository;

    @Autowired
    private UnstagedSignalService unstagedSignalService;

    @Autowired
    private PlanService planService;

    @Autowired
    private StagedFieldService fieldService;

    @Autowired
    private LegacyMapper legacyMapper;

    @Autowired
    private GovernanceConfiguration configuration;

    @Autowired
    private EntityManager entityManager;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public StagedSignal create(@NotNull @Valid StagedSignal signal) {
        try {
            // Staged signals promoted from unstaged environment must keep original id and version.
            // Migrated signals have id/version set from unstaged migrated signal.
            isTrue(Objects.nonNull(signal.getId()), "id must not be null");
            isTrue(Objects.nonNull(signal.getVersion()), "version must not be null");
            return repository.save(signal);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    @Transactional(readOnly = true)
    public StagedSignal getById(@NotNull @Valid VersionedId signalId) {
        return repository.findByIdAndVersion(signalId.getId(), signalId.getVersion())
                .orElseThrow(() -> new DataNotFoundException(StagedSignal.class, signalId.toString()));
    }

    @Transactional(readOnly = true)
    public Optional<StagedSignal> findById(@NotNull @Valid VersionedId signalId) {
        return repository.findByIdAndVersion(signalId.getId(), signalId.getVersion());
    }

    @Transactional(readOnly = true)
    public StagedSignal getEnrichedById(@NotNull @Valid VersionedId signalId) {
        val stagedSignal = getById(signalId);
        entityManager.detach(stagedSignal);

        val unstagedSignal = unstagedSignalService.getByIdAndLatestVersion(stagedSignal.getSignalId().getId());
        val planName = planService.findById(unstagedSignal.getPlanId()).map(Plan::getName).orElse(null);
        stagedSignal.setUnstagedDetails(unstagedSignal, planName);

        val fieldGroup = fieldService.getAllFieldGroups(stagedSignal.getSignalId());
        stagedSignal.setFieldGroups(fieldGroup);
        return stagedSignal;
    }

    @Transactional(readOnly = true)
    public StagedSignal getByIdWithAssociations(@NotNull @Valid VersionedId signalId) {
        val signal = getById(signalId);
        Hibernate.initialize(signal.getFields());

        val events = getEvents(signalId);
        signal.setEvents(events);

        return signal;
    }

    @Transactional(readOnly = true)
    public StagedSignal getByIdWithAssociationsRecursive(@NotNull @Valid VersionedId signalId) {
        val signal = getById(signalId);
        Hibernate.initialize(signal.getFields());
        signal.getFields().forEach(field -> {
            Hibernate.initialize(field.getAttributes());
            for (var attribute : field.getAttributes()) {
                Hibernate.initialize(attribute);
                attribute.setEvent(Hibernate.unproxy(attribute.getEvent(), StagedEvent.class));
            }
        });

        val events = getEvents(signalId);
        events.forEach(event -> Hibernate.initialize(event.getAttributes()));

        signal.setEvents(events);
        return signal;
    }

    @Transactional(readOnly = true)
    public StagedSignal getLatestVersionById(long id, @NotNull Environment env) {
        var signals = repository.findAllById(id).stream()
                .sorted(Comparator.comparingInt(StagedSignal::getVersion).reversed())
                .toList();

        if (PRODUCTION.equals(env)) {
            signals = signals.stream()
                    .filter(signal -> PRODUCTION.equals(signal.getEnvironment()))
                    .toList();
        }

        if (signals.isEmpty()) {
            throw new DataNotFoundException("No staged signal found for the provided id: " + id);
        }

        return signals.get(0);
    }

    @Transactional(readOnly = true)
    public Set<StagedField> getFields(@NotNull @Valid VersionedId signalId) {
        getById(signalId); // Ensure the signal exists
        return Set.copyOf(fieldRepository.findBySignalIdAndSignalVersion(signalId.getId(), signalId.getVersion()));
    }

    @Transactional(readOnly = true)
    public Set<StagedEvent> getEvents(@NotNull @Valid VersionedId signalId) {
        getById(signalId); // Ensure the signal exists

        return eventMappingRepository.findBySignalIdAndSignalVersion(signalId.getId(), signalId.getVersion()).stream()
                .map(StagedSignalEventMapping::getEvent)
                .map(proxy -> (StagedEvent) Hibernate.unproxy(proxy)) // mapping repo respond with proxy objects
                .collect(toSet());
    }

    @Transactional(readOnly = true)
    public Set<SignalChildId> getEventIds(@NotNull Set<@NotNull @Valid VersionedId> signalIds) {
        return eventMappingRepository.findEventIdsBySignalIds(signalIds);
    }

    @Transactional(readOnly = true)
    public Set<SignalChildId> getFieldIds(@NotNull Set<@NotNull @Valid VersionedId> signalIds) {
        return fieldRepository.findFieldIdsBySignalIds(signalIds);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public StagedSignalEventMapping createEventMapping(@NotNull @Valid VersionedId signalId, @PositiveOrZero long eventId) {
        val existingMappings = eventMappingRepository.findBySignalIdAndSignalVersionAndEventId(signalId.getId(), signalId.getVersion(), eventId);
        if (!existingMappings.isEmpty()) {
            // If the mapping already exists, return the first one (a workaround for a known issue with duplicated mappings)
            return existingMappings.get(0);
        }

        val signal = getById(signalId);
        val event = eventService.getById(eventId);
        val mapping = new StagedSignalEventMapping(signal, event);
        return eventMappingRepository.save(mapping);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateToProductionEnvironment(@NotNull @Valid VersionedId signalId) {
        val signal = getById(signalId); // Ensure the signal exists
        isTrue(STAGING == signal.getEnvironment(), "Staged signal must be in STAGING before promotion to PRODUCTION");
        signal.setEnvironment(PRODUCTION);
        repository.save(signal);
    }

    /**
     * The delete operation is relevant only to migrated signals, since client signals should never be deleted
     * Deletes the signal, all its fields, attributes and events.
     * The order of deletion is important, as attributes and events can be shared between different signals.
     */
    public void deleteMigrated(@NotEmpty Set<@Valid VersionedId> signalIds) {
        signalIds.forEach(this::deleteMigrated);
    }

    /**
     * The delete operation is relevant only to migrated signals, since client signals should never be deleted
     * Deletes the signal, all its fields, attributes and events.
     * The order of deletion is important, as attributes and events can be shared between different signals.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteMigrated(@Valid VersionedId signalId) {
        var user = getRequestUser();
        if (!configuration.isAdmin(user)) {
            val admins = String.join(", ", configuration.getAdminSet());
            val message = String.format("User [%s] is not allowed to delete a staged signal. Admins: %s", user, admins);
            log.warn(message);
            throw new ForbiddenException(message);
        }

        // TODO temporarily disabled legacyId check, to be able to delete test data from production
        //val signal = getById(signalId);
        //isTrue(StringUtils.isNotEmpty(signal.getLegacyId()), "Migrated signal must have non-null legacyId");

        unstagedSignalService.delete(signalId);

        // delete all fields with field-to-attribute mappings
        fieldService.deleteMigrated(signalId);

        // delete events with attributes, deletes event-to-signal mappings
        eventService.deleteMigrated(signalId);

        // Finally, delete the signal
        repository.deleteById(signalId);
    }

    /**
     * Used by Portal UI to search for signals (all signal versions).
     *
     * @param withUnstagedDetails Whether to enrich the signals with latest unstaged signal details.
     * @param request RelationalSearchRequest with search filters and sorting criteria.
     * @return A page of StagedSignal objects.
     */
    @Transactional(readOnly = true)
    public Page<StagedSignal> searchAllVersions(boolean withUnstagedDetails, @Valid @NotNull RelationalSearchRequest request) {
        return search(withUnstagedDetails, request, repository, StagedSignal.class);
    }

    // Used by Portal UI to search for staging signals (only latest signal versions).
    @Transactional(readOnly = true)
    public Page<StagedSignalStagingView> searchStagingLatestVersions(boolean withUnstagedDetails, @Valid @NotNull RelationalSearchRequest request) {
        return search(withUnstagedDetails, request, stagingViewRepository, StagedSignalStagingView.class);
    }

    // Used by Portal UI to search for production signals (only latest signal versions).
    @Transactional(readOnly = true)
    public Page<StagedSignalProductionView> searchProductionLatestVersions(boolean withUnstagedDetails, @Valid @NotNull RelationalSearchRequest request) {
        return search(withUnstagedDetails, request, productionViewRepository, StagedSignalProductionView.class);
    }

    private <S extends AbstractStagedSignal> Page<S> search(boolean withUnstagedDetails, RelationalSearchRequest request,
                                                            SpecificationRepository<S, VersionedId> repo, Class<S> type) {
        val page = repo.findAll(
                SearchSpecification.getSpecification(request, false, type),
                SearchSpecification.getPageable(request)
        );

        if (withUnstagedDetails) {
            addUnstagedDetails(page.getContent());
        }

        return page;
    }

    /**
     * Used by service components (pipeline jobs, e.t.c) to get all signal versions.
     *
     * @param env Requested environment.
     *         For PRODUCTION, we must query env = PRODUCTION.
     *         For STAGING, we omit the environment since both environments are in STAGING.
     * @param dataSource Requested UDC data source.
     * @param platformId A signal platformId.
     * @return A list of StagedSignal objects.
     */
    @Transactional(readOnly = true)
    public Set<StagedSignal> getAllVersions(@NotNull Environment env, @NotNull UdcDataSourceType dataSource, @PositiveOrZero long platformId) {
        Set<StagedSignal> signals;
        if (PRODUCTION.equals(env)) {
            signals = new HashSet<>(repository.findAllByEnvironmentAndDataSourceAndPlatformId(PRODUCTION, dataSource, platformId));
        } else {
            signals = new HashSet<>(repository.findAllByDataSourceAndPlatformId(dataSource, platformId));
        }

        setAssociations(signals);
        return signals;
    }

    // Used by service components (pipeline jobs, e.t.c) to get all staging signals (only latest signal versions).
    @Transactional(readOnly = true)
    public Set<StagedSignalStagingView> getAllStagingLatestVersions(@NotNull UdcDataSourceType dataSource, @PositiveOrZero long platformId) {
        val signals = new HashSet<>(stagingViewRepository.findAllByDataSourceAndPlatformId(dataSource, platformId));
        setAssociations(signals);
        return signals;
    }

    // Used by service components (pipeline jobs, e.t.c) to get all production signals (only latest signal versions).
    @Transactional(readOnly = true)
    public Set<StagedSignalProductionView> getAllProductionLatestVersions(@NotNull UdcDataSourceType dataSource, @PositiveOrZero long platformId) {
        val signals = new HashSet<>(productionViewRepository.findAllByDataSourceAndPlatformId(dataSource, platformId));
        setAssociations(signals);
        return signals;
    }

    /**
     * Used by service components (pipeline jobs, e.t.c) to get all signals (all signal versions).
     * Uses a cache to improve performance.
     *
     * @return A list of StagedSignal objects.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = ALL_STAGED_SIGNALS_CACHE, sync = true, key = "#env.name() + '_' + #source.name() + '_' + #platformId")
    public Set<StagedSignal> getAllVersionsCached(@NotNull Environment env, @NotNull UdcDataSourceType source, @PositiveOrZero long platformId) {
        return getAllVersions(env, source, platformId);
    }

    /**
     * Used by service components (pipeline jobs, e.t.c) to get all staging signals (only latest signal versions).
     * Uses a cache to improve performance.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = ALL_STAGING_SIGNALS_LATEST_VERSIONS_CACHE, sync = true, key = "#source.name() + '_' + #platformId")
    public Set<? extends AbstractStagedSignal> getAllStagingLatestVersionsCached(@NotNull UdcDataSourceType source, @PositiveOrZero long platformId) {
        return getAllStagingLatestVersions(source, platformId);
    }

    /**
     * Used by service components (pipeline jobs, e.t.c) to get all production signals (only latest signal versions).
     * Uses a cache to improve performance.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = ALL_PRODUCTION_SIGNALS_LATEST_VERSIONS_CACHE, sync = true, key = "#source.name() + '_' + #platformId")
    public Set<? extends AbstractStagedSignal> getAllProductionLatestVersionsCached(@NotNull UdcDataSourceType source, @PositiveOrZero long platformId) {
        return getAllProductionLatestVersions(source, platformId);
    }

    @Transactional(readOnly = true)
    public List<AuditRecord<StagedSignal>> getAuditLog(@NotNull UdcDataSourceType dataSource, @Valid @NotNull AuditLogParams params) {
        isTrue(params.isNonVersioned(), "AuditLogParams must have no version to get all Signal production versions audit log");
        val id = params.getId();

        val signals = repository.findAllById(id).stream()
                .filter(signal -> PRODUCTION.equals(signal.getEnvironment()))
                .filter(signal -> dataSource.equals(signal.getDataSource()))
                .sorted(Comparator.comparingInt(StagedSignal::getVersion))
                .map(StagedSignal::getSignalId)
                .map(this::getByIdWithAssociationsRecursive)
                .peek(entityManager::detach)
                .peek(signal -> signal.getFields().forEach(StagedField::populateAuditKey))
                .toList();

        if (signals.isEmpty()) {
            throw new DataNotFoundException("No production versions found for the provided signal id: " + id);
        }

        val auditRecords = new ArrayList<AuditRecord<StagedSignal>>();

        for (int i = 0; i < signals.size(); i++) {
            val curr = signals.get(i);
            val prev = i == 0 ? null : signals.get(i - 1);

            val diff = switch (params.getMode()) {
                case BASIC -> null;
                case FULL -> AuditUtils.getChanges(prev, curr);
            };

            val changeType = i == 0 ? CREATED : UPDATED;
            val record = new AuditRecord<>(curr.getRevision(), changeType, null, prev, diff, curr);
            auditRecords.add(record);
        }

        return auditRecords;
    }

    protected <S extends AbstractStagedSignal> void addUnstagedDetails(List<S> signals) {
        val ids = signals.stream()
                .map(AbstractStagedSignal::getId)
                .collect(toSet());

        val unstagedSignals = new HashSet<>(unstagedSignalService.findAllByIdInAndLatestVersion(ids));

        val planIds = unstagedSignals.stream()
                .map(UnstagedSignal::getPlanId)
                .filter(Objects::nonNull)
                .collect(toSet());

        val plans = planService.findAllById(planIds).stream()
                .collect(toMap(Plan::getId, identity()));

        val unstagedSignalsById = unstagedSignals.stream()
                .collect(toMap(UnstagedSignal::getId, identity()));

        signals.forEach(staged -> {
            val id = staged.getId();
            val unstaged = unstagedSignalsById.get(id);

            if (Objects.nonNull(unstaged)) {
                val plan = plans.get(unstaged.getPlanId());
                val planName = Optional.ofNullable(plan).map(Plan::getName).orElse(null);
                staged.setUnstagedDetails(unstaged, planName);
            }
        });
    }

    /**
     * Requires events and fields to be initialized by a caller, to make sure events and fields initialization was not done twice.
     */
    @Transactional(readOnly = true)
    public List<SignalDefinition> toSignalDefinitions(@NotNull Collection<? extends AbstractStagedSignal> signals) {
        signals.forEach(signal -> {
            if (!Hibernate.isInitialized(signal.getFields())) {
                throw new IllegalStateException(String.format(
                        "Fields are not initialized for signal: %s. Must use setAssociations() prior calling toSignalDefinitions()", signal.getName()));
            }
            entityManager.detach(signal); // to prevent the hibernate to fire lazy load queries
            signal.getFields(); // will fail if events are not initialized
        });

        return CollectionUtils.emptyIfNull(signals).stream()
                .map(signal -> legacyMapper.map(signal, SignalDefinition.class))
                .toList();
    }

    /**
     * Sets events and fields, expecting to be initialized by a caller.
     */
    @Transactional(readOnly = true)
    public Page<SignalDefinition> toSignalDefinitionsPage(@NotNull Page<? extends AbstractStagedSignal> page) {
        val signals = page.getContent();
        setAssociations(signals);
        val signalDefinitions = toSignalDefinitions(signals);
        return new PageImpl<>(signalDefinitions, page.getPageable(), page.getTotalElements());
    }

    private void setAssociations(Collection<? extends AbstractStagedSignal> signals) {
        if (CollectionUtils.isEmpty(signals)) {
            return;
        }
        setAssociations(signals, this::getEventIds, eventService::getAllByIds, AbstractStagedSignal::setEvents);
        setAssociations(signals, this::getFieldIds, fieldService::getAllByIds, AbstractStagedSignal::setFields);
    }

    /**
     * Optimized for performance. Loads signal children (events, fields) in bulk.
     * Gets all children ids for all signals in a single query.
     * Then loads all children in a single query.
     * Finally, sets children to each signal.
     */
    private <A extends Auditable> void setAssociations(Collection<? extends AbstractStagedSignal> signals,
                                                       Function<Set<VersionedId>, Set<SignalChildId>> getChildrenIdsFunction,
                                                       Function<Set<Long>, Set<A>> getChildrenFunction,
                                                       BiConsumer<AbstractStagedSignal, Set<A>> setChildrenFunction) {
        val signalIds = signals.stream()
                .map(AbstractStagedSignal::getSignalId)
                .collect(toSet());

        val childrenIds = getChildrenIdsFunction.apply(signalIds);
        val allIds = childrenIds.stream().map(SignalChildId::getChildId).collect(toSet());
        val allChildren = getChildrenFunction.apply(allIds);
        val eventsById = allChildren.stream().collect(toMap(A::getId, identity()));
        val childrenBySignal = childrenIds.stream().collect(groupingBy(SignalChildId::getSignalVersionedId, toSet()));

        signals.forEach(signal -> {
            val children = childrenBySignal.getOrDefault(signal.getSignalId(), Set.of()).stream()
                    .map(eventId -> eventsById.get(eventId.getChildId()))
                    .filter(Objects::nonNull)
                    .collect(toSet());
            setChildrenFunction.accept(signal, children);
        });
    }
}
