package com.ebay.behavior.gds.mdm.common.service.udc;

import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;
import com.ebay.com.google.common.annotations.VisibleForTesting;
import com.ebay.datagov.pushingestion.EntityRelationshipTarget;
import com.ebay.datagov.pushingestion.EntityVersionData;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

import static com.ebay.datagov.pushingestion.EntityRelationshipTarget.createByKeyProperties;

@Component
@Validated
public class UdcEntityConverter {

    @Autowired
    private ObjectMapper objectMapper;

    public EntityVersionData toDeleteEntity(@NotNull Metadata metadata, @NotNull UdcDataSourceType dataSource) {
        val entity = toEntity(metadata.getEntityType(), dataSource, metadata.getIdMap(), Map.of());
        entity.setDeleted(true);
        return entity;
    }

    @VisibleForTesting
    public EntityVersionData toEntity(@NotNull Metadata metadata, @NotNull UdcDataSourceType dataSource) {
        return toEntity(metadata.getEntityType(), dataSource, metadata.toMetadataMap(objectMapper), Map.of());
    }

    public EntityVersionData toEntity(@NotNull Metadata metadata, @NotNull Map<String, List<EntityRelationshipTarget>> relations,
                                      @NotNull UdcDataSourceType dataSource) {
        return new EntityVersionData(metadata.getEntityType().getValue(), dataSource.getValue(), metadata.toMetadataMap(objectMapper), relations, false);
    }

    public EntityVersionData toEntity(@NotNull UdcEntityType entityType, @NotNull UdcDataSourceType dataSource,
                                      @NotNull Map<String, Object> properties, @NotNull Map<String, List<EntityRelationshipTarget>> relations) {
        return new EntityVersionData(entityType.getValue(), dataSource.getValue(), properties, relations, false);
    }

    public List<EntityRelationshipTarget> toRelationList(@NotNull UdcEntityType parentType, @PositiveOrZero long parentId) {
        return toRelationList(parentType, parentType.getIdName(), parentId);
    }

    public List<EntityRelationshipTarget> toRelationList(@NotNull UdcEntityType parentType, @NotBlank String idName, @PositiveOrZero long parentId) {
        return List.of(createByKeyProperties(parentType.getValue(), Map.of(idName, parentId)));
    }

    public EntityRelationshipTarget toRelation(@NotNull UdcEntityType parentType, @NotBlank String parentIdName, @PositiveOrZero long parentId) {
        return createByKeyProperties(parentType.getValue(), Map.of(parentIdName, parentId));
    }

    public Map<String, List<EntityRelationshipTarget>> toRelationMap(@NotBlank String relationType, @NotNull List<EntityRelationshipTarget> relation) {
        return Map.of(relationType, relation);
    }
}