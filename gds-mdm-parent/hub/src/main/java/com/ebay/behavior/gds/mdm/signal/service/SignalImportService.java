package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.commonSvc.service.CrudService;
import com.ebay.behavior.gds.mdm.signal.common.model.MetadataAttribute;
import com.ebay.behavior.gds.mdm.signal.common.model.MetadataEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.MetadataField;
import com.ebay.behavior.gds.mdm.signal.common.model.PlatformLookup;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedAttribute;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedAttribute;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.service.MetadataWriteService;

import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.val;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
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
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.Environment.PRODUCTION;
import static com.ebay.behavior.gds.mdm.common.util.ServiceConstants.DEFAULT_RETRY_BACKOFF;
import static com.ebay.behavior.gds.mdm.signal.model.SpecialPlanType.IMPORT;
import static com.ebay.behavior.gds.mdm.signal.util.ImportUtils.createImportPlanIfAbsent;
import static com.ebay.behavior.gds.mdm.signal.util.ServiceUtils.copyAttributeProperties;
import static com.ebay.behavior.gds.mdm.signal.util.ServiceUtils.copyEventProperties;
import static com.ebay.behavior.gds.mdm.signal.util.ServiceUtils.copyFieldProperties;
import static com.ebay.behavior.gds.mdm.signal.util.ServiceUtils.copySignalProperties;
import static java.util.stream.Collectors.toSet;

@Service
@Validated
public class SignalImportService {

    @Autowired
    private DomainLookupService domainService;

    @Autowired
    private UnstagedSignalService unstagedSignalService;

    @Autowired
    private UnstagedEventService unstagedEventService;

    @Autowired
    private UnstagedAttributeService unstagedAttributeService;

    @Autowired
    private UnstagedFieldService unstagedFieldService;

    @Autowired
    private StagedSignalService stagedSignalService;

    @Autowired
    private StagedEventService stagedEventService;

    @Autowired
    private StagedAttributeService stagedAttributeService;

    @Autowired
    private StagedFieldService stagedFieldService;

    @Autowired
    private PlanService planService;

    @Autowired
    private MetadataWriteService udcWriteService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformLookupService platformService;

    @Autowired
    private PlatformLookup platformLookup;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public String injectAndImport(@NotNull UdcDataSourceType dataSource, @NotNull @Valid UnstagedSignal signal) {
        val entityId = udcWriteService.upsert(signal, dataSource);

        importUnstagedSignalIfAbsent(signal);
        importStagedSignalIfAbsent(signal);
        return entityId;
    }

