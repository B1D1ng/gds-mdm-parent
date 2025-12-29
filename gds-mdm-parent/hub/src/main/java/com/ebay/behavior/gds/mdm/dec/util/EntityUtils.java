package com.ebay.behavior.gds.mdm.dec.util;

import com.ebay.behavior.gds.mdm.dec.model.Dataset;
import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmField;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalStorage;
import com.ebay.behavior.gds.mdm.dec.model.enums.AccessType;
import com.ebay.behavior.gds.mdm.dec.model.enums.CodeLanguageType;
import com.ebay.behavior.gds.mdm.dec.model.enums.NamespaceType;
import com.ebay.behavior.gds.mdm.dec.model.enums.ViewType;
import com.ebay.behavior.gds.mdm.dec.model.udc.AccessDetail;
import com.ebay.behavior.gds.mdm.dec.model.udc.ConsumableDataset;
import com.ebay.behavior.gds.mdm.dec.model.udc.DataStoreTableDetail;
import com.ebay.behavior.gds.mdm.dec.model.udc.LogicalDataModel;
import com.ebay.behavior.gds.mdm.dec.model.udc.LogicalDataModelType;
import com.ebay.behavior.gds.mdm.dec.model.udc.LogicalField;
import com.ebay.behavior.gds.mdm.dec.model.udc.LogicalView;
import com.ebay.behavior.gds.mdm.dec.repository.LdmEntityRepository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.UNCHECKED;

/**
 * Utility class for entity-related operations.
 * Organized into subclasses by functionality to improve maintainability and cohesion.
 */
@Slf4j
@UtilityClass
public class EntityUtils {

    public static final String TYPE = "type";
    public static final String RETENTION = "retention";
    public static final String FREQUENCY = "frequency";
    public static final String LATENCY = "latency";
    
    /**
     * Field-related utilities for working with LdmField entities
     */
    @Slf4j
    @UtilityClass
    public static class FieldUtils {

        /**
         * Sorts a set of LdmField objects by their ordinal value from smallest to largest.
         *
         * @param fields the set of LdmField objects to sort
         * @return a new LinkedHashSet containing the sorted fields, preserving the sorted order
         */
        public static Set<LdmField> getSortedFieldsByOrdinal(Set<LdmField> fields) {
            if (fields == null || fields.isEmpty()) {
                return new HashSet<>();
            }

            return fields.stream()
                    .sorted(Comparator.comparing(LdmField::getOrdinal, Comparator.nullsLast(Integer::compareTo)))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }
    
    /**
     * Entity-related utilities for working with LDM entities
     */
    @Slf4j
    @UtilityClass
    public static class LdmUtils {

        /**
         * Recursively collects all downstream LDM IDs from a set of initial IDs.
         */
        public static void collectDownstreamLdm(Set<Long> ids, Set<Long> downstreamLdmIdSet, LdmEntityRepository repository) {
            Set<Long> downstreamLdmIds = new HashSet<>();
            for (Long id : ids) {
                List<LdmEntity> downstreamLdmEntities = repository.findAllByUpstreamLdmId(id);
                Set<Long> downstreamIds = downstreamLdmEntities.stream()
                        .map(LdmEntity::getId)
                        .filter(downstreamId -> !downstreamLdmIdSet.contains(downstreamId))
                        .collect(Collectors.toSet());
                log.info("Downstream LDM ids {} for id {}", downstreamIds, id);
                downstreamLdmIds.addAll(downstreamIds);
            }

            if (downstreamLdmIds.isEmpty()) {
                return;
            }

            downstreamLdmIdSet.addAll(downstreamLdmIds);
            collectDownstreamLdm(downstreamLdmIds, downstreamLdmIdSet, repository);
        }
        
        /**
         * Creates a copy of an LDM entity with a new update user.
         */
        public static LdmEntity copyLdm(LdmEntity source, EntityManager entityManager, String updateBy) {
            LdmEntity target = new LdmEntity();
            BeanUtils.copyProperties(source, target, "updateBy", "updateDate");
            target.setUpdateBy(updateBy);
            if (target.getFields() != null) {
                target.getFields().forEach(f -> {
                    entityManager.detach(f);
                    if (f.getSignalMapping() != null) {
                        f.getSignalMapping().forEach(entityManager::detach);
                    }
                    if (f.getPhysicalStorageMapping() != null) {
                        f.getPhysicalStorageMapping().forEach(entityManager::detach);
                    }
                });
            }
            return target;
        }

