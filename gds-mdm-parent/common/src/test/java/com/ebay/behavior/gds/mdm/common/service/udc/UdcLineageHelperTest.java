package com.ebay.behavior.gds.mdm.common.service.udc;

import com.ebay.behavior.gds.mdm.common.model.LineageParameters;
import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.testUtil.TestMetadata;
import com.ebay.datagov.pushingestion.EntityRelationshipTarget;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.COLUMN_TRANSFORMATION;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.TRANSFORMATION;
import static com.ebay.behavior.gds.mdm.common.service.udc.UdcLineageHelper.TRANSFORMATION_INPUT_COLUMN;
import static com.ebay.behavior.gds.mdm.common.service.udc.UdcLineageHelper.TRANSFORMATION_INPUT_TABLE;
import static com.ebay.behavior.gds.mdm.common.service.udc.UdcLineageHelper.TRANSFORMATION_OUTPUT_COLUMN;
import static com.ebay.behavior.gds.mdm.common.service.udc.UdcLineageHelper.TRANSFORMATION_OUTPUT_TABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class UdcLineageHelperTest {

    @Mock
    private ObjectMapper objectMapper;

    @Spy
    @InjectMocks
    private UdcEntityConverter converter;

    @Mock
    private EntityRelationshipTarget target;

    @InjectMocks
    private UdcLineageHelper lineageHelper;

    private final Metadata metadata = new TestMetadata(1L);

    private final UdcDataSourceType dataSource = UdcDataSourceType.TEST;

    private final LineageParameters transformationParams = new LineageParameters(
            TRANSFORMATION, metadata, "inputTableRelationType", metadata, Set.of(2L), "outputTableRelationType");

    private final LineageParameters columnTransformationParams = new LineageParameters(
            COLUMN_TRANSFORMATION, metadata, "inputColumnRelationType", metadata, Set.of(2L), "outputColumnRelationType");

    @BeforeEach
    void setUp() {
        setField(lineageHelper, "entityConverter", converter);
    }

    @Test
    void toLineageEntity_transformation() {
        doReturn(target).when(converter).toRelation(eq(transformationParams.getOutputEntityType()), eq(transformationParams.getOutputEntityIdName()), anyLong());

        var entity = lineageHelper.toLineageEntity(transformationParams, dataSource);

        assertThat(entity).isNotNull();
        assertThat(entity.getEntityType()).isEqualTo(TRANSFORMATION.getValue());
        assertThat(entity.getSource()).isEqualTo(dataSource.getValue());
        assertThat(entity.isDeleted()).isFalse();

        assertThat(entity.getProperties()).hasSize(2);
        assertThat(entity.getProperties().keySet()).containsOnly(TRANSFORMATION_INPUT_TABLE, TRANSFORMATION_OUTPUT_TABLE);

        assertThat(entity.getRelationships()).hasSize(2);
        assertThat(entity.getRelationships().keySet()).containsOnly("inputTableRelationType", "outputTableRelationType");
    }

    @Test
    void toLineageEntity_columnTransformation() {
        doReturn(target).when(converter).toRelation(eq(columnTransformationParams.getOutputEntityType()), eq(columnTransformationParams.getOutputEntityIdName()), anyLong());

        var entity = lineageHelper.toLineageEntity(columnTransformationParams, dataSource);

        assertThat(entity).isNotNull();
        assertThat(entity.getEntityType()).isEqualTo(COLUMN_TRANSFORMATION.getValue());
        assertThat(entity.getSource()).isEqualTo(dataSource.getValue());
        assertThat(entity.isDeleted()).isFalse();

        assertThat(entity.getProperties()).hasSize(2);
        assertThat(entity.getProperties().keySet()).containsOnly(TRANSFORMATION_INPUT_COLUMN, TRANSFORMATION_OUTPUT_COLUMN);

        assertThat(entity.getRelationships()).hasSize(2);
        assertThat(entity.getRelationships().keySet()).containsOnly("inputColumnRelationType", "outputColumnRelationType");
    }
}
