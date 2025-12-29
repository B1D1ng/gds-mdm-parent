package com.ebay.behavior.gds.mdm.contract.util;

import com.ebay.behavior.gds.mdm.contract.model.UnstagedContract;

import org.junit.jupiter.api.Test;

import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.updateContractRequest;
import static org.assertj.core.api.Assertions.assertThat;

class ServiceUtilsTest {

    @Test
    void copyModelProperties() {
        var source = updateContractRequest("test").withId(123L).withRevision(2);
        var target = new UnstagedContract();

        ServiceUtils.copyModelProperties(source, target);

        // copied properties
        assertThat(target.getDescription()).isEqualTo(source.getDescription());
        assertThat(target.getName()).isEqualTo(source.getName());
        assertThat(target.getDl()).isEqualTo(source.getDl());
        assertThat(target.getStatus()).isEqualTo(source.getStatus());
        assertThat(target.getEnvironment()).isEqualTo(source.getEnvironment());
        assertThat(target.getRevision()).isEqualTo(source.getRevision());

        //not copied properties
        assertThat(target.getId()).isNull();
        assertThat(target.getCreateBy()).isNull();
        assertThat(target.getUpdateBy()).isNull();
        assertThat(target.getCreateDate()).isNull();
        assertThat(target.getUpdateDate()).isNull();
    }
}