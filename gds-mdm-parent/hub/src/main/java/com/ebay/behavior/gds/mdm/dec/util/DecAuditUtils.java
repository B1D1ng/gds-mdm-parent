package com.ebay.behavior.gds.mdm.dec.util;

import com.ebay.behavior.gds.mdm.common.model.Auditable;
import com.ebay.behavior.gds.mdm.dec.model.Dataset;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;

import lombok.experimental.UtilityClass;
import lombok.val;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Change;
import org.javers.core.diff.Diff;
import org.javers.core.diff.DiffBuilder;
import org.javers.core.diff.changetype.InitialValueChange;
import org.javers.core.diff.changetype.ReferenceChange;
import org.javers.core.diff.changetype.TerminalValueChange;
import org.javers.core.diff.changetype.map.MapChange;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

@UtilityClass
public class DecAuditUtils {

    public static final Javers JAVERS;

    private static final Predicate<Change> CHANGE_PREDICATE = change ->
            !(change instanceof InitialValueChange)
                    && !(change instanceof ReferenceChange)
                    && !(change instanceof TerminalValueChange)
                    && !(change instanceof MapChange);

    static {
        JAVERS = getJaversBuilder().build();
    }

    private static JaversBuilder getJaversBuilder() {
        val builder = JaversBuilder.javers();
        builder.registerValueObject(Namespace.class);
        builder.registerValueObject(LdmEntity.class);
        builder.registerValueObject(Dataset.class);
        return builder;
    }

    public static <H extends Auditable> Diff getChanges(H prev, H curr) {
        val diff = JAVERS.compare(prev, curr);
        var valueChanges = diff.getChanges(CHANGE_PREDICATE);
        return new DiffBuilder().addChanges(valueChanges).build();
    }

    private Set<String> getNonNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyFields = new HashSet<String>();
        for (java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue != null) {
                emptyFields.add(pd.getName());
            }
        }

        return emptyFields;
    }

    public static String[] getIgnoredProperties(Object source, Set<String> additionalProps) {
        Set<String> nonEmptyProps = getNonNullPropertyNames(source);
        nonEmptyProps.addAll(additionalProps);
        String[] result = new String[nonEmptyProps.size()];
        return nonEmptyProps.toArray(result);
    }
}
