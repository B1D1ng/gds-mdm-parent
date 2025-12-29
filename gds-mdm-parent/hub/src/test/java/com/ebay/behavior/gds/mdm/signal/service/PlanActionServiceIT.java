package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.model.audit.AuditLogParams;
import com.ebay.behavior.gds.mdm.commonSvc.service.EmailService;
import com.ebay.behavior.gds.mdm.signal.common.model.Plan;
import com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus;
import com.ebay.behavior.gds.mdm.signal.common.model.PlanUserAction;
import com.ebay.behavior.gds.mdm.commonTestUtil.TestRequestContextUtils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.ebay.behavior.gds.mdm.common.model.audit.AuditMode.BASIC;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus.REJECTED;
import static com.ebay.behavior.gds.mdm.signal.common.model.PlanUserAction.APPROVE;
import static com.ebay.behavior.gds.mdm.signal.common.model.PlanUserAction.CANCEL;
import static com.ebay.behavior.gds.mdm.signal.common.model.PlanUserAction.COMPLETE;
import static com.ebay.behavior.gds.mdm.signal.common.model.PlanUserAction.HIDE;
import static com.ebay.behavior.gds.mdm.signal.common.model.PlanUserAction.REJECT;
import static com.ebay.behavior.gds.mdm.signal.common.model.PlanUserAction.SUBMIT_FOR_REVIEW;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestAuthFilter.IT_TEST_USER;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.plan;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PlanActionServiceIT {

    @MockitoBean
    private EmailService emailService;

    @Autowired
    private PlanActionService service;

    @Autowired
    private PlanService planService;

    private Plan plan;

    @BeforeAll
    void setUpAll() {
        TestRequestContextUtils.setUser(IT_TEST_USER);
    }

    @BeforeEach
    void setUp() {
        plan = planService.create(plan());
    }

    @Test
    void complete() {
        var updated = service.complete(plan.getId());

        assertThat(updated.getId()).isEqualTo(plan.getId());
        validateBasicAuditLog(COMPLETE);
    }

    @Test
    void complete_onRejected() {
        plan.setStatus(REJECTED);
        plan = planService.update(plan);
        var before = plan.getStatus();
        assertThat(before).isEqualTo(REJECTED);

        var updated = service.complete(plan.getId());

        var after = updated.getStatus();
        assertThat(after).isEqualTo(PlanStatus.DEVELOPMENT);
        validateBasicAuditLog(COMPLETE);
    }

    @Test
    void submitForReview() {
        var updated = service.submitForReview(plan.getId());

        assertThat(updated.getId()).isEqualTo(plan.getId());
        assertThat(updated.getStatus()).isEqualTo(SUBMIT_FOR_REVIEW.getPlanStatus());
        assertThat(updated.getUpdateBy()).isEqualTo(IT_TEST_USER);
        validateBasicAuditLog(SUBMIT_FOR_REVIEW);
    }

    @Test
    void approve() {
        service.submitForReview(plan.getId());
        var updated = service.approve(plan.getId());

        assertThat(updated.getId()).isEqualTo(plan.getId());
        assertThat(updated.getStatus()).isEqualTo(APPROVE.getPlanStatus());
        assertThat(updated.getUpdateBy()).isEqualTo(IT_TEST_USER);
        validateBasicAuditLog(APPROVE);
    }

    @Test
    void hide() {
        var updated = service.hide(plan.getId());

        assertThat(updated.getId()).isEqualTo(plan.getId());
        assertThat(updated.getStatus()).isEqualTo(HIDE.getPlanStatus());
        assertThat(updated.getUpdateBy()).isEqualTo(IT_TEST_USER);
        validateBasicAuditLog(HIDE);
    }

    @Test
    void cancel() {
        var updated = service.cancel(plan.getId());

        assertThat(updated.getId()).isEqualTo(plan.getId());
        assertThat(updated.getStatus()).isEqualTo(CANCEL.getPlanStatus());
        assertThat(updated.getUpdateBy()).isEqualTo(IT_TEST_USER);
        validateBasicAuditLog(CANCEL);
    }

    @Test
    void reject() {
        var comment = getRandomSmallString();
        service.submitForReview(plan.getId());
        var updated = service.reject(plan.getId(), comment);

        assertThat(updated.getId()).isEqualTo(plan.getId());
        assertThat(updated.getStatus()).isEqualTo(REJECTED);
        assertThat(updated.getComment()).isEqualTo(comment);
        assertThat(updated.getUpdateBy()).isEqualTo(IT_TEST_USER);
        validateBasicAuditLog(REJECT);
    }

    void validateBasicAuditLog(PlanUserAction action) {
        var auditParams = AuditLogParams.ofNonVersioned(plan.getId(), BASIC);
        var auditLog = planService.getAuditLog(auditParams);
        var latest = auditLog.get(auditLog.size() - 1);

        assertThat(auditLog).isNotEmpty();
        assertThat(latest.getChangeReason()).isEqualTo(action.getValue());
    }
}