        /**
         * Formats an LDM name based on entity name and view type.
         */
        public static String getLdmName(String entityName, ViewType viewType) {
            if (entityName == null || entityName.isEmpty()) {
                return entityName;
            }

            if (viewType == null || viewType == ViewType.NONE) {
                return entityName.toLowerCase(Locale.US);
            }

            return String.format("%s_%s", entityName, viewType).toLowerCase(Locale.US);
        }
        
        public static LdmEntity copyBasicInfoFromBaseEntity(LdmEntity entity, LdmBaseEntity baseEntity) {
            entity.setPk(baseEntity.getPk());
            entity.setOwners(baseEntity.getOwners());
            entity.setJiraProject(baseEntity.getJiraProject());
            entity.setTeam(baseEntity.getTeam());
            entity.setTeamDl(baseEntity.getTeamDl());
            return entity;
        }
    }
    
    /**
     * Mapping utilities for converting between different entity representations
     */
    @Slf4j
    @UtilityClass
    public static class MapperUtils {

        /**
         * Maps a Dataset to a ConsumableDataset model.
         */
        public static ConsumableDataset mapToConsumableDataset(ObjectMapper objectMapper, Dataset dataset,
                                                            Namespace namespace, Long ldmId, String ldmName,
                                                            AccessType accessType, DataStoreTableDetail storeTableDetail, String pipelineId) {
            ConsumableDataset consumableDataset = ConsumableDataset.builder()
                    .consumableDatasetId(dataset.getId())
                    .consumableDatasetName(dataset.getName())
                    .ldmId(ldmId)
                    .ldmName(ldmName)
                    .owners(dataset.getOwnersAsList())
                    .status(dataset.getStatus())
                    .namespace(namespace.getName())
                    .createBy(dataset.getCreateBy())
                    .updateBy(dataset.getUpdateBy())
                    .createDate(dataset.getCreateDate())
                    .updateDate(dataset.getUpdateDate())
                    .storeTableDetail(storeTableDetail)
                    .accessType(accessType)
                    .build();

            Map<String, Object> consumptionSpec = parseConsumptionSpecs(objectMapper, dataset.getConsumptionParameters());
            List<AccessDetail> accessInfos = new ArrayList<>();
            AccessDetail accessInfo = AccessDetail.builder()
                    .accessMode(TextUtils.getStr(JsonUtils.getFromObjectMap(consumptionSpec, TYPE)))
                    .latency(TextUtils.getStr(JsonUtils.getFromObjectMap(consumptionSpec, LATENCY)))
                    .retention(TextUtils.getStr(JsonUtils.getFromObjectMap(consumptionSpec, RETENTION)))
                    .frequency(TextUtils.getStr(JsonUtils.getFromObjectMap(consumptionSpec, FREQUENCY)))
                    .account("b_gdsdec")
                    .backendSystemParameters(dataset.getRuntimeConfigurations())
                    .entityName(storeTableDetail.getDataTableName())
                    .pipelineId(pipelineId)
                    .build();
            accessInfos.add(accessInfo);
            consumableDataset.setAccessDetails(accessInfos);
            return consumableDataset;
        }

        private Map<String, Object> parseConsumptionSpecs(ObjectMapper objectMapper, String consumptionParameters) {
            Map<String, Object> result = new HashMap<>();
            if (consumptionParameters == null || consumptionParameters.isEmpty()) {
                return result;
            }

            Map<String, Object> root = TextUtils.readJson(objectMapper, consumptionParameters, new TypeReference<Map<String, Object>>() {});
            if (root.get(TYPE) == null) {
                return result;
            }
            AccessType accessType = AccessType.valueOf(String.valueOf(root.get(TYPE)).toUpperCase(Locale.US));
            result.put(TYPE, accessType);
            Object spec = root.get(accessType.toString().toLowerCase(Locale.US));
            if (spec instanceof Map) {
                Map<String, Object> specMap = JsonUtils.toMap(spec);
                result.put(RETENTION, JsonUtils.getFromObjectMap(JsonUtils.toMap(specMap.get("retention")), "period"));
                result.put(LATENCY, JsonUtils.getFromObjectMap(JsonUtils.toMap(specMap.get("dataLatency")), "threshold"));
                result.put(FREQUENCY, JsonUtils.getFromObjectMap(JsonUtils.toMap(specMap.get("frequency")), "interval"));
            }
            return result;
        }

