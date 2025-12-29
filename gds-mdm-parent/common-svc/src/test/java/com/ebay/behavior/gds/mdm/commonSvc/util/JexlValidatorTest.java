package com.ebay.behavior.gds.mdm.commonSvc.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class JexlValidatorTest {

    @Test
    void isValidExpression() {
        Assertions.assertDoesNotThrow(() -> JexlValidator.isValidExpression("1 + 1"));
    }

    @Test
    void isValidExpression_invalidExpression() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> JexlValidator.isValidExpression("1 + "));
    }

    @Test
    void isValidExpression_nullExpression() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> JexlValidator.isValidExpression(null));
    }

    @Test
    void isValidExpression_emptyExpression() {
        Assertions.assertDoesNotThrow(() -> JexlValidator.isValidExpression(""));
    }
}