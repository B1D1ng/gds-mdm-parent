package com.ebay.behavior.gds.mdm.dec.service.udc;

import com.ebay.behavior.gds.mdm.common.config.UdcConfiguration;
import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.exception.UdcException;
import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;
import com.ebay.behavior.gds.mdm.common.service.token.UdcTokenGenerator;
import com.ebay.behavior.gds.mdm.common.service.udc.MetadataReadService;
import com.ebay.behavior.gds.mdm.common.service.udc.UdcEntityConverter;
import com.ebay.behavior.gds.mdm.common.service.udc.UdcIngestionService;
import com.ebay.behavior.gds.mdm.dec.model.Dataset;
import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalStorage;
import com.ebay.behavior.gds.mdm.dec.model.Pipeline;
import com.ebay.behavior.gds.mdm.dec.model.enums.AccessType;
import com.ebay.behavior.gds.mdm.dec.model.udc.ConsumableDataset;
import com.ebay.behavior.gds.mdm.dec.model.udc.DataStoreTableDetail;
import com.ebay.behavior.gds.mdm.dec.model.udc.InboundLineageParameters;
import com.ebay.behavior.gds.mdm.dec.model.udc.LogicalDataModel;
import com.ebay.behavior.gds.mdm.dec.model.udc.LogicalField;
import com.ebay.behavior.gds.mdm.dec.util.EntityUtils;
import com.ebay.com.google.common.annotations.VisibleForTesting;
import com.ebay.datagov.pushingestion.EntityRelationshipTarget;
import com.ebay.datagov.pushingestion.EntityVersionData;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ebay.behavior.gds.mdm.common.model.Model.COMMA;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.DATASET;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.DATA_TABLE;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.LDM;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.LDM_FIELD;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.RHEOS_KAFKA_TOPIC;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.SIGNAL;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.TRANSFORMATION;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.UNKNOWN;
import static com.ebay.behavior.gds.mdm.dec.util.ApiConstants.FIELDS;
import static com.ebay.datagov.pushingestion.EntityRelationshipTarget.createByKeyProperties;

@Slf4j
@Service
@Validated
@SuppressWarnings("PMD.TooManyMethods")
public class DecUdcWriteServiceImpl implements MetadataWriteService {

    public static final String LDM_ENTITY_RELATION_NAME = "belongToLogicalDataModelRelatedToLogicalField";

    public static final String LINEAGE_SIGNAL_INPUT_RELATION = "hasSignalAsInputRelatedToTransformation";
    public static final String LINEAGE_LDM_OUTPUT_RELATION = "hasLogicalDataModelAsOutputRelatedToTransformation";
    public static final String LINEAGE_LDM_INPUT_RELATION = "hasLogicalDataModelAsInputRelatedToTransformation";
    public static final String LINEAGE_DATASET_OUTPUT_RELATION = "hasConsumableDatasetAsOutputRelatedToTransformation";
    public static final String LINEAGE_DATASET_INPUT_RELATION = "hasConsumableDatasetAsInputRelatedToTransformation";
    public static final String LINEAGE_DATA_TABLE_OUTPUT_RELATION = "hasDataTableAsOutputRelatedToTransformation";
    public static final String LINEAGE_KAFKA_OUTPUT_RELATION = "hasRheosKafkaTopicAsOutputRelatedToTransformation";

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
    private ObjectMapper objectMapper;

    @Autowired
    private UdcIngestionService ingestService;

    private UdcDataSourceType dataSource;

    private static Map<String, Object> getInboundLineageProperties(InboundLineageParameters params, String csvInputIds, String outputId) {
        if (params.getEntityType() == TRANSFORMATION) {
            return Map.of(
                    "TransformationInputTable", csvInputIds,
                    "TransformationOutputTable", String.valueOf(outputId),
                    "TransformationTimestamp", System.currentTimeMillis());
        } else {
            throw new IllegalStateException("Unsupported lineage entity type: " + params.getEntityType());
        }
    }

    @PostConstruct
    private void init() {
        dataSource = config.getDataSource();
    }

    @Override
    public String deleteLogicalDataModel(@PositiveOrZero long id) {
        val ldmProxy = new LogicalDataModel();
        ldmProxy.setLogicalDataModelId(id);
        deleteAssociatedFields(id);
        return delete(ldmProxy);
    }