        /**
         * Maps an LdmBaseEntity to a LogicalDataModel model.
         */
        public static LogicalDataModel mapToLogicalDataModel(LdmBaseEntity ldm, Namespace namespace) {
            LogicalDataModel model = LogicalDataModel.builder()
                    .logicalDataModelId(ldm.getId())
                    .logicalDataModelName(ldm.getName())
                    .description(ldm.getDescription())
                    .owners(ldm.getOwnersAsList())
                    .jiraProject(ldm.getJiraProject())
                    .domain(ldm.getDomain())
                    .pk(ldm.getPk())
                    .namespace(namespace.getName())
                    .team(ldm.getTeam())
                    .teamDl(ldm.getTeamDl())
                    .isDcs(ldm.getViews() != null && !ldm.getViews().isEmpty() ? ldm.getViews().get(0).getIsDcs() : Boolean.FALSE)
                    .dcsFields(ldm.getViews() != null && !ldm.getViews().isEmpty() ? ldm.getViews().get(0).getDcsFields() : List.of())
                    .createBy(ldm.getCreateBy())
                    .updateBy(ldm.getUpdateBy())
                    .createDate(ldm.getCreateDate())
                    .updateDate(ldm.getUpdateDate())
                    .build();
            if (namespace.getType() != NamespaceType.BASE) {
                if (!ldm.getViews().isEmpty()) {
                    CodeLanguageType language = ldm.getViews().get(0).getCodeLanguage();
                    String code = ldm.getViews().get(0).getCodeContent();
                    model.setLanguage(String.valueOf(language));
                    model.setCode(code);
                    model.setCreateBy(ldm.getCreateBy());
                    model.setUpdateBy(ldm.getUpdateBy());
                    model.setCreateDate(ldm.getCreateDate());
                    model.setUpdateDate(ldm.getUpdateDate());
                }
                model.setType(LogicalDataModelType.DERIVED);
            } else {
                if (!ldm.getViews().isEmpty()) {
                    List<LdmEntity> views = ldm.getViews().stream().filter(x -> x.getUpdateDate() != null)
                            .sorted(Comparator.comparing(LdmEntity::getUpdateDate).reversed()).toList();
                    if (!views.isEmpty() && (ldm.getUpdateDate() == null || views.get(0).getUpdateDate().after(ldm.getUpdateDate()))) {
                        LdmEntity latestChangedView = views.get(0);
                        model.setUpdateBy(latestChangedView.getUpdateBy());
                        model.setUpdateDate(latestChangedView.getUpdateDate());
                    }
                }
                model.setType(LogicalDataModelType.BASE);
            }
            // add fields & Views
            Map<String, LogicalField> fieldMap = new HashMap<>();
            Map<String, Set<ViewType>> fieldViewMap = new HashMap<>();
            List<LogicalView> viewList = getViewList(ldm, fieldMap, fieldViewMap);
            List<LogicalView> finalizedViewList = viewList.stream().filter(v -> v.getViewType() != ViewType.NONE).toList();
            model.setViews(finalizedViewList);

            List<LogicalField> fieldList = new ArrayList<>();
            fieldMap.forEach((k, v) -> {
                Set<ViewType> fViewSet = fieldViewMap.get(k);
                v.setViews(fViewSet.stream().filter(x -> !ViewType.NONE.equals(x)).toList());
                fieldList.add(v);
            });
            model.setFields(fieldList);

            return model;
        }

        private List<LogicalView> getViewList(LdmBaseEntity ldm, Map<String, LogicalField> fieldMap, Map<String, Set<ViewType>> fieldViewMap) {
            List<LdmEntity> views = new ArrayList<>(ldm.getViews()).stream()
                    .sorted(Comparator.comparing(v -> v.getViewType() == ViewType.RAW ? 1 : 0))
                    .toList();
            List<LogicalView> viewList = new ArrayList<>();
            for (LdmEntity view : views) {
                Set<LdmField> viewFields = FieldUtils.getSortedFieldsByOrdinal(view.getFields());
                for (LdmField f : viewFields) {
                    LogicalField field = LogicalField.builder()
                            .id(f.getId())
                            .logicalFieldName(f.getName())
                            .ldmId(ldm.getId())
                            .description(f.getDescription())
                            .dataType(f.getDataType())
                            .build();
                    String fKey = field.getCompositeId();
                    Set<ViewType> viewSet = fieldViewMap.getOrDefault(fKey, new HashSet<>());
                    viewSet.add(view.getViewType());
                    fieldViewMap.put(fKey, viewSet);
                    fieldMap.put(fKey, field);
                }
                LogicalView logicalView = LogicalView.builder()
                        .viewType(view.getViewType())
                        .viewName(String.format("%s_%s", ldm.getName(), view.getViewType()).toLowerCase(Locale.US))
                        .version(view.getVersion().toString())
                        .updateBy(view.getUpdateBy())
                        .updateDate(view.getUpdateDate())
                        .build();
                viewList.add(logicalView);
            }
            return viewList;
        }
    }
    
