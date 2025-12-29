package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.util.TimeUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.model.Model.REVISION;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestModelUtils.unstagedAttribute;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestUtils.getRandomSmallString;
import static org.assertj.core.api.Assertions.assertThat;

class UnstagedAttributeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void toMetadataMap() {
        var id = 1L;
        var now = TimeUtils.toNowSqlTimestamp();
        var attribute = unstagedAttribute(2L);
        attribute.setId(id);
        attribute.setRevision(0);
        attribute.setIsStoreInState(true);
        attribute.setCreateDate(now);
        attribute.setUpdateDate(now);
        attribute.setCreateBy(getRandomSmallString());
        attribute.setUpdateBy(getRandomSmallString());

        var map = attribute.toMetadataMap(objectMapper);

        assertThat(map).containsEntry(ID, id);
        assertThat(map).containsEntry(REVISION, attribute.getRevision());
        assertThat(map).containsEntry(attribute.getEntityType().getIdName(), id);
        assertThat(map).containsEntry("eventId", attribute.getEventId());
        assertThat(map).containsEntry("description", attribute.getDescription());
        assertThat(map).containsEntry("javaType", attribute.getJavaType().getValue());
        assertThat(map).containsEntry("schemaPath", attribute.getSchemaPath());
        assertThat(map).containsEntry("isStoreInState", attribute.getIsStoreInState());
        assertThat(map).containsEntry("createBy", attribute.getCreateBy());
        assertThat(map).containsEntry("updateBy", attribute.getUpdateBy());
        assertThat(map).containsEntry("createDate", attribute.getCreateDate().getTime());
        assertThat(map).containsEntry("updateDate", attribute.getUpdateDate().getTime());

        assertThat(map).doesNotContainKey("event");
    }
}