package com.ebay.behavior.gds.mdm.dec.model.dto;

import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmField;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LdmEntityRequestTest {

    @Test
    void toLdmEntity_fieldsNull() {
        LdmEntityRequest request = LdmEntityRequest.builder().fields(null).build();
        LdmEntity entity = request.toLdmEntity();
        assertThat(entity).isNotNull();
        assertThat(entity.getFields()).isNull();
    }

    @Test
    void toLdmEntity_fieldsNotNull() {
        LdmField field1 = LdmField.builder().name("field1").build();
        LdmField field2 = LdmField.builder().name("field2").build();
        List<LdmField> fields = Arrays.asList(field1, field2);
        LdmEntityRequest request = LdmEntityRequest.builder().fields(fields).build();
        LdmEntity entity = request.toLdmEntity();
        assertThat(entity).isNotNull();
        assertThat(entity.getFields()).hasSize(2);
        // Assert ordinals based on field name
        for (LdmField field : entity.getFields()) {
            if ("field1".equals(field.getName())) {
                assertThat(field.getOrdinal()).isEqualTo(1);
            } else if ("field2".equals(field.getName())) {
                assertThat(field.getOrdinal()).isEqualTo(2);
            }
        }
    }
}