    /**
     * Storage-related utilities for working with physical storage
     */
    @Slf4j
    @UtilityClass
    public static class StorageUtils {

        /**
         * Extracts storage details from a PhysicalStorage object.
         */
        public static DataStoreTableDetail getStorageDetail(ObjectMapper objectMapper, PhysicalStorage storage) {
            if (storage == null || storage.getStorageDetails() == null) {
                return null;
            }

            AccessType accessType = storage.getAccessType();
            Map<String, String> storageDetails = TextUtils.readJson(objectMapper, storage.getStorageDetails(), new TypeReference<Map<String, String>>() {
            });
            if (accessType == AccessType.HIVE || accessType == AccessType.ICEBERG) {
                String originalClusterName = storageDetails.get("clusterName");
                String clusterName = originalClusterName != null ? originalClusterName : "APOLLO_RNO";

                if ("hubblelvs".equals(clusterName)) {
                    clusterName = "HUBBLE";
                }

                String dbName = storageDetails.get("databaseName") != null ? storageDetails.get("databaseName") : "GDS_T";

                String tableName = storageDetails.get("tableName");
                if (tableName == null) {
                    tableName = storageDetails.get("table_name");
                }

                return DataStoreTableDetail.builder()
                                        .dataStoreName(clusterName + "." + dbName)
                                        .dataStoreType("Hive")
                                        .dataTableName(tableName)
                                        .build();
            } else if (accessType == AccessType.STREAM) {
                return DataStoreTableDetail.builder()
                        .dataStoreName(storageDetails.get("streamName"))
                        .dataStoreType("Kafka")
                        .dataTableName(storageDetails.get("topicName"))
                        .build();
            }
            return null;
        }
    }
    
    /**
     * JSON and Map utilities for working with serialized data
     */
    @Slf4j
    @UtilityClass
    public static class JsonUtils {
        /**
         * Safely converts an object to a Map.
         */
        @SuppressWarnings(UNCHECKED)
        public static Map<String, Object> toMap(Object obj) {
            if (!(obj instanceof Map)) {
                return Map.of();
            }
            return (Map<String, Object>) obj;
        }

        /**
         * Safely gets a value from a map using a key.
         */
        public static Object getFromObjectMap(Map<String, Object> map, String key) {
            if (map == null || map.isEmpty() || key == null) {
                return null;
            }
            return map.get(key);
        }
    }
    
    // Public delegating methods to maintain backwards compatibility
    
    public static Set<LdmField> getSortedFieldsByOrdinal(Set<LdmField> fields) {
        return FieldUtils.getSortedFieldsByOrdinal(fields);
    }
    
    public static void collectDownstreamLdm(Set<Long> ids, Set<Long> downstreamLdmIdSet, LdmEntityRepository repository) {
        LdmUtils.collectDownstreamLdm(ids, downstreamLdmIdSet, repository);
    }
    
    public static ConsumableDataset mapToConsumableDataset(ObjectMapper objectMapper, Dataset dataset,
                                                       Namespace namespace, Long ldmId, String ldmName,
                                                       AccessType accessType, DataStoreTableDetail storeTableDetail, String pipelineId) {
        return MapperUtils.mapToConsumableDataset(objectMapper, dataset, namespace, ldmId, ldmName, accessType, storeTableDetail, pipelineId);
    }
    
    public static LogicalDataModel mapToLogicalDataModel(LdmBaseEntity ldm, Namespace namespace) {
        return MapperUtils.mapToLogicalDataModel(ldm, namespace);
    }
    
    public static DataStoreTableDetail getStorageDetail(ObjectMapper objectMapper, PhysicalStorage storage) {
        return StorageUtils.getStorageDetail(objectMapper, storage);
    }
    
    public static String getLdmName(String entityName, ViewType viewType) {
        return LdmUtils.getLdmName(entityName, viewType);
    }
    
    public static LdmEntity copyLdm(LdmEntity source, EntityManager entityManager, String updateBy) {
        return LdmUtils.copyLdm(source, entityManager, updateBy);
    }
    
    public static LdmEntity copyBasicInfoFromBaseEntity(LdmEntity ldmEntity, LdmBaseEntity baseEntity) {
        return LdmUtils.copyBasicInfoFromBaseEntity(ldmEntity, baseEntity);
    }

    public static void excludeTextFields(LdmEntity entity) {
        entity.setIr(null);
        entity.setGeneratedSql(null);
        entity.setCodeContent(null);
    }
}
