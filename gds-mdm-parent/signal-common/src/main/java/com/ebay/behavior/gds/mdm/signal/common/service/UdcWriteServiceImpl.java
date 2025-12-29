package com.ebay.behavior.gds.mdm.signal.common.service;

import com.ebay.behavior.gds.mdm.common.config.UdcConfiguration;
import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.exception.UdcException;
import com.ebay.behavior.gds.mdm.common.model.LineageParameters;
import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.common.model.Model;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;
import com.ebay.behavior.gds.mdm.common.service.token.UdcTokenGenerator;
import com.ebay.behavior.gds.mdm.common.service.udc.MetadataReadService;
import com.ebay.behavior.gds.mdm.common.service.udc.UdcEntityConverter;
import com.ebay.behavior.gds.mdm.common.service.udc.UdcIngestionService;
import com.ebay.behavior.gds.mdm.common.service.udc.UdcLineageHelper;
import com.ebay.behavior.gds.mdm.common.util.TimeUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedAttribute;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.datagov.pushingestion.EntityVersionData;

import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.COLUMN_TRANSFORMATION;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.EVENT;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.FIELD;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.SIGNAL;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.TRANSFORMATION;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.UNKNOWN;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@Slf4j
@Service
@Validated
public class UdcWriteServiceImpl implements MetadataWriteService {

    public static final String SIGNAL_ENTITY_RELATION_NAME = "belongToSignalRelatedToSignalField";
    public static final String EVENT_ATTRIBUTE_RELATION_NAME = "belongToUnifiedEventRelatedToUnifiedEventAttribute";

    public static final String LINEAGE_FIELD_OUTPUT_RELATION = "hasSignalFieldAsOutputRelatedToColumnTransformation";
    public static final String LINEAGE_ATTRIBUTE_INPUT_RELATION = "hasUnifiedEventAttributeAsInputRelatedToColumnTransformation";
    public static final String LINEAGE_SIGNAL_OUTPUT_RELATION = "hasSignalAsOutputRelatedToTransformation";
    public static final String LINEAGE_EVENT_INPUT_RELATION = "hasUnifiedEventAsInputRelatedToTransformation";

    public static final UdcEntityType SIGNAL_EVENT_LINEAGE_TYPE = TRANSFORMATION;
    public static final UdcEntityType FIELD_ATTRIBUTE_LINEAGE_TYPE = COLUMN_TRANSFORMATION;

    @Autowired
    private UdcConfiguration config;

    @Getter
    @Autowired
    private UdcTokenGenerator tokenGenerator;

    @Autowired
    private MetadataReadService readService;

    @Autowired
    private UdcEntityConverter entityConverter;

    @Autowired
    private UdcLineageHelper lineageHelper;

    @Autowired
    private UdcIngestionService ingestService;

    private UdcDataSourceType dataSource;

    @PostConstruct
    public void init() {
        dataSource = config.getDataSource();
    }

    @Override
    public String deleteSignal(@PositiveOrZero long id) {
        val signalProxy = new UnstagedSignal();
        signalProxy.setId(id);
        return delete(signalProxy, dataSource);
    }

    private String deleteSignal(long id, UdcDataSourceType dataSource) {
        val signalProxy = new UnstagedSignal();
        signalProxy.setId(id);
        return delete(signalProxy, dataSource);
    }

    @VisibleForTesting
    public String deleteSignalWithAssociations(long signalId, UdcDataSourceType dataSource) {
        try {
            deleteAssociatedEntities(signalId, dataSource);
            return deleteSignal(signalId, dataSource);
        } catch (Exception ex) {
            throw new UdcException(String.valueOf(signalId), ex);
        }
    }

    @Override
    public String deleteField(@PositiveOrZero long id) {
        return delete(UnstagedField.builder().id(id).build(), dataSource);
    }

    private String deleteField(long id, UdcDataSourceType dataSource) {
        return delete(UnstagedField.builder().id(id).build(), dataSource);
    }

    @Override
    public String deleteEvent(@PositiveOrZero long id) {
        return delete(UnstagedEvent.builder().id(id).build(), dataSource);
    }

    private String deleteEvent(long id, UdcDataSourceType dataSource) {
        return delete(UnstagedEvent.builder().id(id).build(), dataSource);
    }

    @Override
    public String deleteAttribute(@PositiveOrZero long id) {
        return delete(UnstagedAttribute.builder().id(id).build(), dataSource);
    }

