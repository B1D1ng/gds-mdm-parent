package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.VersionedId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestModelUtils.unstagedField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FieldGroupTest {

    private UnstagedField field;
    private final VersionedId signalId = VersionedId.of(1L, 1);

    @BeforeEach
    void setUp() {
        field = unstagedField(signalId);
        field.setId(1L);
    }

    @Test
    void constructor_emptyFields_error() {
        assertThatThrownBy(() -> new FieldGroup<>(Collections.emptyList()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Fields must not be empty");
    }

    @Test
    void constructor_nullFieldId_throwsNullPointerException() {
        field.setId(null);
        var fields = List.of(field);

        assertThatThrownBy(() -> new FieldGroup<>(fields))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Field id must not be null");
    }

    @Test
    void constructor_blankFieldTag_error() {
        field.setTag(" ");
        var fields = List.of(field);

        assertThatThrownBy(() -> new FieldGroup<>(fields))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Field tag must not be blank");
    }

    @Test
    void constructor_blankEventTypes_error() {
        field.setEventTypes("");
        var fields = List.of(field);

        assertThatThrownBy(() -> new FieldGroup<>(fields))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Field eventTypes must not be blank");
    }

    @Test
    void constructor_mismatchedTag_error() {
        var field1 = unstagedField(signalId).toBuilder().tag("tag1").name("name").id(1L).build();
        var field2 = unstagedField(signalId).toBuilder().tag("tag2").name("name").id(1L).build();
        var fields = List.of(field1, field2);

        assertThatThrownBy(() -> new FieldGroup<>(fields))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Field tag must be the same");
    }

    @Test
    void constructor_mismatchedName_error() {
        var field1 = unstagedField(signalId).toBuilder().tag("tag").name("name1").id(1L).build();
        var field2 = unstagedField(signalId).toBuilder().tag("tag").name("name2").id(1L).build();
        var fields = List.of(field1, field2);

        assertThatThrownBy(() -> new FieldGroup<>(fields))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Field name must be the same");
    }

    @Test
    void constructor_mismatchedSignalIds_error() {
        var field1 = unstagedField(signalId)
                .toBuilder().tag("tag").name("name").id(1L).build();
        var field2 = unstagedField(VersionedId.of(signalId.getId() + 1, signalId.getVersion()))
                .toBuilder().tag("tag").name("name").id(2L).build();

        var fields = List.of(field1, field2);

        assertThatThrownBy(() -> new FieldGroup<>(fields))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Field signalId must be the same for all group fields");
    }

    @Test
    void constructor_mismatchedSignalVersions_error() {
        var field1 = unstagedField(signalId)
                .toBuilder().tag("tag").name("name").id(1L).build();
        field1.setTag("tag");
        field1.setId(1L);

        var field2 = unstagedField(VersionedId.of(signalId.getId(), signalId.getVersion() + 1))
                .toBuilder().tag("tag").name("name").id(2L).build();

        var fields = List.of(field1, field2);

        assertThatThrownBy(() -> new FieldGroup<>(fields))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Field signalVersion must be the same for all group fields");
    }

    @Test
    void getGroupKey() {
        var field1 = UnstagedField.builder().tag("tag").name("name").build();

        var key = field1.getGroupKey();

        assertThat(key).contains("tag", "name");
    }

    @Test
    void getGroupKey_nullTag() {
        var field1 = UnstagedField.builder().name("name").build();

        var key = field1.getGroupKey();

        assertThat(key).contains("null", "name");
    }

    @Test
    void getGroupKey_nullName() {
        var field1 = UnstagedField.builder().tag("tag").build();

        var key = field1.getGroupKey();

        assertThat(key).contains("null", "tag");
    }

    @Test
    void getGroupKey_nullTagAndName() {
        var field1 = new UnstagedField();

        var key = field1.getGroupKey();

        assertThat(key).contains("null");
    }
}