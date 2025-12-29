package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.util.TimeUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.model.Model.REVISION;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestModelUtils.unstagedAttribute;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestModelUtils.unstagedEvent;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestUtils.getRandomSmallString;
import static org.assertj.core.api.Assertions.assertThat;

class UnstagedEventTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void toMetadataMap() {
        var eventId = 1L;
        var attributeId = 2L;
        var now = TimeUtils.toNowSqlTimestamp();

        var attr = unstagedAttribute(eventId);
        attr.setId(attributeId);

        var event = unstagedEvent();
        event.setId(eventId);
        event.setRevision(0);
        event.setFsmOrder(1);
        event.setCardinality(1);
        event.setAttributes(Set.of(attr));
        event.setPageIds(Set.of(1L));
        event.setModuleIds(Set.of(2L));
        event.setClickIds(Set.of(3L));

        event.setCreateDate(now);
        event.setUpdateDate(now);
        event.setCreateBy(getRandomSmallString());
        event.setUpdateBy(getRandomSmallString());

        var map = event.toMetadataMap(objectMapper);

        assertThat(map).containsEntry(ID, eventId);
        assertThat(map).containsEntry(REVISION, event.getRevision());
        assertThat(map).containsEntry(event.getEntityType().getIdName(), eventId);
        assertThat(map).containsEntry(NAME, event.getName());
        assertThat(map).containsEntry("description", event.getDescription());
        assertThat(map).containsEntry("type", event.getType());
        assertThat(map).containsEntry("source", event.getSource().name());
        assertThat(map).containsEntry("fsmOrder", event.getFsmOrder());
        assertThat(map).containsEntry("cardinality", event.getCardinality());
        assertThat(map).containsEntry("githubRepositoryUrl", event.getGithubRepositoryUrl());
        assertThat(map).containsEntry("surfaceType", event.getSurfaceType().name());
        assertThat(map).containsEntry("expression", event.getExpression());
        assertThat(map).containsEntry("expressionType", event.getExpressionType().name());
        assertThat(map).containsEntry("createBy", event.getCreateBy());
        assertThat(map).containsEntry("updateBy", event.getUpdateBy());
        assertThat(map).containsEntry("createDate", event.getCreateDate().getTime());
        assertThat(map).containsEntry("updateDate", event.getUpdateDate().getTime());

        assertThat(map.get("attributes")).isInstanceOf(List.class);
        val attributes = (List<Map<String, Object>>) map.get("attributes");
        assertThat(attributes).hasSize(1);
        val attribute = attributes.get(0);
        assertThat(attribute.get(ID).toString()).isEqualTo(String.valueOf(attributeId));

        assertThat(map.get("pageIds")).isInstanceOf(Set.class);
        val pageIds = (Set<Long>) map.get("pageIds");
        assertThat(pageIds).containsExactly(1L);

        assertThat(map.get("moduleIds")).isInstanceOf(Set.class);
        val moduleIds = (Set<Long>) map.get("moduleIds");
        assertThat(moduleIds).containsExactly(2L);

        assertThat(map.get("clickIds")).isInstanceOf(Set.class);
        val clickIds = (Set<Long>) map.get("clickIds");
        assertThat(clickIds).containsExactly(3L);
    }
}