    private String deleteAttribute(long id, UdcDataSourceType dataSource) {
        return delete(UnstagedAttribute.builder().id(id).build(), dataSource);
    }

    public String delete(UdcEntityType type, @NotBlank String id) {
        Map<String, Object> properties = Map.of(type.getIdName(), id);
        val entity = entityConverter.toEntity(type, dataSource, properties, Map.of());
        entity.setDeleted(true);
        return ingestService.ingest(entity, id);
    }

    private String delete(Metadata metadata, UdcDataSourceType dataSource) {
        val entity = entityConverter.toDeleteEntity(metadata, dataSource);
        return ingestService.ingest(entity, metadata.getId());
    }

    private String deleteLineageEntity(EntityVersionData entity) {
        entity.setDeleted(true);
        return ingestService.ingest(entity, "lineage_id"); // ID have no use in UDC for deletion of lineage entities
    }

    /**
     * UnstagedEvent must include associated events and fields.
     * That can be achieved by calling getByIdWithAssociationsRecursive() method
     */
    @Override
    public Map<UdcEntityType, String> upsertSignal(Set<Long> eventIds, @Valid @NotNull UnstagedSignal signal) {
        return upsertSignal(eventIds, signal, dataSource);
    }

    private Map<UdcEntityType, String> upsertSignal(Set<Long> eventIds, UnstagedSignal signal, UdcDataSourceType dataSource) {
        // signal
        val entity = entityConverter.toEntity(signal, dataSource);
        val entityId = ingestService.ingest(entity, signal.getId());

        if (CollectionUtils.isEmpty(eventIds)) {
            return Map.of(SIGNAL, entityId);
        }

        // lineage
        val lineageEntity = toSignalEventLineageEntity(eventIds, signal, dataSource);

        try {
            val lineageEntityId = ingestService.ingest(lineageEntity, signal.getId());
            return Map.of(SIGNAL, entityId, SIGNAL_EVENT_LINEAGE_TYPE, lineageEntityId);
        } catch (UdcException ex) {
            deleteSignalWithAssociations(signal.getId(), dataSource); // rollback signal
            throw ex;
        }
    }

    private EntityVersionData toSignalEventLineageEntity(Set<Long> eventIds, UnstagedSignal signal, UdcDataSourceType dataSource) {
        val lineageParams = new LineageParameters(
                SIGNAL_EVENT_LINEAGE_TYPE, signal, LINEAGE_SIGNAL_OUTPUT_RELATION,
                new UnstagedEvent(), eventIds, LINEAGE_EVENT_INPUT_RELATION);
        return lineageHelper.toLineageEntity(lineageParams, dataSource);
    }

    private EntityVersionData toFieldAttributeLineageEntity(Set<Long> attributeIds, UnstagedField field, UdcDataSourceType dataSource) {
        val lineageParams = new LineageParameters(
                FIELD_ATTRIBUTE_LINEAGE_TYPE, field, LINEAGE_FIELD_OUTPUT_RELATION,
                new UnstagedAttribute(), attributeIds, LINEAGE_ATTRIBUTE_INPUT_RELATION);
        return lineageHelper.toLineageEntity(lineageParams, dataSource);
    }

    /**
     * UnstagedField must include associated attributes.
     * That can be achieved by calling getByIdWithAssociationsRecursive() method
     */
    @Override
    public Map<UdcEntityType, String> upsertField(@PositiveOrZero long signalId, Set<Long> attributeIds, @Valid @NotNull UnstagedField field) {
        return upsertField(signalId, attributeIds, field, dataSource);
    }

    private Map<UdcEntityType, String> upsertField(long signalId, Set<Long> attributeIds, UnstagedField field, UdcDataSourceType dataSource) {
        val belongToSignal = entityConverter.toRelationList(SIGNAL, signalId);
        val relationMap = entityConverter.toRelationMap(SIGNAL_ENTITY_RELATION_NAME, belongToSignal);
        val entity = entityConverter.toEntity(field, relationMap, dataSource);
        val entityId = ingestService.ingest(entity, field.getId());

        if (CollectionUtils.isEmpty(attributeIds)) {
            return Map.of(FIELD, entityId);
        }

        try {
            val lineageEntity = toFieldAttributeLineageEntity(attributeIds, field, dataSource);
            val lineageEntityId = ingestService.ingest(lineageEntity, field.getId());
            return Map.of(FIELD, entityId, FIELD_ATTRIBUTE_LINEAGE_TYPE, lineageEntityId);
        } catch (UdcException ex) {
            deleteField(field.getId()); // rollback field
            throw ex;
        }
    }

