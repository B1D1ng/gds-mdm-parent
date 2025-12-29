package com.ebay.behavior.gds.mdm.signal.util;

import com.ebay.behavior.gds.mdm.signal.common.model.FieldGroup;
import com.ebay.behavior.gds.mdm.signal.common.model.MetadataField;

import lombok.experimental.UtilityClass;
import lombok.val;
import org.apache.commons.lang3.Validate;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@UtilityClass
public class FieldGroupUtils {

    public static <F extends MetadataField> Set<FieldGroup<F>> getAllFieldGroups(Set<F> fields) {
        Validate.isTrue(Objects.nonNull(fields), "fields cannot be null");

        val fieldsByKey = fields.stream().collect(groupingBy(F::getGroupKey));

        return fieldsByKey.values().stream()
                .map(FieldGroup::new)
                .collect(Collectors.toUnmodifiableSet());
    }
}
