package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.signal.common.model.Plan;
import com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus;
import com.ebay.behavior.gds.mdm.signal.common.model.PlanUserAction;
import com.ebay.behavior.gds.mdm.signal.config.GovernanceConfiguration;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.Model.COMMA;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.UNKNOWN;
import static com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus.APPROVED_BY_GOVERNANCE;
import static com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus.CREATED;
import static com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus.PRODUCTION;
import static com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus.STAGING;
import static com.ebay.behavior.gds.mdm.signal.common.model.PlanUserAction.CANCEL;
import static com.ebay.behavior.gds.mdm.signal.common.model.PlanUserAction.COMPLETE;
import static com.ebay.behavior.gds.mdm.signal.common.model.PlanUserAction.HIDE;
import static com.ebay.behavior.gds.mdm.signal.common.model.PlanUserAction.SUBMIT_FOR_REVIEW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class OwnershipServiceTest {

    @Spy
    private GovernanceConfiguration configuration;

    @InjectMocks
    private OwnershipService service;

    @BeforeEach
    public void setup() {
        setField(configuration, "moderatorSet", Set.of("moderator1", "moderator2"));
    }

    @Test
    void getUserPermissions_emptyOwners() {
        var user = "user";
        var plan = Plan.builder().owners("").createBy(user).build();

        val permissions = service.getUserPermissions(plan, user);

        assertThat(permissions).isEmpty();
    }

    @Test
    void getUserPermissions_unknownUser() {
        var user = UNKNOWN;
        var plan = Plan.builder().owners(user).createBy(user).build();

        val permissions = service.getUserPermissions(plan, user);

        assertThat(permissions).isEmpty();
    }

    @Test
    void setUserPermissions_noUser() {
        var user = "user";
        var plan = Plan.builder().owners("other_user").createBy(user).status(CREATED).build();

        service.setUserPermissions(plan, null);

        assertThat(plan.getOperations()).isEmpty();
    }

    @Test
    void setUserPermissions_userIsOwnerNewStatus() {
        var user = "user";
        var plan = Plan.builder().createBy("other_user").owners(user).status(CREATED).build();

        service.setUserPermissions(plan, user);

        var expected = List.of(SUBMIT_FOR_REVIEW, COMPLETE, CANCEL);
        assertThat(plan.getOperations()).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void setUserPermissions_userIsOwnerNewStatus_multipleOwners() {
        var user = "user";
        var plan = Plan.builder().createBy("other_user").owners(String.join(COMMA, user, "other_owner"))
                .status(CREATED).build();

        service.setUserPermissions(plan, user);

        var expected = List.of(SUBMIT_FOR_REVIEW, COMPLETE, CANCEL);
        assertThat(plan.getOperations()).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void setUserPermissions_userIsOwnerStagingStatus() {
        var user = "user";
        var plan = Plan.builder().createBy("other_user").owners(user).status(STAGING).build();

        service.setUserPermissions(plan, user);

        var expected = List.of(PlanUserAction.PROMOTE_TO_PROD);
        assertThat(plan.getOperations()).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void setUserPermissions_userIsOwnerProdStatus() {
        var user = "user";
        var plan = Plan.builder().createBy("other_user").owners(user).status(PRODUCTION).build();

        service.setUserPermissions(plan, user);

        assertThat(plan.getOperations()).isEmpty();
    }

    @Test
    void setUserPermissions_userIsOwnerApprovedStatus() {
        var user = "user";
        var plan = Plan.builder().createBy("other_user").owners(user).status(APPROVED_BY_GOVERNANCE).build();

        service.setUserPermissions(plan, user);

        var expected = List.of(CANCEL, PlanUserAction.PROMOTE_TO_STAGING);
        assertThat(plan.getOperations()).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void setUserPermissions_userNotOwner() {
        var user = "user";
        var plan = Plan.builder().createBy("other_user").owners("another_user").status(CREATED).build();

        service.setUserPermissions(plan, user);

        assertThat(plan.getOperations()).isEmpty();
    }

    @Test
    void setUserPermissions_moderatorNewStatus() {
        var user = "moderator1";
        var plan = Plan.builder().createBy("other_user").owners("another_user").status(CREATED).build();

        service.setUserPermissions(plan, user);

        var expected = List.of(HIDE, CANCEL);
        assertThat(plan.getOperations()).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void setUserPermissions_moderatorStagingStatus() {
        var user = "moderator1";
        var plan = Plan.builder().createBy("other_user").owners("another_user").status(STAGING).build();

        service.setUserPermissions(plan, user);

        var expected = List.of(CANCEL, HIDE);
        assertThat(plan.getOperations()).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void setUserPermissions_moderatorProdStatus() {
        var user = "moderator1";
        var plan = Plan.builder().createBy("other_user").owners("another_user").status(PRODUCTION).build();

        service.setUserPermissions(plan, user);

        assertThat(plan.getOperations()).isEmpty();
    }

    @Test
    void setUserPermissions_moderatorSubmitStatus() {
        var user = "moderator1";
        var plan = Plan.builder().createBy("other_user").owners("another_user").status(PlanStatus.SUBMITTED_FOR_REVIEW).build();

        service.setUserPermissions(plan, user);

        var expected = List.of(CANCEL, PlanUserAction.REJECT, PlanUserAction.APPROVE);
        assertThat(plan.getOperations()).containsExactlyInAnyOrderElementsOf(expected);
    }
}