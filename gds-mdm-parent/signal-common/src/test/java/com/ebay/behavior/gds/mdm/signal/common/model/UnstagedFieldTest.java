package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.util.TimeUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.model.Model.REVISION;
import static com.ebay.behavior.gds.mdm.common.model.VersionedId.MIN_VERSION;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.IS_MANDATORY;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestModelUtils.unstagedAttribute;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestModelUtils.unstagedField;
import static org.assertj.core.api.Assertions.assertThat;

class UnstagedFieldTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void toMetadataMap() {
        var signalId = VersionedId.of(1L, MIN_VERSION);
        var fieldId = 2L;
        var attributeId = 3L;
        var now = TimeUtils.toNowSqlTimestamp();

        val attribute = unstagedAttribute(fieldId);
        attribute.setId(attributeId);

        var field = unstagedField(signalId);
        field.setId(fieldId);
        field.setRevision(0);
        field.setSignalId(3L);
        field.setAttributes(Set.of(attribute));
        field.setUpdateDate(now);
        field.setCreateDate(now);

        var map = field.toMetadataMap(objectMapper);

        assertThat(map).containsEntry(ID, fieldId);
        assertThat(map).containsEntry(REVISION, field.getRevision());
        assertThat(map).containsEntry(field.getEntityType().getIdName(), fieldId);
        assertThat(map).containsEntry("signalId", field.getSignalId());
        assertThat(map).containsEntry("signalVersion", field.getSignalVersion());
        assertThat(map).containsEntry(NAME, field.getName());
        assertThat(map).containsEntry("description", field.getDescription());
        assertThat(map).containsEntry("tag", field.getTag());
        assertThat(map).containsEntry("javaType", field.getJavaType().getValue());
        assertThat(map).containsEntry("expression", field.getExpression());
        assertThat(map).containsEntry("expressionType", field.getExpressionType().name());
        assertThat(map).containsEntry(IS_MANDATORY, field.getIsMandatory());

        assertThat(map.get("attributes")).isInstanceOf(List.class);
        val attributes = (List<Map<String, Object>>) map.get("attributes");
        assertThat(attributes).hasSize(1);
        val attributeEntity = attributes.get(0);
        assertThat(attributeEntity.get(ID).toString()).isEqualTo(String.valueOf(attributeId));
    }
}