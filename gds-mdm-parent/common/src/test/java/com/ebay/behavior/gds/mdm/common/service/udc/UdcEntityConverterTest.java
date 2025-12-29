package com.ebay.behavior.gds.mdm.common.service.udc;

import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;
import com.ebay.behavior.gds.mdm.common.testUtil.TestMetadata;
import com.ebay.datagov.pushingestion.EntityRelationshipTarget;
import com.ebay.datagov.pushingestion.TargetType;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class UdcEntityConverterTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private UdcEntityConverter converter;

    private final Metadata metadata = new TestMetadata(1L);
    private final UdcDataSourceType dataSource = UdcDataSourceType.TEST;

    @Test
    void toDeleteEntity() {
        var entity = converter.toDeleteEntity(metadata, dataSource);

        assertThat(entity).isNotNull();
        assertThat(entity.isDeleted()).isTrue();
        assertThat(entity.getRelationships()).isEmpty();
        assertThat(entity.getSource()).isEqualTo(dataSource.getValue());
        assertThat(entity.getEntityType()).isEqualTo(metadata.getEntityType().getValue());

        var properties = entity.getProperties();
        assertThat(properties).hasSize(1);
        assertThat(properties.get(metadata.getEntityType().getIdName())).isEqualTo(metadata.getId());
    }

    @Test
    void toEntity() {
        var entity = converter.toEntity(metadata, dataSource);

        assertThat(entity).isNotNull();
        assertThat(entity.isDeleted()).isFalse();
        assertThat(entity.getRelationships()).isEmpty();
        assertThat(entity.getSource()).isEqualTo(dataSource.getValue());
        assertThat(entity.getEntityType()).isEqualTo(metadata.getEntityType().getValue());

        var properties = entity.getProperties();
        assertThat(properties).hasSize(2);
        assertThat(properties.get("id")).isEqualTo(metadata.getId());
        assertThat(properties.get("entityType")).isEqualTo(metadata.getEntityType().getValue());
    }

    @Test
    void toEntity_withRelations() {
        var relations = mockRelations();

        var entity = converter.toEntity(metadata, relations, dataSource);

        assertThat(entity).isNotNull();
        assertThat(entity.isDeleted()).isFalse();
        assertThat(entity.getRelationships()).isNotEmpty();
        assertThat(entity.getSource()).isEqualTo(dataSource.getValue());
        assertThat(entity.getEntityType()).isEqualTo(metadata.getEntityType().getValue());

        var properties = entity.getProperties();
        assertThat(properties).hasSize(2);
        assertThat(properties.get("id")).isEqualTo(metadata.getId());
        assertThat(properties.get("entityType")).isEqualTo(metadata.getEntityType().getValue());
    }

    @Test
    void toRelationList() {
        var parentType = UdcEntityType.EVENT;
        var parentId = 1L;

        var relations = converter.toRelationList(parentType, parentId);

        assertThat(relations).hasSize(1);
        assertThat(relations.get(0).getEntityType()).isEqualTo(parentType.getValue());
        assertThat(relations.get(0).getTargetType()).isEqualTo(TargetType.PROPERTIES);
        assertThat(relations.get(0).getProperties()).containsEntry(parentType.getIdName(), parentId);

    }

    private Map<String, List<EntityRelationshipTarget>> mockRelations() {
        return Map.of("relationKey", List.of(mock(EntityRelationshipTarget.class)));
    }
}