    private String deleteLogicalField(@PositiveOrZero long ldmId, @NotBlank String fieldName) {
        val ldmProxy = new LogicalField();
        ldmProxy.setLdmId(ldmId);
        ldmProxy.setLogicalFieldName(fieldName);
        return deleteField(ldmProxy);
    }

    @Override
    public String deleteConsumableDataset(@PositiveOrZero long id) {
        val ldmProxy = new ConsumableDataset();
        ldmProxy.setConsumableDatasetId(id);
        return delete(ldmProxy);
    }

    @Override
    public String deleteLineage(@NotNull @Valid UdcEntityType entityType, @NotBlank String id) {
        Map<String, Object> properties = Map.of(entityType.getIdName(), id);
        val entity = entityConverter.toEntity(entityType, dataSource, properties, Map.of());
        entity.setDeleted(true);
        return ingestService.ingest(entity, id);
    }

    private String delete(Metadata metadata) {
        val entity = entityConverter.toDeleteEntity(metadata, dataSource);
        return ingestService.ingest(entity, metadata.getId());
    }

    private String deleteField(LogicalField field) {
        val entity = entityConverter.toEntity(field, Map.of(), dataSource);
        entity.setDeleted(true);
        return ingestService.ingest(entity, field.getCompositeId());
    }

    @Override
    public String upsertLogicalDataModel(@Valid @NotNull LdmBaseEntity ldm, @Valid @NotNull Namespace namespace,
                                         Set<Long> upstreamLdmIds, Set<Long> lastUpstreamLdmIds) {
        String entityId = null;
        try {
            // need delete ldm field first and insert the new list of fields
            deleteAssociatedFields(ldm.getId());

            // clear ldm -> ldm lineage
            Set<Long> idsToDelete = getIdsToDelete(upstreamLdmIds, lastUpstreamLdmIds);
            if (!idsToDelete.isEmpty()) {
                updateLdmLineage(idsToDelete, ldm.getId(), true);
            }

            // ingest ldm, with ldm -> ldm lineage
            LogicalDataModel model = EntityUtils.mapToLogicalDataModel(ldm, namespace);
            entityId = upsertLogicalDataModel(model, upstreamLdmIds).get(LDM);

            // ingest logical field with ldm -> field relationship
            if (model.getFields() != null) {
                model.getFields().forEach(this::upsertLogicalField);
            }

            return entityId;
        } catch (Exception ex) {
            throw new UdcException(Optional.ofNullable(entityId).orElse(UNKNOWN), ex);
        }
    }

    @Override
    public Map<UdcEntityType, String> upsertLogicalDataModel(@Valid @NotNull LogicalDataModel model, Set<Long> upstreamLdmIds) {
        val entity = entityConverter.toEntity(model, dataSource);
        val entityId = ingestService.ingest(entity, model.getId());
        val entityIdMap = new HashMap<>(Map.of(LDM, entityId));

        if (CollectionUtils.isEmpty(upstreamLdmIds)) {
            return entityIdMap;
        }

        // N:1 ldm -> ldm lineage
        Map<UdcEntityType, String> lineageIdMap = updateLdmLineage(upstreamLdmIds, model.getId(), false);
        entityIdMap.putAll(lineageIdMap);

        return entityIdMap;
    }

    private Set<Long> getIdsToDelete(Set<Long> upstreamLdmIds, Set<Long> lastUpstreamLdmIds) {
        if (CollectionUtils.isEmpty(lastUpstreamLdmIds)) {
            return new HashSet<>();
        }
        if (CollectionUtils.isEmpty(upstreamLdmIds)) {
            return lastUpstreamLdmIds;
        }
        // get the upstream ldm ids that are not in the current upstream ldm ids
        return lastUpstreamLdmIds.stream()
                .filter(id -> !upstreamLdmIds.contains(id))
                .collect(Collectors.toSet());
    }

    private Map<UdcEntityType, String> updateLdmLineage(Set<Long> upstreamLdmIds, Long ldmId, boolean isDelete) {
        // N:1 ldm -> ldm lineage
        val lineageType = TRANSFORMATION;
        val lineageParams = new InboundLineageParameters(
                lineageType, upstreamLdmIds, LINEAGE_LDM_INPUT_RELATION, LDM, LDM, ldmId.toString(), LINEAGE_LDM_OUTPUT_RELATION);

        val lineageEntity = toLineageEntity(lineageParams, isDelete);
        try {
            val lineageEntityId = ingestService.ingest(lineageEntity, ldmId);
            return Map.of(lineageType, lineageEntityId);
        } catch (UdcException ex) {
            deleteLogicalDataModel(ldmId); // rollback ldm
            throw ex;
        }
    }

