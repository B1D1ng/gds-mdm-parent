package com.ebay.behavior.gds.mdm.udf.common.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.SchemaParseException;

@Slf4j
@UtilityClass
public class UdfParamUtil {
    private static final String FUNCTION_SIGNATURE = "FunctionSignature";
    private static final String FUNCTION_ARGUMENTS = "FunctionArguments";

    public static Boolean isValid(String parameters) {
        try {
            Schema.Parser parser = new Schema.Parser();
            Schema schema = parser.parse(parameters);

            // Validate FunctionSignature
            if (!FUNCTION_SIGNATURE.equals(schema.getName())) {
                log.error("Invalid record name: FunctionSignature expected");
                return false;
            }

            // Validate fields
            Schema.Field argumentsField = schema.getField("arguments");
            if (argumentsField == null || !FUNCTION_ARGUMENTS.equals(argumentsField.schema().getName())) {
                log.error("Invalid field or record name for arguments");
                return false;
            }

            // Check if arguments.fields is record
            Schema argumentsSchema = argumentsField.schema();
            if (argumentsSchema.getType() != Schema.Type.RECORD) {
                log.error("Invalid fields structure for arguments");
                return false;
            }

            Schema.Field returnValueField = schema.getField("returnValue");
            if (returnValueField == null) {
                log.error("Invalid field name or type for returnValue");
                return false;
            }

            return true;
        } catch (SchemaParseException e) {
            log.error("Invalid Avro Schema: " + e.getMessage());
            return false;
        }
    }
}
