package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.contract.config.ContractGovernanceConfiguration;
import com.ebay.behavior.gds.mdm.contract.model.ContractStatus;
import com.ebay.behavior.gds.mdm.contract.model.ContractUserAction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.unstagedContract;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.getRandomString;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles(IT)
class ContractOwnershipServiceIT {

    @Autowired
    private ContractOwnershipService service;

    @Autowired
    private ContractGovernanceConfiguration configuration;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void test_setUserPermission_failed() {
        var createdContract = unstagedContract(getRandomString()).toBuilder().id(1L).version(1).build();
        createdContract.setCreateBy("creatBy");
        service.setUserPermissions(createdContract, createdContract.getCreateBy());

        assertThat(createdContract.getOperations().contains(ContractUserAction.SUBMIT)).isEqualTo(false);
        assertThat(createdContract.getOperations().contains(ContractUserAction.UPDATE)).isEqualTo(false);
        assertThat(createdContract.getOperations().contains(ContractUserAction.ARCHIVE)).isEqualTo(false);
    }

    @Test
    void test_setUserPermission_succeed() {
        var createdContract = unstagedContract(getRandomString()).toBuilder().id(1L).version(1).build();
        createdContract.setCreateBy("xianahan");
        service.setUserPermissions(createdContract, createdContract.getCreateBy());

        assertThat(createdContract.getOperations().contains(ContractUserAction.SUBMIT)).isEqualTo(true);
        assertThat(createdContract.getOperations().contains(ContractUserAction.UPDATE)).isEqualTo(true);
        assertThat(createdContract.getOperations().contains(ContractUserAction.ARCHIVE)).isEqualTo(true);
    }

    @Test
    void test_setUserPermission_stageReleased_succeed() {
        var createdContract = unstagedContract(getRandomString()).toBuilder().id(1L).version(1).status(ContractStatus.STAGING_RELEASED).build();
        createdContract.setOwners("xianahan");
        service.setUserPermissions(createdContract, createdContract.getOwners());

        assertThat(createdContract.getOperations().contains(ContractUserAction.TEST)).isEqualTo(true);
        assertThat(createdContract.getOperations().contains(ContractUserAction.UPDATE)).isEqualTo(true);
        assertThat(createdContract.getOperations().contains(ContractUserAction.ARCHIVE)).isEqualTo(true);
        assertThat(createdContract.getOperations().contains(ContractUserAction.DEPLOY_PRODUCTION)).isEqualTo(true);
    }

    @Test
    void test_getUserPermission_succeed() {
        var createdContract = unstagedContract(getRandomString()).toBuilder().id(1L).version(1).build();
        createdContract.setOwners("xianahan");
        var result = service.getUserPermissions(createdContract, createdContract.getOwners());

        assertThat(result.contains(ContractUserAction.SUBMIT)).isEqualTo(true);
        assertThat(result.contains(ContractUserAction.UPDATE)).isEqualTo(true);
        assertThat(result.contains(ContractUserAction.ARCHIVE)).isEqualTo(true);
    }
}