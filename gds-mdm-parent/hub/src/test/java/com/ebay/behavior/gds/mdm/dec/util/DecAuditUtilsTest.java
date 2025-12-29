package com.ebay.behavior.gds.mdm.dec.util;

import com.ebay.behavior.gds.mdm.dec.model.LdmEntityIndex;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils;

import org.javers.core.diff.Diff;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DecAuditUtilsTest {

    @Test
    void getChanges() {
        Namespace testObject1 = Namespace.builder().name("Name1").owners("User1").build();
        Namespace testObject2 = Namespace.builder().name("Name2").owners("User2").build();
        Diff diff = DecAuditUtils.getChanges(testObject1, testObject2);
        assertThat(diff.getChanges()).hasSize(2);
        assertThat(diff.getPropertyChanges("name").get(0).getLeft()).isEqualTo(testObject1.getName());
        assertThat(diff.getPropertyChanges("name").get(0).getRight()).isEqualTo(testObject2.getName());
        assertThat(diff.getPropertyChanges("owners").get(0).getLeft()).isEqualTo(testObject1.getOwners());
        assertThat(diff.getPropertyChanges("owners").get(0).getRight()).isEqualTo(testObject2.getOwners());
    }

    @Test
    void getIgnoredProperties() {
        LdmEntityIndex testObject = TestModelUtils.ldmEntityIndex(1L);
        String[] nonNullPropertyNames = DecAuditUtils.getIgnoredProperties(testObject, Set.of());
        Set<String> expectedResult = Set.of("class", "viewType", "currentVersion", "baseEntityId");
        assertThat(nonNullPropertyNames.length).isEqualTo(expectedResult.size());
        for (String nonNullPropertyName : nonNullPropertyNames) {
            assertThat(expectedResult.contains(nonNullPropertyName)).isTrue();
        }
    }

    @Test
    void getIgnoredProperties_WithAdditionalProps() {
        LdmEntityIndex testObject = TestModelUtils.ldmEntityIndex(1L);
        String[] nonNullPropertyNames = DecAuditUtils.getIgnoredProperties(testObject, Set.of("updateDate"));
        Set<String> expectedResult = Set.of("class", "viewType", "currentVersion", "baseEntityId", "updateDate");
        assertThat(nonNullPropertyNames.length).isEqualTo(expectedResult.size());
        for (String nonNullPropertyName : nonNullPropertyNames) {
            assertThat(expectedResult.contains(nonNullPropertyName)).isTrue();
        }
    }
}
