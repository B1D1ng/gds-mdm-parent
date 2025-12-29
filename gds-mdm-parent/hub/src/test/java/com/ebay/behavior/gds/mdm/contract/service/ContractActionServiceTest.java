package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.contract.client.ControlPlaneManagerClient;
import com.ebay.behavior.gds.mdm.contract.model.ContractPipeline;
import com.ebay.behavior.gds.mdm.contract.model.ContractStatus;
import com.ebay.behavior.gds.mdm.contract.model.ContractUserAction;
import com.ebay.behavior.gds.mdm.contract.model.DeployContractRequest;
import com.ebay.behavior.gds.mdm.contract.model.DeployScope;
import com.ebay.behavior.gds.mdm.contract.model.UnstagedContract;
import com.ebay.behavior.gds.mdm.contract.model.client.ControlPlaneRequest;
import com.ebay.behavior.gds.mdm.contract.model.client.ControlPlaneResponse;
import com.ebay.behavior.gds.mdm.contract.model.exception.ControlPlaneManagerException;
import com.ebay.behavior.gds.mdm.signal.service.SignalImportService;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import jakarta.ws.rs.ForbiddenException;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.ebay.behavior.gds.mdm.common.model.Environment.PRODUCTION;
import static com.ebay.behavior.gds.mdm.common.model.Environment.STAGING;
import static com.ebay.behavior.gds.mdm.contract.model.ContractStatus.ARCHIVED;
import static com.ebay.behavior.gds.mdm.contract.model.ContractStatus.DEPLOYING_PRODUCTION;
import static com.ebay.behavior.gds.mdm.contract.model.ContractStatus.DEPLOYING_STAGING;
import static com.ebay.behavior.gds.mdm.contract.model.ContractStatus.DEPLOYING_TEST;
import static com.ebay.behavior.gds.mdm.contract.model.ContractStatus.DEPLOY_PRODUCTION_FAILED;
import static com.ebay.behavior.gds.mdm.contract.model.ContractStatus.DEPLOY_STAGING_FAILED;
import static com.ebay.behavior.gds.mdm.contract.model.ContractStatus.STOPPING_TEST;
import static com.ebay.behavior.gds.mdm.contract.model.ContractStatus.TESTING;
import static com.ebay.behavior.gds.mdm.contract.model.ContractStatus.TESTING_FAILED;
import static com.ebay.behavior.gds.mdm.contract.model.ContractUserAction.ARCHIVE;
import static com.ebay.behavior.gds.mdm.contract.model.ContractUserAction.COMPLETE_TEST;
import static com.ebay.behavior.gds.mdm.contract.model.ContractUserAction.DEPLOY_PRODUCTION;
import static com.ebay.behavior.gds.mdm.contract.model.ContractUserAction.DEPLOY_STAGING;
import static com.ebay.behavior.gds.mdm.contract.model.ContractUserAction.TEST;
import static com.ebay.behavior.gds.mdm.contract.model.ContractUserAction.UPDATE;
import static com.ebay.behavior.gds.mdm.contract.model.DeployScope.RELEASE;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.kafkaSource;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.routing;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.unstagedContract;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractActionServiceTest {

    @InjectMocks
    private ContractActionService contractActionService;

    @Mock
    private ContractOwnershipService contractOwnershipService;

    @Mock
    private UnstagedContractService unstagedContractService;

    @Mock
    private ContractPipelineService contractPipelineService;

    @Mock
    private ControlPlaneManagerClient controlPlaneManagerClient;

    @Mock
    private SignalImportService signalImportService;

    @Mock
    private ContractAutoGeneratorService contractAutoGeneratorService;

    private UnstagedContract contract;
    private DeployContractRequest deployRequest;
    private ContractPipeline pipeline;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        val kafkaSource = kafkaSource("test_source");

        val routing = routing("test").setComponentChain(Lists.newArrayList(kafkaSource));

        contract = unstagedContract("kafka_contract").setId(123L).setRoutings(Sets.newHashSet(routing)).setEnvironment(STAGING);
        contract.setVersion(1);

        deployRequest = DeployContractRequest.builder()
                .id(123L)
                .version(1)
                .appName("test-app")
                .namespace("test-namespace")
                .build();

        pipeline = ContractPipeline.builder()
                .id(456L)
                .contractId(123L)
                .contractVersion(1)
                .environment(STAGING)
                .deployScope(DeployScope.TEST)
                .dlsPipelineId("dls-pipeline-123")
                .build();
    }

    // ===== Helper Methods for Mock Setup =====

    private void mockPermissions(UnstagedContract contract, ContractUserAction action) {
        when(contractOwnershipService.getUserPermissions(eq(contract), anyString()))
                .thenReturn(List.of(action));
    }

    private void mockBasicDeployment(UnstagedContract contract, ContractUserAction action) {
        when(unstagedContractService.getByIdAndLatestVersion(123L)).thenReturn(contract);
        mockPermissions(contract, action);
        when(unstagedContractService.update(contract)).thenReturn(contract);
    }

    private void mockStreamingDeployment(UnstagedContract contract, ContractUserAction action,
                                         Environment env) {
        mockBasicDeployment(contract, action);
        when(contractPipelineService.findPipelineByContract(123L, 1, env))
                .thenReturn(Optional.of(pipeline));
        when(controlPlaneManagerClient.requestWorkflow(any())).thenReturn(new ControlPlaneResponse());
    }

    // ===== Permission and Basic Action Tests =====

    @Nested
    class PermissionTests {
        @Test
        void update_withValidPermissions_updatesContract() {
            mockBasicDeployment(contract, UPDATE);

            val result = contractActionService.update(123L);

            assertThat(result.getStatus()).isEqualTo(UPDATE.getContractStatus());
            verify(unstagedContractService).update(contract);
        }

        @Test
        void update_withoutPermissions_throwsForbiddenException() {
            when(unstagedContractService.getByIdAndLatestVersion(123L)).thenReturn(contract);
            when(contractOwnershipService.getUserPermissions(eq(contract), anyString())).thenReturn(List.of());

            assertThatThrownBy(() -> contractActionService.update(123L))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        void archive_withValidPermissions_archivesContract() {
            mockBasicDeployment(contract, ARCHIVE);

            val result = contractActionService.archive(123L);

            assertThat(result.getStatus()).isEqualTo(ARCHIVED);
            verify(unstagedContractService).update(contract);
        }
    }

    // ===== Streaming Deployment Tests =====

    @Nested
    class StreamingDeploymentTests {
        @ParameterizedTest
        @MethodSource("com.ebay.behavior.gds.mdm.contract.service.ContractActionServiceTest#streamingDeploymentScenarios")
        void deploy_withValidPermissions_deploysSuccessfully(ContractUserAction action,
                                                             Environment env,
                                                             ContractStatus expectedStatus) {
            mockStreamingDeployment(contract, action, env);

            UnstagedContract result;
            if (action == DEPLOY_STAGING) {
                result = contractActionService.deployStaging(deployRequest);
            } else if (action == DEPLOY_PRODUCTION) {
                result = contractActionService.deployProduction(deployRequest);
            } else {
                result = contractActionService.test(deployRequest);
            }

            assertThat(result.getStatus()).isEqualTo(expectedStatus);
            verify(controlPlaneManagerClient).requestWorkflow(any());
            verify(unstagedContractService).update(contract);
        }
    }

    static Stream<Arguments> streamingDeploymentScenarios() {
        return Stream.of(
                Arguments.of(DEPLOY_STAGING, STAGING, DEPLOYING_STAGING),
                Arguments.of(DEPLOY_PRODUCTION, PRODUCTION, DEPLOYING_PRODUCTION),
                Arguments.of(TEST, STAGING, DEPLOYING_TEST)
        );
    }

    @Test
    void completeTest_withValidPermissions_completesTest() {
        contract.setStatus(TESTING);
        when(unstagedContractService.getByIdAndLatestVersion(123L)).thenReturn(contract);
        when(contractOwnershipService.getUserPermissions(eq(contract), anyString())).thenReturn(List.of(COMPLETE_TEST));
        when(contractPipelineService.findPipelineByContract(123L, 1, STAGING, DeployScope.TEST))
                .thenReturn(Optional.of(pipeline));
        when(controlPlaneManagerClient.requestWorkflow(any())).thenReturn(new ControlPlaneResponse());
        when(unstagedContractService.update(contract)).thenReturn(contract);

        val result = contractActionService.completeTest(123L);

        assertThat(result.getStatus()).isEqualTo(STOPPING_TEST);
        verify(controlPlaneManagerClient).requestWorkflow(any(ControlPlaneRequest.class));
        verify(unstagedContractService).update(contract);
    }

    @Nested
    class WorkflowFailureTests {
        @ParameterizedTest
        @MethodSource("com.ebay.behavior.gds.mdm.contract.service.ContractActionServiceTest#workflowFailureScenarios")
        void deployment_workflowFailure_marksAsDeployFailed(ContractUserAction action,
                                                            Environment env,
                                                            ContractStatus expectedFailureStatus) {
            when(unstagedContractService.getByIdAndLatestVersion(123L)).thenReturn(contract);
            mockPermissions(contract, action);
            when(contractPipelineService.findPipelineByContract(123L, 1, env))
                    .thenReturn(Optional.of(pipeline));
            when(controlPlaneManagerClient.requestWorkflow(any()))
                    .thenThrow(new ControlPlaneManagerException("Workflow failed"));
            when(unstagedContractService.update(contract)).thenReturn(contract);

            if (action == DEPLOY_STAGING) {
                contractActionService.deployStaging(deployRequest);
            } else if (action == DEPLOY_PRODUCTION) {
                contractActionService.deployProduction(deployRequest);
            } else {
                contractActionService.test(deployRequest);
            }

            verify(unstagedContractService).updateLatestVersion(argThat(req ->
                    req.getId().equals(123L) && req.getStatus().equals(expectedFailureStatus)
            ));
        }
    }

    static Stream<Arguments> workflowFailureScenarios() {
        return Stream.of(
                Arguments.of(DEPLOY_STAGING, STAGING, DEPLOY_STAGING_FAILED),
                Arguments.of(DEPLOY_PRODUCTION, PRODUCTION, DEPLOY_PRODUCTION_FAILED),
                Arguments.of(TEST, STAGING, TESTING_FAILED)
        );
    }

    @Nested
    class CompleteTestTests {
        @Test
        void completeTest_noPipeline_doesNotTriggerWorkflow() {
            contract.setStatus(TESTING);
            mockBasicDeployment(contract, COMPLETE_TEST);
            when(contractPipelineService.findPipelineByContract(123L, 1, STAGING))
                    .thenReturn(Optional.empty());

            contractActionService.completeTest(123L);

            verify(controlPlaneManagerClient, never()).requestWorkflow(any());
            verify(unstagedContractService).update(contract);
        }

        @Test
        void completeTest_wrongDeployScope_doesNotTriggerWorkflow() {
            contract.setStatus(TESTING);
            pipeline.setDeployScope(RELEASE);
            mockBasicDeployment(contract, COMPLETE_TEST);
            when(contractPipelineService.findPipelineByContract(123L, 1, STAGING))
                    .thenReturn(Optional.of(pipeline));

            contractActionService.completeTest(123L);

            verify(controlPlaneManagerClient, never()).requestWorkflow(any());
            verify(unstagedContractService).update(contract);
        }

        @Test
        void completeTest_workflowFailure_rollsBackToTesting() {
            contract.setStatus(TESTING);
            pipeline.setDeployScope(DeployScope.TEST);
            mockBasicDeployment(contract, COMPLETE_TEST);
            when(contractPipelineService.findPipelineByContract(123L, 1, STAGING, DeployScope.TEST))
                    .thenReturn(Optional.of(pipeline));
            when(controlPlaneManagerClient.requestWorkflow(any()))
                    .thenThrow(new ControlPlaneManagerException("Workflow failed"));

            assertThatThrownBy(() -> contractActionService.completeTest(123L))
                    .isInstanceOf(ControlPlaneManagerException.class);
        }
    }

    @Nested
    class PipelineManagementTests {
        @Test
        void deployStaging_noPipeline_createsPipelineAndDeploys() {
            mockBasicDeployment(contract, DEPLOY_STAGING);
            when(contractPipelineService.findPipelineByContract(123L, 1, STAGING))
                    .thenReturn(Optional.empty());
            when(contractPipelineService.create(any())).thenReturn(pipeline);
            when(controlPlaneManagerClient.requestWorkflow(any())).thenReturn(new ControlPlaneResponse());

            contractActionService.deployStaging(deployRequest);

            verify(contractPipelineService).create(any());
            verify(controlPlaneManagerClient).requestWorkflow(any());
        }

        @Test
        void deployStaging_existingPipelineWithDifferentScope_updatesPipeline() {
            pipeline.setDeployScope(DeployScope.TEST);
            mockBasicDeployment(contract, DEPLOY_STAGING);
            when(contractPipelineService.findPipelineByContract(123L, 1, STAGING))
                    .thenReturn(Optional.of(pipeline));
            when(controlPlaneManagerClient.requestWorkflow(any())).thenReturn(new ControlPlaneResponse());

            contractActionService.deployStaging(deployRequest);

            assertThat(pipeline.getDeployScope()).isEqualTo(RELEASE);
            verify(contractPipelineService).update(pipeline);
            verify(controlPlaneManagerClient).requestWorkflow(any());
        }
    }

}