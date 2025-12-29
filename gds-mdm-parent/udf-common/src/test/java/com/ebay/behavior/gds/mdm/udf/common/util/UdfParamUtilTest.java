package com.ebay.behavior.gds.mdm.udf.common.util;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class UdfParamUtilTest {

    @Test
    public void testValidSchema() {
        String validSchema = "{\"type\": \"record\", \"name\": \"FunctionSignature\", \"fields\": [{\"name\": \"arguments\", \"type\": {\"type\": \"record\", \"name\": \"FunctionArguments\", \"fields\": [{\"name\": \"arg0\", \"type\": \"string\"}]}}, {\"name\": \"returnValue\", \"type\": \"long\"}]}";
        assertTrue(UdfParamUtil.isValid(validSchema));
    }

    @Test
    public void testValidSchemaEmptyArg() {
        String validSchema = "{\"type\": \"record\", \"name\": \"FunctionSignature\", \"fields\": [{\"name\": \"arguments\", \"type\": {\"type\": \"record\", \"name\": \"FunctionArguments\", \"fields\": []}}, {\"name\": \"returnValue\", \"type\": \"long\"}]}";
        assertTrue(UdfParamUtil.isValid(validSchema));
    }

    @Test
    public void testInvalidSchemaName() {
        String invalidSchemaName = "{\"type\": \"record\", \"name\": \"InvalidName\", \"fields\": [{\"name\": \"arguments\", \"type\": {\"type\": \"record\", \"name\": \"FunctionArguments\", \"fields\": [{\"name\": \"arg0\", \"type\": \"string\"}]}}, {\"name\": \"returnValue\", \"type\": \"long\"}]}";
        assertFalse(UdfParamUtil.isValid(invalidSchemaName));
    }

    @Test
    public void testInvalidFieldName() {
        String invalidFieldName = "{\"type\": \"record\", \"name\": \"FunctionSignature\", \"fields\": [{\"name\": \"wrongArguments\", \"type\": {\"type\": \"record\", \"name\": \"FunctionArguments\", \"fields\": [{\"name\": \"arg0\", \"type\": \"string\"}]}}, {\"name\": \"returnValue\", \"type\": \"long\"}]}";
        assertFalse(UdfParamUtil.isValid(invalidFieldName));
    }

    @Test
    public void testInvalidFieldType() {
        String invalidFieldType = "{\"type\": \"record\", \"name\": \"FunctionSignature\", \"fields\": [{\"name\": \"arguments\", \"type\": {\"type\": \"record\", \"name\": \"FunctionArguments\", \"fields\": [{\"name\": \"arg0\", \"type\": \"string\"}]}}, {\"name\": \"returnValue\", \"type\": \"wrongAvroType\"}]}";
        assertFalse(UdfParamUtil.isValid(invalidFieldType));
    }

    @Test
    public void test() {
        String param = "{\"type\":\"record\",\"name\":\"FunctionSignature\",\"fields\":[{\"name\":\"AnyType\",\"type\":{\"type\":\"record\",\"name\":\"AnyType\",\"fields\":[]}},{\"name\":\"arguments\",\"type\":{\"type\":\"record\",\"name\":\"FunctionArguments\",\"fields\":[{\"name\":\"arg0\",\"type\":\"AnyType\"},{\"name\":\"arg1\",\"type\":\"AnyType\"}]}},{\"name\":\"returnValue\",\"type\":\"AnyType\"}]}";
        assertTrue(UdfParamUtil.isValid(param));
    }
}
