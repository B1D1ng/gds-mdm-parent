package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.signal.common.testUtil.TestModelUtils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlanTest {

    @Test
    void getOwnersAsList_singleOwner() {
        var plan = TestModelUtils.plan();

        var ownerList = plan.getOwnersAsList();

        assertThat(ownerList.size()).isEqualTo(1);
        assertThat(ownerList.get(0)).isEqualTo(plan.getOwners());
    }

    @Test
    void getOwnersAsList_multipleOwners() {
        var plan = TestModelUtils.plan().setOwners("owner1,owner2,owner3");

        var ownerList = plan.getOwnersAsList();

        assertThat(ownerList.size()).isEqualTo(3);
        assertThat(ownerList.get(0)).isEqualTo("owner1");
        assertThat(ownerList.get(1)).isEqualTo("owner2");
        assertThat(ownerList.get(2)).isEqualTo("owner3");
    }

    @Test
    void getOwnersAsList_noOwner() {
        var plan = TestModelUtils.plan().setOwners(null);

        var ownerList = plan.getOwnersAsList();

        assertThat(ownerList.size()).isEqualTo(0);
    }
}
