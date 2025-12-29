package com.ebay.behavior.gds.mdm.commonSvc.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;

@UtilityClass
public class JexlValidator {

    private static final JexlEngine jexlEngine = new JexlBuilder().create();

    public static void isValidExpression(String expression) {
        if (expression == null) {
            throw new IllegalArgumentException("JEXL expression cannot be null");
        }
        try {
            jexlEngine.createExpression(expression);
        } catch (JexlException e) {
            throw new IllegalArgumentException(String.format("Invalid JEXL expression: %s", expression), e);
        }
    }
}