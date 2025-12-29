package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.CompletionStatus;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.VersionedModel;
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
import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestModelUtils.unstagedAttribute;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestModelUtils.unstagedEvent;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestModelUtils.unstagedField;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestModelUtils.unstagedSignal;
import static org.assertj.core.api.Assertions.assertThat;

class UnstagedSignalTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void toMetadataMap() {
        var signalId = VersionedId.of(1L, MIN_VERSION);
        var eventId = 2L;
        var fieldId = 3L;
        var attributeId = 4L;
        var now = TimeUtils.toNowSqlTimestamp();

        val attribute = unstagedAttribute(eventId);
        attribute.setId(attributeId);

        var event = unstagedEvent();
        event.setId(eventId);
        event.setRevision(0);
        event.setAttributes(Set.of(attribute));
        event.setClickIds(Set.of(1L));
        event.setModuleIds(Set.of(2L));
        event.setPageIds(Set.of(3L));

        val field = unstagedField(signalId);
        field.setId(fieldId);
        field.setAttributes(Set.of(attribute));

        var signal = unstagedSignal(1L);
        signal.setSignalId(signalId);
        signal.setRevision(0);
        signal.setSignalTemplateSourceId(2L);
        signal.setSignalSourceId(3L);
        signal.setRetentionPeriod(30L);
        signal.setCompletionStatus(CompletionStatus.COMPLETED);
        signal.setOwners("owners");
        signal.setFields(Set.of(field));
        signal.setEvents(Set.of(event));

        signal.setCreateDate(now);
        signal.setUpdateDate(now);
        var map = signal.toMetadataMap(objectMapper);

        assertThat(map).containsEntry(ID, signalId.getId());
        assertThat(map).containsEntry(VersionedModel.VERSION, signalId.getVersion());
        assertThat(map).containsEntry(signal.getEntityType().getIdName(), signalId.getId());
        assertThat(map).containsEntry("planId", signal.getPlanId());
        assertThat(map).containsEntry("signalTemplateSourceId", signal.getSignalTemplateSourceId());
        assertThat(map).containsEntry("signalSourceId", signal.getSignalSourceId());
        assertThat(map).containsEntry(NAME, signal.getName());
        assertThat(map).containsEntry(REVISION, signal.getRevision());
        assertThat(map).containsEntry("description", signal.getDescription());
        assertThat(map).containsEntry("domain", signal.getDomain());
        assertThat(map).containsEntry("owners", signal.getOwners());
        assertThat(map).containsEntry("type", signal.getType());
        assertThat(map).containsEntry("retentionPeriod", signal.getRetentionPeriod());
        assertThat(map).containsEntry("completionStatus", signal.getCompletionStatus().name());
        assertThat(map).containsEntry("environment", signal.getEnvironment().name());
        assertThat(map).containsEntry("createDate", signal.getCreateDate().getTime());
        assertThat(map).containsEntry("updateDate", signal.getUpdateDate().getTime());

        assertThat(map.get("fields")).isInstanceOf(List.class);
        val fields = (List<Map<String, Object>>) map.get("fields");
        assertThat(fields).hasSize(1);
        val fieldEntity = fields.get(0);
        assertThat(fieldEntity.get(ID).toString()).isEqualTo(String.valueOf(fieldId));

        assertThat(map.get("events")).isInstanceOf(List.class);
        val events = (List<Map<String, Object>>) map.get("events");
        assertThat(events).hasSize(1);
        val eventEntity = events.get(0);
        assertThat(eventEntity.get(ID).toString()).isEqualTo(String.valueOf(eventId));
    }
}