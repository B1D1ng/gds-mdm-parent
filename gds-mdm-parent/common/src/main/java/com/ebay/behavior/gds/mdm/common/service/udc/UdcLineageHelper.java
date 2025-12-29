package com.ebay.behavior.gds.mdm.common.service.udc;

import com.ebay.behavior.gds.mdm.common.model.LineageParameters;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.datagov.pushingestion.EntityVersionData;

import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.Map;
import java.util.stream.Collectors;

import static com.ebay.behavior.gds.mdm.common.model.Model.COMMA;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.COLUMN_TRANSFORMATION;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.TRANSFORMATION;

@Component
@Validated
public class UdcLineageHelper {

    public static final String TRANSFORMATION_INPUT_TABLE = "TransformationInputTable";
    public static final String TRANSFORMATION_OUTPUT_TABLE = "TransformationOutputTable";
    public static final String TRANSFORMATION_INPUT_COLUMN = "TransformationInputColumn";
    public static final String TRANSFORMATION_OUTPUT_COLUMN = "TransformationOutputColumn";

    @Autowired
    private UdcEntityConverter entityConverter;

    public EntityVersionData toLineageEntity(@NotNull LineageParameters params, @NotNull UdcDataSourceType dataSource) {
        final long inputId = params.getInputEntityId();
        final String csvOutputIds = params.getOutputEntityIds().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(COMMA));

        final var outputRelations = params.getOutputEntityIds().stream()
                .map(id -> entityConverter.toRelation(params.getOutputEntityType(), params.getOutputEntityIdName(), id))
                .toList();
        final var inputRelations = entityConverter.toRelationList(params.getInputEntityType(), params.getInputEntityIdName(), inputId);

        final Map<String, Object> properties = getLineageProperties(params, inputId, csvOutputIds);

        final var relationMap = Map.of(
                params.getInputRelationType(), inputRelations,
                params.getOutputRelationType(), outputRelations
        );

        return entityConverter.toEntity(params.getEntityType(), dataSource, properties, relationMap);
    }

    private Map<String, Object> getLineageProperties(LineageParameters params, long inputId, String csvOutputIds) {
        if (params.getEntityType() == TRANSFORMATION) {
            return Map.of(
                    TRANSFORMATION_INPUT_TABLE, String.valueOf(inputId),
                    TRANSFORMATION_OUTPUT_TABLE, csvOutputIds);
        } else if (params.getEntityType() == COLUMN_TRANSFORMATION) {
            return Map.of(
                    TRANSFORMATION_INPUT_COLUMN, String.valueOf(inputId),
                    TRANSFORMATION_OUTPUT_COLUMN, csvOutputIds);
        } else {
            throw new IllegalStateException("Unsupported lineage entity type: " + params.getEntityType());
        }
    }
}
