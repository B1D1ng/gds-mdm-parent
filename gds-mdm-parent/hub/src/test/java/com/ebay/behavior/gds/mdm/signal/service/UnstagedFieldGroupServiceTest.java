package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.RevisionedId;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.signal.common.model.FieldGroup;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class UnstagedFieldGroupServiceTest {

    @Mock
    private UnstagedSignalService signalService;

    @Mock
    private UnstagedFieldService fieldService;

    @Spy
    @InjectMocks
    private UnstagedFieldGroupService service;

    @Test
    void getAll_withNoFields() {
        var signalId = VersionedId.of(1L, 1);
        Set<UnstagedField> fields = Set.of();
        doReturn(fields).when(signalService).getFields(signalId);

        var fieldGroup = service.getAll(signalId);

        assertThat(fieldGroup).isEmpty();
    }

    @Test
    void getAll_withUniqueFields() {
        var tag1 = "tag1";
        var tag2 = "tag2";
        var signalId = VersionedId.of(1L, 1);
        var field1 = unstagedField(signalId).setTag(tag1).setEventTypes("type1");
        field1.setId(1L);
        field1.setRevision(1);
        var field2 = unstagedField(signalId).setTag(tag2).setEventTypes("type2");
        field2.setId(2L);
        field2.setRevision(2);
        var fields = Set.of(field1, field2);
        doReturn(fields).when(signalService).getFields(signalId);

        var fieldGroup = new ArrayList<>(service.getAll(signalId));
        fieldGroup.sort(Comparator.comparing(FieldGroup::getTag));

        assertThat(fieldGroup).hasSize(2);
        assertThat(fieldGroup).extracting(FieldGroup::getTag).containsExactlyInAnyOrder(tag1, tag2);

        assertThat(fieldGroup.get(0).getRevisionedIds().stream().map(RevisionedId::getId))
                .hasSize(1).containsExactlyInAnyOrder(1L);
        assertThat(fieldGroup.get(0).getEventTypesAsList()).hasSize(1).containsOnly("type1");
        assertThat(fieldGroup.get(1).getRevisionedIds().stream().map(RevisionedId::getId))
                .hasSize(1).containsExactlyInAnyOrder(2L);
        assertThat(fieldGroup.get(1).getEventTypesAsList()).hasSize(1).containsOnly("type2");
    }

    @Test
    void getAll_withDuplicateTagsNames() {
        var tag = "tag";
        var name = "name";
        var signalId = VersionedId.of(1L, 1);
        var field1 = unstagedField(signalId).setTag(tag).setName(name).setEventTypes("type1");
        field1.setId(1L);
        field1.setRevision(1);
        var field2 = unstagedField(signalId).setTag(tag).setName(name).setEventTypes("type2");
        field2.setId(2L);
        field2.setRevision(2);
        var fields = Set.of(field1, field2);
        doReturn(fields).when(signalService).getFields(signalId);

        var fieldGroups = new ArrayList<>(service.getAll(signalId));
        fieldGroups.sort(Comparator.comparing(FieldGroup::getTag));

        assertThat(fieldGroups).hasSize(1);
        assertThat(fieldGroups).extracting(FieldGroup::getTag, FieldGroup::getName).containsOnly(tuple(tag, name));

        var fieldGroup = fieldGroups.get(0);
        assertThat(fieldGroup.getRevisionedIds().stream().map(RevisionedId::getId))
                .hasSize(2).containsExactlyInAnyOrder(1L, 2L);
        assertThat(fieldGroup.getEventTypesAsList()).hasSize(2).containsExactlyInAnyOrder("type1", "type2");
    }

    @Test
    void getByTag_withNoFields_error() {
        var signalId = VersionedId.of(1L, 1);
        Set<UnstagedField> fields = Set.of();
        doReturn(fields).when(signalService).getFields(signalId);

        assertThatThrownBy(() -> service.getByTag(signalId, "tag"))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void getByKey_withNoFields_error() {
        var signalId = VersionedId.of(1L, 1);
        Set<UnstagedField> fields = Set.of();
        doReturn(fields).when(signalService).getFields(signalId);

        assertThatThrownBy(() -> service.getByKey(signalId, "key"))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void update_withoutIds_error() {
        val field = new FieldGroup<UnstagedField>();

        assertThatThrownBy(() -> service.update(field))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be empty");
    }

    @Test
    void update_fieldsNotFound_error() {
        val emptyList = List.of();
        doReturn(emptyList).when(fieldService).findAllById(any());
        val field = new FieldGroup<UnstagedField>();
        field.setRevisionedIds(Set.of(RevisionedId.of(1L, 1)));

        assertThatThrownBy(() -> service.update(field))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void deleteByTag_mandatoryField_error() {
        var signalId = VersionedId.of(1L, 1);
        var field = unstagedField(signalId).setTag("tag").setIsMandatory(true);
        field.setId(1L);
        field.setRevision(1);
        var fields = Set.of(field);
        doReturn(fields).when(signalService).getFields(signalId);

        assertThatThrownBy(() -> service.deleteByTag(signalId, "tag"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot delete a mandatory field group");
    }

    @Test
    void deleteByKey_mandatoryField_error() {
        var signalId = VersionedId.of(1L, 1);
        var field = unstagedField(signalId).setTag("tag").setName("name").setIsMandatory(true);
        field.setId(1L);
        field.setRevision(1);
        var fields = Set.of(field);
        doReturn(fields).when(signalService).getFields(signalId);

        assertThatThrownBy(() -> service.deleteByKey(signalId, field.getGroupKey()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot delete a mandatory field group");
    }
}
