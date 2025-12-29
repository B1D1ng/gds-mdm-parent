package com.ebay.behavior.gds.mdm.common.util;

import com.ebay.behavior.gds.mdm.common.testUtil.TestModel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.ebay.behavior.gds.mdm.common.util.CommonValidationUtils.validateForCreate;
import static com.ebay.behavior.gds.mdm.common.util.CommonValidationUtils.validateForUpdate;
import static com.ebay.behavior.gds.mdm.common.util.CommonValidationUtils.validateNumeric;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class CommonValidationUtilsTest {

    @Test
    void validateForCreate_auditable() {
        var plan = new TestModel().withId(null).withRevision(null);

        assertThatThrownBy(() -> validateForCreate(plan.withId(1L))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validateForCreate(plan.withRevision(0))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateForUpdate_auditable() {
        var plan = new TestModel().withId(123L);

        assertThatThrownBy(() -> validateForUpdate(plan.withId(null))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validateForUpdate(plan.withRevision(null))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateForUpdate_model() {
        var plan = new TestModel().withId(123L);

        assertThatThrownBy(() -> validateForUpdate(plan.withId(null))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validateForUpdate(plan.withRevision(null))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateNumeric_validNumber() {
        assertDoesNotThrow(() -> validateNumeric("12345"));
    }

    @Test
    void validateNumeric_invalidNumber() {
        assertThatThrownBy(() -> validateNumeric("abc123")).isInstanceOf(IllegalArgumentException.class);
    }
}