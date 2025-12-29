package com.ebay.behavior.gds.mdm.udf.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.Model.COMMA;
import static java.util.stream.Collectors.toSet;

@UtilityClass
public class UdfUtils {
    public static final String WITH_ASSOCIATIONS = "withAssociations";

    public static final String UDFMM = "/udfmm";

    public static final String UDC = "/udc";

    public static Set<Long> toIdSet(String csvIds) {
        Validate.notBlank(csvIds, "csvIds cannot be blank");
        return Arrays.stream(csvIds.split(COMMA))
                .map(String::trim)
                .map(Long::parseLong)
                .collect(toSet());
    }

    public static String incrementStringNumber(String numberStr) {
        try {
            int number = Integer.parseInt(numberStr);
            number++;
            return String.valueOf(number);
        } catch (NumberFormatException e) {
            return numberStr;
        }
    }
}
