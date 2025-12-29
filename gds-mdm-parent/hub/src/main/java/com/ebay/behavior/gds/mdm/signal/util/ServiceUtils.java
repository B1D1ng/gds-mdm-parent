package com.ebay.behavior.gds.mdm.signal.util;

import com.ebay.behavior.gds.mdm.common.model.Auditable;
import com.ebay.behavior.gds.mdm.common.model.Model;
import com.ebay.behavior.gds.mdm.common.model.VersionedModel;
import com.ebay.behavior.gds.mdm.signal.common.model.Attribute;
import com.ebay.behavior.gds.mdm.signal.common.model.Event;
import com.ebay.behavior.gds.mdm.signal.common.model.Field;
import com.ebay.behavior.gds.mdm.signal.common.model.Signal;
import com.ebay.com.google.common.collect.Sets;

import lombok.experimental.UtilityClass;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.InvalidPropertyException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.Auditable.CREATE_DATE;
import static com.ebay.behavior.gds.mdm.common.model.Auditable.UPDATE_DATE;
import static com.ebay.behavior.gds.mdm.common.model.Model.COMMA;
import static com.ebay.behavior.gds.mdm.common.model.Model.ID;
import static com.ebay.behavior.gds.mdm.common.model.Model.REVISION;
import static java.util.stream.Collectors.toSet;

@UtilityClass
public class ServiceUtils {

    public static final int PREFIX_MIN_LENGTH = 2;

    public static <M extends Model> void setModelProperty(M model, String propertyName, Object value) {
        Validate.isTrue(Objects.nonNull(model), "model cannot be null");
        Validate.isTrue(Objects.nonNull(propertyName), "propertyName cannot be null");

        new BeanWrapperImpl(model).setPropertyValue(propertyName, value);
    }

    public static <S extends Model, T extends Model> void copyOverwriteAllProperties(S source, T target) {
        Validate.isTrue(Objects.nonNull(source), "source cannot be null");
        Validate.isTrue(Objects.nonNull(target), "target cannot be null");
        BeanUtils.copyProperties(source, target, ID);
    }

    public static <S extends Model, T extends Model> void copyModelProperties(S source, T target) {
        BeanUtils.copyProperties(source, target, getNullPropertyNames(source, Set.of(ID)));
    }

    public static <S extends Model, T extends Model> void copyModelProperties(S source, T target, Set<String> ignoredProps) {
        Validate.isTrue(Objects.nonNull(ignoredProps), "ignoredProps cannot be null");
        val allIgnored = Sets.union(Set.of(ID), ignoredProps);
        BeanUtils.copyProperties(source, target, getNullPropertyNames(source, allIgnored));
        nullifyProperties(target, ignoredProps);
    }

    private static <T extends Model> void nullifyProperties(T target, Set<String> ignoredProps) {
        val wrapper = new BeanWrapperImpl(target);
        for (String prop : ignoredProps) {
            wrapper.setPropertyValue(prop, null);
        }
    }

    public static <S extends Auditable, T extends Auditable> void copyAuditableProperties(S source, T target, Set<String> ignoredProps) {
        Validate.isTrue(Objects.nonNull(ignoredProps), "ignoredProps cannot be null ");
        val allIgnored = Sets.union(Set.of(ID, CREATE_DATE, UPDATE_DATE), ignoredProps);
        BeanUtils.copyProperties(source, target, getNullPropertyNames(source, allIgnored));
        nullifyProperties(target, allIgnored);
    }

    public static <M extends Signal, T extends Signal> void copySignalProperties(M source, T target) {
        copyAuditableProperties(source, target, Set.of(REVISION, VersionedModel.VERSION, "fields", "events", "signalTemplateSourceId", "signalSourceId"));
    }

    public static <M extends Field, T extends Field> void copyFieldProperties(M source, T target) {
        copyAuditableProperties(source, target, Set.of(REVISION, "signal", "attributes"));
    }

    public static <M extends Event, T extends Event> void copyEventProperties(M source, T target) {
        copyAuditableProperties(source, target, Set.of(REVISION, "attributes", "pageIds", "moduleIds", "clickIds"));
    }

    public static <M extends Attribute, T extends Attribute> void copyAttributeProperties(M source, T target) {
        copyAuditableProperties(source, target, Set.of(REVISION, "event"));
    }

    public static Set<Long> toIdSet(String csvIds) {
        Validate.notBlank(csvIds, "csvIds cannot be blank");
        return Arrays.stream(csvIds.split(COMMA))
                .map(String::trim)
                .map(Long::parseLong)
                .collect(toSet());
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
                try {
                    value = src.getPropertyValue(name);
                } catch (InvalidPropertyException ex) {
                    emptyNames.add(pd.getName());
                }
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