    private void deleteAssociatedFields(long ldmId) {
        try {
            val ldm = readService.getEntityById(LDM, ldmId);
            val ldmProperties = ldm.getProperties();
            if (ldmProperties == null || !ldmProperties.containsKey(FIELDS)
                    || ldmProperties.get(FIELDS) == null || !(ldmProperties.get(FIELDS) instanceof List<?> ldmFields)) {
                return;
            }
            val fields = ldmFields.stream().map(f -> objectMapper.convertValue(f, LogicalField.class)).toList();
            fields.forEach(field -> deleteLogicalField(ldmId, field.getLogicalFieldName()));
        } catch (DataNotFoundException ignored) {
        }
    }

    @Override
    public Map<UdcEntityType, String> upsertSignalToLdmLineage(Long ldmId, Set<Long> upstreamSignalIds) {
        // lineage
        val lineageType = TRANSFORMATION;
        val lineageParams = new InboundLineageParameters(
                lineageType, upstreamSignalIds, LINEAGE_SIGNAL_INPUT_RELATION, SIGNAL, LDM, ldmId.toString(), LINEAGE_LDM_OUTPUT_RELATION);

        val lineageEntity = toLineageEntity(lineageParams, dataSource);
        val lineageEntityId = ingestService.ingest(lineageEntity, ldmId);
        return Map.of(lineageType, lineageEntityId);
    }

    public Map<UdcEntityType, String> upsertDatasetPhysicalRelation(@Valid @NotNull ConsumableDataset dataset) {
        try {
            DataStoreTableDetail detail = dataset.getStoreTableDetail();
            if (detail == null) {
                return Map.of();
            }

            String outputRelationType;
            List<EntityRelationshipTarget> outputRelations;
            if (dataset.getAccessType() == AccessType.HIVE || dataset.getAccessType() == AccessType.ICEBERG) {
                outputRelationType = LINEAGE_DATA_TABLE_OUTPUT_RELATION;
                outputRelations = List.of(createByKeyProperties(DATA_TABLE.getValue(),
                        Map.of("DataStoreType", detail.getDataStoreType(),
                                "DataStoreName", detail.getDataStoreName(),
                                "DataTableName", detail.getDataTableName())));
            } else if (dataset.getAccessType() == AccessType.STREAM) {
                outputRelationType = LINEAGE_KAFKA_OUTPUT_RELATION;
                outputRelations = List.of(createByKeyProperties(RHEOS_KAFKA_TOPIC.getValue(),
                        Map.of("DataStoreType", detail.getDataStoreType(),
                                "DataStoreName", detail.getDataStoreName(),
                                "DataTableName", detail.getDataTableName())));
            } else {
                throw new IllegalStateException("Unsupported access type: " + dataset.getAccessType());
            }

            val inputRelations = List.of(createByKeyProperties(DATASET.getValue(),
                    Map.of(DATASET.getIdName(), dataset.getId())));
            val relationMap = Map.of(
                    LINEAGE_DATASET_INPUT_RELATION, inputRelations,
                    outputRelationType, outputRelations
            );
            Map<String, Object> properties = Map.of(
                    "TransformationInputTable", dataset.getId().toString(),
                    "TransformationOutputTable", String.valueOf(detail.getDataTableName()),
                    "TransformationTimestamp", System.currentTimeMillis());
            val lineageEntity = entityConverter.toEntity(TRANSFORMATION, dataSource, properties, relationMap);

            val entityId  = ingestService.ingest(lineageEntity, dataset.getId());
            return Map.of(TRANSFORMATION, entityId);
        } catch (UdcException ex) {
            log.error("Failed to upsert dataset physical relation for dataset id: {}", dataset.getId(), ex);
        }
        return Map.of();
    }

