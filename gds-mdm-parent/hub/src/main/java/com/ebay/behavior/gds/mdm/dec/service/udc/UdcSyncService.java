package com.ebay.behavior.gds.mdm.dec.service.udc;

import com.ebay.behavior.gds.mdm.common.exception.UdcException;
import com.ebay.behavior.gds.mdm.common.model.IdWithStatus;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;
import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmField;
import com.ebay.behavior.gds.mdm.dec.model.LdmFieldSignalMapping;
import com.ebay.behavior.gds.mdm.dec.model.enums.NamespaceType;
import com.ebay.behavior.gds.mdm.dec.service.DatasetService;
import com.ebay.behavior.gds.mdm.dec.service.LdmBaseEntityService;
import com.ebay.behavior.gds.mdm.dec.service.LdmEntityService;
import com.ebay.behavior.gds.mdm.dec.service.NamespaceService;
import com.ebay.behavior.gds.mdm.dec.service.PhysicalStorageService;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.DATASET;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.LDM;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.TRANSFORMATION;

@Slf4j
@Service
@Validated
public class UdcSyncService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LdmEntityService entityService;

    @Autowired
    private LdmBaseEntityService baseEntityService;

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private PhysicalStorageService physicalStorageService;

    @Autowired
    private MetadataWriteService udcWriteService;

    @Autowired
    private NamespaceService namespaceService;

    @Transactional(readOnly = true)
    public IdWithStatus syncLdm(@PositiveOrZero long baseEntityId) {
        val baseEntity = baseEntityService.getByIdWithAssociations(baseEntityId);
        val namespaceId = baseEntity.getNamespaceId();
        val namespace = namespaceService.getById(namespaceId);

        // get upstream ldms in last version
        Set<Long> upstreamIds = Set.of();
        Set<Long> lastUpstreamIds = Set.of();
        if (namespace.getType() != NamespaceType.BASE) {
            val view = baseEntity.getViews().get(0);
            upstreamIds = getUpstreamIds(view);

            if (view.getVersion() > 1) {
                val lastVersionView = entityService.getById(VersionedId.of(view.getId(), view.getVersion() - 1));
                lastUpstreamIds = getUpstreamIds(lastVersionView);
            }
        }

        // for different env will use different push endpoint
        String udcEntityId;
        try {
            udcEntityId = udcWriteService.upsertLogicalDataModel(baseEntity, namespace, upstreamIds, lastUpstreamIds);
            log.info(String.format("LDM with id %s successfully injected to UDC Portal", baseEntityId));
            return IdWithStatus.okStatus(baseEntityId, udcEntityId);
        } catch (UdcException ex) {
            val error = ex.getMessage();
            log.error(String.format("Failed to inject LDM (id: %d). Error: %s", baseEntityId, error));
            return IdWithStatus.failedStatus(baseEntityId, error);
        }
    }

    private Set<Long> getUpstreamIds(LdmEntity view) {
        Set<Long> upstreamViewIds = view.getUpstreamLdm() != null
                ? Arrays.stream(view.getUpstreamLdm().split(",")).map(Long::parseLong).collect(Collectors.toSet()) : Set.of();
        return upstreamViewIds.stream().map(id -> entityService.getByIdCurrentVersion(id).getBaseEntityId()).collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public IdWithStatus syncDataset(@PositiveOrZero long datasetId) {
        // get dataset, namespace, ldmId, ldmName
        val dataset = datasetService.getByIdCurrentVersion(datasetId);
        val namespace = namespaceService.getById(dataset.getNamespaceId());
        val ldmView = entityService.getByIdCurrentVersion(dataset.getLdmEntityId());
        val ldmId = ldmView.getBaseEntityId();
        val ldm = baseEntityService.getById(ldmId);
        val ldmName = ldm.getName();

        // get pipeline id
        val storages = physicalStorageService.getAllByDatasetId(datasetId, false, null);
        val filteredStorages = storages.stream().filter(s -> s.getPipelines() != null).toList();
        val storage = filteredStorages.isEmpty() ? null : filteredStorages.get(0);

        Long lastLdmId = null;
        if (dataset.getVersion() > 1) {
            val lastDataset = datasetService.getById(VersionedId.of(dataset.getId(), dataset.getVersion() - 1));
            val lastLdmView = entityService.getByIdCurrentVersion(lastDataset.getLdmEntityId());
            lastLdmId = lastLdmView.getBaseEntityId();
        }

        // sync to udc
        String udcEntityId;
        try {
            val result = udcWriteService.upsertConsumableDataset(dataset, namespace, ldmId, ldmName, storage, lastLdmId);
            udcEntityId = result.get(DATASET);
            log.info(String.format("Dataset with id %s successfully injected to UDC Portal", datasetId));
            return IdWithStatus.okStatus(datasetId, udcEntityId);
        } catch (UdcException ex) {
            val error = ex.getMessage();
            log.error(String.format("Failed to inject LDM (id: %d). Error: %s", datasetId, error));
            return IdWithStatus.failedStatus(datasetId, error);
        }
    }

    @Transactional(readOnly = true)
    public IdWithStatus syncSignalLineage(@PositiveOrZero long baseEntityId, Long signalId) {
        if (signalId != null) {
            return syncSignalLineage(baseEntityId, Set.of(signalId));
        }
        LdmBaseEntity baseEntity = baseEntityService.getByIdWithAssociations(baseEntityId);
        List<LdmEntity> views = baseEntity.getViews();
        Set<Long> signalIdSet = new HashSet<>();
        for (LdmEntity view : views) {
            Set<LdmField> fields = view.getFields();
            if (fields == null) {
                continue;
            }
            for (LdmField field : fields) {
                if (field.getSignalMapping() != null) {
                    Set<Long> ids = field.getSignalMapping().stream()
                            .map(LdmFieldSignalMapping::getSignalDefinitionId)
                            .collect(Collectors.toSet());
                    signalIdSet.addAll(ids);
                }
            }
        }
        return syncSignalLineage(baseEntityId, signalIdSet);
    }

    private IdWithStatus syncSignalLineage(long baseEntityId, Set<Long> signalIdSet) {
        // sync to udc
        String udcEntityId;
        try {
            Map<UdcEntityType, String> result = udcWriteService.upsertSignalToLdmLineage(baseEntityId, signalIdSet);
            udcEntityId = result.get(TRANSFORMATION);
            log.info(String.format("Signal lineage of LDM with id %s successfully injected to UDC Portal", baseEntityId));
            return IdWithStatus.okStatus(baseEntityId, udcEntityId);
        } catch (UdcException ex) {
            val error = ex.getMessage();
            log.error(String.format("Failed to inject Signal lineage of LDM (id: %d). Error: %s", baseEntityId, error));
            return IdWithStatus.failedStatus(baseEntityId, error);
        }
    }

    protected Set<Long> convertToLong(Set<String> set) {
        if (set == null || set.isEmpty()) {
            return Set.of();
        }

        Set<Long> res = new HashSet<>();
        for (String s : set) {
            if (isValidLong(s)) {
                res.add(Long.parseLong(s));
            }
        }
        return res;
    }

    protected boolean isValidLong(String str) {
        return NumberUtils.isCreatable(str) && str.matches("-?\\d+");
    }

    public String delete(@Valid @NotNull UdcEntityType entityType, @NotBlank String id) {
        if (entityType != LDM && entityType != DATASET && entityType != TRANSFORMATION) {
            throw new IllegalArgumentException("Unsupported entity type for deletion: " + entityType);
        }

        String udcEntityId;
        try {
            udcEntityId = deleteByType(entityType, id);
            log.info(String.format("Entity with id %s successfully deleted in UDC Portal", id));
            return udcEntityId;
        } catch (UdcException ex) {
            val error = ex.getMessage();
            log.error(String.format("Failed to delete entity (id: %s). Error: %s", id, error));
            return id;
        }
    }

    private String deleteByType(UdcEntityType entityType, String id) {
        if (entityType == LDM) {
            return udcWriteService.deleteLogicalDataModel(Long.parseLong(id));
        } else if (entityType == DATASET) {
            return udcWriteService.deleteConsumableDataset(Long.parseLong(id));
        } else if (entityType == TRANSFORMATION) {
            return udcWriteService.deleteLineage(entityType, id);
        } else {
            throw new IllegalArgumentException("Unsupported entity type for deletion: " + entityType);
        }
    }
}