    /**
     * UnstagedEvent must include associated attributes.
     * That can be achieved by calling getByIdWithAssociationsRecursive() method
     */
    @Override
    public String upsertEvent(@Valid @NotNull UnstagedEvent event) {
        return upsertEvent(event, dataSource);
    }

    private String upsertEvent(UnstagedEvent event, UdcDataSourceType dataSource) {
        return ingestService.ingest(entityConverter.toEntity(event, dataSource), event.getId());
    }

    @Override
    public String upsertAttribute(@PositiveOrZero long eventId, @Valid @NotNull UnstagedAttribute attribute) {
        return upsertAttribute(eventId, attribute, dataSource);
    }

    private String upsertAttribute(long eventId, UnstagedAttribute attribute, UdcDataSourceType dataSource) {
        val belongToEvent = entityConverter.toRelationList(EVENT, eventId);
        val relationMap = entityConverter.toRelationMap(EVENT_ATTRIBUTE_RELATION_NAME, belongToEvent);
        val entity = entityConverter.toEntity(attribute, relationMap, dataSource);

        return ingestService.ingest(entity, attribute.getId());
    }

    /**
     * Inject whole signal with all associated events, attributes, fields.
     * in case of existing signal, deletes its associated events, attributes, fields and injects new ones.
     *
     * @param signal An UnstagedSignal. Better be detached entity.
     * @return Injected entityId of the signal.
     */
    @Override
    public String upsert(@Valid @NotNull UnstagedSignal signal, @NotNull UdcDataSourceType dataSource) {
        val signalId = signal.getSignalId();
        String signalEntityId = null;
        Validate.isTrue(isNotEmpty(signal.getEvents()), "Signal must have at least one event");
        Validate.isTrue(isNotEmpty(signal.getFields()), "Signal must have at least one field");
        try {
            // Old data must be deleted first, but we have to keep the signal
            deleteAssociatedEntities(signalId.getId(), dataSource);

            // inject events and attributes
            signal.getEvents().forEach(event -> {
                upsertEvent(event, dataSource);
                val attributes = event.getAttributes();
                attributes.forEach(attribute -> upsertAttribute(event.getId(), attribute, dataSource));
            });

            // inject signal and fields
            val eventIds = signal.getEvents().stream()
                    .map(Model::getId)
                    .collect(toSet());
            signalEntityId = upsertSignal(eventIds, signal, dataSource)
                    .get(signal.getEntityType());

            // inject fields
            signal.getFields().forEach(field -> {
                val attributeIds = field.getAttributes().stream()
                        .map(Model::getId)
                        .collect(toSet());
                upsertField(signalId.getId(), attributeIds, field, dataSource);
            });

            return signalEntityId;
        } catch (Exception ex) {
            throw new UdcException(Optional.ofNullable(signalEntityId).orElse(UNKNOWN), ex);
        }
    }

    /**
     * Deletes all associated entities of the signal: events, fields, attributes.
     * Also deletes lineage entities for events and fields.
     */
    private void deleteAssociatedEntities(long signalId, UdcDataSourceType dataSource) {
        try {
            val signal = readService.getById(SIGNAL, signalId, UnstagedSignal.class);
            val eventIds = signal.getEvents().stream()
                    .map(Model::getId)
                    .collect(toSet());

            if (isNotEmpty(eventIds)) { // delete lineage event-to-signal entities
                val eventsLineageEntity = toSignalEventLineageEntity(eventIds, signal, dataSource);
                deleteLineageEntity(eventsLineageEntity);
            }

            val fields = signal.getFields();
            for (val field : fields) {
                val attributeIds = field.getAttributes().stream().map(Model::getId).collect(toSet());
                if (isNotEmpty(attributeIds)) { // delete lineage field-to-attribute entities
                    val lineageEntity = toFieldAttributeLineageEntity(attributeIds, field, dataSource);
                    deleteLineageEntity(lineageEntity);
                }
            }

            val attributes = fields.stream()
                    .flatMap(field -> field.getAttributes().stream())
                    .toList();

            signal.getFields().forEach(field -> deleteField(field.getId(), dataSource));
            attributes.forEach(attribute -> deleteAttribute(attribute.getId(), dataSource));
            signal.getEvents().forEach(event -> deleteEvent(event.getId(), dataSource));
        } catch (DataNotFoundException ignored) {
        } finally {
            TimeUtils.sleepSeconds(3);
        }
    }
}