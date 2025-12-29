package com.ebay.behavior.gds.mdm.contract.util;

import com.ebay.behavior.gds.mdm.common.model.Model;

import lombok.experimental.UtilityClass;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.Model.ID;

@UtilityClass
public class ServiceUtils {
    public static <S extends Model, T extends Model> void copyModelProperties(S source, T target) {
        BeanUtils.copyProperties(source, target, getNullPropertyNames(source, Set.of(ID)));
    }

    private static String[] getNullPropertyNames(Object source, Set<String> ignoredProps) {
        Validate.isTrue(Objects.nonNull(source), "source cannot be null");
        Validate.isTrue(Objects.nonNull(ignoredProps), "ignoredProps cannot be null");

        val src = new BeanWrapperImpl(source);
        val pds = src.getPropertyDescriptors();
        val emptyNames = new HashSet<String>();

        for (val pd : pds) {
            val name = pd.getName();
            Object value = null;

            if (!ignoredProps.contains(name)) {
                value = src.getPropertyValue(name);
            }

            if (value == null) {
                emptyNames.add(pd.getName());
            }
            if (CollectionUtils.isNotEmpty(ignoredProps)) {
                emptyNames.addAll(ignoredProps);
            }
        }

        val result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }
}