    /**
     * Persist the exact copy of the unstaged signal and its events, attributes and fields into the database, keeping the original signal id and version.
     *
     * @param signal the original signal to be copied.
     * @return The signal id of the copied signal.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public VersionedId importUnstagedSignal(@Valid @NotNull UnstagedSignal signal) {
        Validate.isTrue(Objects.nonNull(signal.getPlatform()), "Platform cannot be null");
        val planId = createImportPlanIfAbsent(IMPORT, platformLookup, planService, domainService);
        signal.setPlanId(planId);
        val events = signal.getEvents();
        val fields = signal.getFields();
        signal.setFields(null); // hibernate will be confused to see associated entities during signal.save()
        try {
            val signalId = unstagedSignalService.create(signal).getSignalId();
            val eventMap = createEventMappings(events, unstagedEventService, UnstagedEvent.class);
            val attributeMap = createAttributeMappings(events, eventMap, unstagedAttributeService, UnstagedAttribute.class);
            createFields(fields, signal.getSignalId(), attributeMap, UnstagedField.class);
            return signalId;
        } finally {
            signal.setFields(fields); // restore the fields, if needed for further processing
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void importUnstagedSignalIfAbsent(@Valid @NotNull UnstagedSignal signal) {
        val signalId = signal.getSignalId();
        val maybePersisted = unstagedSignalService.findById(signalId);

        if (maybePersisted.isPresent()) {
            val persisted = maybePersisted.get();
            if (!persisted.getDataSource().equals(signal.getDataSource())) {
                unstagedSignalService.delete(signalId);
                importUnstagedSignal(signal);
            } else {
                unstagedSignalService.updateEnvironment(signalId, signal.getEnvironment());
            }
        } else {
            importUnstagedSignal(signal);
        }
    }

    /**
     * Persist the exact copy of the unstaged signal and its events, attributes and fields into the database, keeping the original signal id and version.
     * The copy is stored under a staged signal table, representing the signal promotion to the staging environment.
     *
     * @param signal the original signal to be copied.
     * @return The signal id of the copied signal.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public VersionedId importStagedSignal(@Valid @NotNull UnstagedSignal signal) {
        val events = signal.getEvents();

        val stagedSignal = toStagedSignal(signal);
        val signalId = stagedSignalService.create(stagedSignal).getSignalId();

        val eventMap = createEventMappings(events, stagedEventService, StagedEvent.class);
        val attributeMap = createAttributeMappings(events, eventMap, stagedAttributeService, StagedAttribute.class);
        createFields(signal.getFields(), stagedSignal.getSignalId(), attributeMap, StagedField.class);

        for (val eventId : eventMap.values()) {
            stagedSignalService.createEventMapping(signalId, eventId);
        }

        return signalId;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @Retryable(retryFor = CannotAcquireLockException.class, backoff = @Backoff(delay = DEFAULT_RETRY_BACKOFF))
    public void importStagedSignalIfAbsent(@Valid @NotNull UnstagedSignal signal) {
        val signalId = signal.getSignalId();
        val maybeStagedPersisted = stagedSignalService.findById(signalId);

        if (maybeStagedPersisted.isPresent()) {
            val persisted = maybeStagedPersisted.get();
            Validate.isTrue(persisted.getDataSource().equals(signal.getDataSource()),
                    "StagedSignal with id %s already exists with a different data source: %s", signalId, persisted.getDataSource());
            if (signal.getEnvironment() == PRODUCTION && persisted.getEnvironment() != PRODUCTION) {
                stagedSignalService.updateToProductionEnvironment(signalId);
            }
        } else {
            importStagedSignal(signal);
        }
    }

    private <M extends MetadataEvent> Map<Long, Long> createEventMappings(Collection<UnstagedEvent> srcEvents, CrudService<M> service, Class<M> type) {
        var eventMap = new HashMap<Long, Long>();
        if (CollectionUtils.isEmpty(srcEvents)) {
            return eventMap;
        }
        srcEvents.forEach(srcEvent -> {
            val dstEvent = toEvent(srcEvent, type);
            dstEvent.setEventSourceId(srcEvent.getId());
            val dstEventId = service.create(dstEvent).getId();
            eventMap.put(srcEvent.getId(), dstEventId);
        });
        return eventMap;
    }

    private <M extends MetadataAttribute> Map<Long, Long> createAttributeMappings(Collection<UnstagedEvent> srcEvents, Map<Long, Long> eventMap,
                                                                                  CrudService<M> service, Class<M> type) {
        var attributeMap = new HashMap<Long, Long>();
        if (CollectionUtils.isEmpty(srcEvents)) {
            return attributeMap;
        }
        srcEvents.stream()
                .flatMap(srcEvent -> srcEvent.getAttributes().stream())
                .forEach(srcAttr -> {
                    val dstAttr = toAttribute(srcAttr, eventMap, type);
                    val dstAttrId = service.create(dstAttr).getId();
                    attributeMap.put(srcAttr.getId(), dstAttrId);
                });
        return attributeMap;
    }

    private <M extends MetadataField> void createFields(Collection<UnstagedField> fields, VersionedId dstSignalId,
                                                        Map<Long, Long> attributeMap, Class<M> type) {
        fields.forEach(srcField -> {
            val srcAttributes = srcField.getAttributes();
            val dstAttributeIds = toAttributeIds(srcAttributes, attributeMap);
            val dstField = toField(srcField, dstSignalId, type);

            if (dstField instanceof StagedField stagedField) {
                stagedFieldService.create(stagedField, dstAttributeIds);
            } else if (dstField instanceof UnstagedField unstagedField) {
                unstagedFieldService.create(unstagedField, dstAttributeIds);
            } else {
                throw new IllegalArgumentException("Unsupported field type: " + dstField.getClass().getName());
            }
        });
    }

    private StagedSignal toStagedSignal(UnstagedSignal src) {
        val dst = new StagedSignal();
        copySignalProperties(src, dst);

        dst.setId(src.getId()); // migrated staged signal should keep the same id/version as the unstaged signal
        dst.setVersion(src.getVersion());
        dst.setSignalSourceId(src.getId());
        dst.setSignalSourceVersion(src.getSignalSourceVersion());
        dst.setSignalTemplateSourceId(src.getSignalTemplateSourceId());
        dst.setCreateBy(src.getCreateBy());
        dst.setCreateDate(src.getCreateDate());
        dst.setUpdateBy(src.getUpdateBy());
        dst.setUpdateDate(src.getUpdateDate());

        return dst;
    }

    private <M extends MetadataField> M toField(UnstagedField src, VersionedId signalId, Class<M> type) {
        Validate.isTrue(Objects.nonNull(signalId), "signalId cannot be null");
        M dst;
        try {
            dst = type.getDeclaredConstructor().newInstance();
            copyFieldProperties(src, dst);
            dst.setSignalId(signalId.getId());
            dst.setSignalVersion(signalId.getVersion());
            dst.setEventTypes(src.getEventTypesAsString());
            return dst;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create instance of  " + type.getName(), ex);
        }
    }

    private <M extends MetadataEvent> M toEvent(UnstagedEvent src, Class<M> type) {
        M dst;
        try {
            dst = type.getDeclaredConstructor().newInstance();
            copyEventProperties(src, dst);
            dst.setEventTemplateSourceId(src.getEventTemplateSourceId());
            dst.setEventSourceId(src.getId());
            dst.setPageIds(new HashSet<>(src.getPageIds()));
            dst.setModuleIds(new HashSet<>(src.getModuleIds()));
            dst.setClickIds(new HashSet<>(src.getClickIds()));
            return dst;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create instance of " + type.getName(), ex);
        }
    }

    private <M extends MetadataAttribute> M toAttribute(UnstagedAttribute src, Map<Long, Long> eventMap, Class<M> type) {
        val srcEventId = src.getParentId();
        val dstEventId = eventMap.get(srcEventId);
        Validate.isTrue(Objects.nonNull(dstEventId), "Dst eventId cannot be found for src eventId: " + srcEventId);
        M dst;
        try {
            dst = type.getDeclaredConstructor().newInstance();
            copyAttributeProperties(src, dst);
            dst.setEventId(dstEventId);
            return dst;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create instance of " + type.getName(), ex);
        }
    }

    private Set<Long> toAttributeIds(Collection<UnstagedAttribute> fieldAttributes, Map<Long, Long> eventAttributeMap) {
        if (MapUtils.isEmpty(eventAttributeMap)) {
            return new HashSet<>();
        }
        return fieldAttributes.stream()
                .map(srcAttr -> {
                    val srcAttrId = srcAttr.getId();
                    val dstAttrId = eventAttributeMap.get(srcAttrId);
                    Validate.isTrue(Objects.nonNull(dstAttrId), "Dst attributeId cannot be found for src attributeId: " + srcAttrId);
                    return dstAttrId;
                })
                .collect(toSet());
    }
}