    @Override
    public Map<UdcEntityType, String> upsertConsumableDataset(@Valid @NotNull Dataset dataset, @Valid @NotNull Namespace namespace, @NotNull Long ldmId,
                                                              @NotEmpty String ldmName, PhysicalStorage storage, Long lastLdmId) {
        try {
            // clear ldm -> dataset lineage
            if (lastLdmId != null && !Objects.equals(ldmId, lastLdmId)) {
                updateDatasetLineage(lastLdmId, dataset.getId(), true);
            }

            String pipelineId = null;
            DataStoreTableDetail storeTableDetail = null;
            AccessType accessType = null;
            if (storage != null) {
                storeTableDetail = EntityUtils.getStorageDetail(objectMapper, storage);
                accessType = storage.getAccessType();
                val pipelines = storage.getPipelines();
                if (pipelines != null && !pipelines.isEmpty()) {
                    val pipelineIds = pipelines.stream().map(Pipeline::getPipelineId).toList();
                    pipelineId = pipelineIds.get(0);
                }
            }
            // update dataset, with ldm -> dataset lineage
            ConsumableDataset model = EntityUtils.mapToConsumableDataset(
                    objectMapper, dataset, namespace, ldmId, ldmName, accessType, storeTableDetail, pipelineId);
            return upsertConsumableDataset(model);
        } catch (Exception ex) {
            throw new UdcException(String.valueOf(dataset.getId()), ex);
        }
    }

    @Override
    public Map<UdcEntityType, String> upsertConsumableDataset(@Valid @NotNull ConsumableDataset dataset) {
        val entity = entityConverter.toEntity(dataset, dataSource);
        val entityId = ingestService.ingest(entity, dataset.getId());
        val entityIdMap = new HashMap<>(Map.of(DATASET, entityId));

        if (dataset.getLdmId() == null) {
            return entityIdMap;
        }

        // ldm -> dataset lineage
        Map<UdcEntityType, String> lineageIdMap = updateDatasetLineage(dataset.getLdmId(), dataset.getId(), false);
        entityIdMap.putAll(lineageIdMap);

        // dataset -> table/kafka lineage
        upsertDatasetPhysicalRelation(dataset);

        return entityIdMap;
    }

    private Map<UdcEntityType, String> updateDatasetLineage(Long ldmId, Long datasetId, boolean isDelete) {
        val lineageType = TRANSFORMATION;
        val lineageParams = new InboundLineageParameters(
                lineageType, Set.of(ldmId), LINEAGE_LDM_INPUT_RELATION, LDM, DATASET, datasetId.toString(), LINEAGE_DATASET_OUTPUT_RELATION);
        val ldmLineageEntity = toLineageEntity(lineageParams, isDelete);
        try {
            val ldmLineageEntityId = ingestService.ingest(ldmLineageEntity, datasetId);
            return Map.of(lineageType, ldmLineageEntityId);
        } catch (UdcException ex) {
            deleteConsumableDataset(datasetId); // rollback
            throw ex;
        }
    }

    private Map<UdcEntityType, String> upsertLogicalField(@Valid @NotNull LogicalField field) {
        val belongToLogicalDataModel = entityConverter.toRelationList(LDM, field.getLdmId());
        val relationMap = entityConverter.toRelationMap(LDM_ENTITY_RELATION_NAME, belongToLogicalDataModel);
        val entity = entityConverter.toEntity(field, relationMap, dataSource);
        val entityId = ingestService.ingest(entity, field.getCompositeId());
        return Map.of(LDM_FIELD, entityId);
    }

    private EntityVersionData toLineageEntity(InboundLineageParameters lineageParams, boolean isDelete) {
        val lineageEntity = toLineageEntity(lineageParams, dataSource);
        if (isDelete) {
            lineageEntity.setDeleted(true);
        }
        return lineageEntity;
    }

    @VisibleForTesting
    protected EntityVersionData toLineageEntity(InboundLineageParameters params, UdcDataSourceType dataSource) {
        val outputId = params.getOutputEntityId();
        val csvInputIds = params.getInputEntityIds().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(COMMA));

        val inputRelations = params.getInputEntityIds().stream()
                .map(id -> entityConverter.toRelation(params.getInputEntityType(), params.getInputEntityIdName(), id))
                .toList();
        val outputRelations = List.of(createByKeyProperties(params.getOutputEntityType().getValue(), Map.of(params.getOutputEntityIdName(), outputId)));

        val relationMap = Map.of(
                params.getInputRelationType(), inputRelations,
                params.getOutputRelationType(), outputRelations
        );

        val properties = getInboundLineageProperties(params, csvInputIds, outputId);
        return entityConverter.toEntity(params.getEntityType(), dataSource, properties, relationMap);
    }
}
