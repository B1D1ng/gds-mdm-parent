package com.ebay.behavior.gds.mdm.dec.testUtil;

import com.ebay.behavior.gds.mdm.dec.model.*;
import com.ebay.behavior.gds.mdm.dec.model.enums.*;

import lombok.experimental.UtilityClass;

import java.util.concurrent.atomic.AtomicInteger;

import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.getRandomString;

@UtilityClass
public class TestPhsicalAssetInfraUtils {
    // Counter to keep track of which combination to return
    private static final AtomicInteger counter = new AtomicInteger(0);

    // Array of possible InfraType values
    private static final InfraType[] INFRA_TYPES = InfraType.values();

    // Array of possible PropertyType values
    private static final PropertyType[] PROPERTY_TYPES = PropertyType.values();

    public static PhysicalAssetInfra physicalAssetInfra() {
        return PhysicalAssetInfra.builder()
                .infraType(InfraType.DLS)
                .propertyType(PropertyType.RHEOS_NAMESPACE)
                .propertyDetails(getRandomString())
                .build();
    }

    public static PhysicalAssetInfraGlobalProperty physicalAssetInfraGlobalProperty() {
        // Get next counter value and wrap around if needed
        int index = counter.getAndIncrement() % (INFRA_TYPES.length * PROPERTY_TYPES.length);

        // Calculate which InfraType and PropertyType to use
        InfraType infraType = INFRA_TYPES[index % INFRA_TYPES.length];
        PropertyType propertyType = PROPERTY_TYPES[(index / INFRA_TYPES.length) % PROPERTY_TYPES.length];

        return PhysicalAssetInfraGlobalProperty.builder()
                .infraType(infraType)
                .propertyType(propertyType)
                .propertyDetails(getRandomString())
                .build();
    }
}