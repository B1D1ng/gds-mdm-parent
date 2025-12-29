package com.ebay.behavior.gds.mdm.signal.util;

import com.ebay.behavior.gds.mdm.signal.common.model.audit.PlanHistory;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;

import org.junit.jupiter.api.Test;

import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.UPDATED;
import static org.assertj.core.api.Assertions.assertThat;

class AuditUtilsTest {

    @Test
    void toHistoryRecord() {
        var plan = TestModelUtils.plan().withId(123L).withRevision(2);

        var history = AuditUtils.toHistoryRecord(plan, UPDATED, null, PlanHistory.class);

        // copied properties
        assertThat(history.getName()).isEqualTo(plan.getName());
        assertThat(history.getDescription()).isEqualTo(plan.getDescription());
        assertThat(history.getTeamDls()).isEqualTo(plan.getTeamDls());
        assertThat(history.getOwners()).isEqualTo(plan.getOwners());
        assertThat(history.getJiraProject()).isEqualTo(plan.getJiraProject());
        assertThat(history.getDomain()).isEqualTo(plan.getDomain());
        assertThat(history.getStatus()).isEqualTo(plan.getStatus());
        assertThat(history.getCreateBy()).isEqualTo(plan.getCreateBy());
        assertThat(history.getUpdateBy()).isEqualTo(plan.getUpdateBy());

        //not copied properties
        assertThat(history.getId()).isNull();
        assertThat(history.getRevision()).isNull();
        assertThat(history.getCreateDate()).isNull();
        assertThat(history.getUpdateDate()).isNull();

        // audit properties
        assertThat(history.getOriginalId()).isEqualTo(plan.getId());
        assertThat(history.getOriginalRevision()).isEqualTo(plan.getRevision());
        assertThat(history.getOriginalCreateDate()).isEqualTo(plan.getCreateDate());
        assertThat(history.getOriginalUpdateDate()).isEqualTo(plan.getUpdateDate());
        assertThat(history.getChangeType()).isEqualTo(UPDATED);
    }
}
