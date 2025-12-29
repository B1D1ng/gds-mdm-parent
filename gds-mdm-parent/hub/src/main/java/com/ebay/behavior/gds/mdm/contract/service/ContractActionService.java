package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.contract.client.ControlPlaneManagerClient;
import com.ebay.behavior.gds.mdm.contract.model.ContractActionRequest;
import com.ebay.behavior.gds.mdm.contract.model.ContractPipeline;
import com.ebay.behavior.gds.mdm.contract.model.ContractSourceType;
import com.ebay.behavior.gds.mdm.contract.model.ContractUserAction;
import com.ebay.behavior.gds.mdm.contract.model.DeployContractRequest;
import com.ebay.behavior.gds.mdm.contract.model.DeployScope;
import com.ebay.behavior.gds.mdm.contract.model.UnstagedContract;
import com.ebay.behavior.gds.mdm.contract.model.UpdateContractRequest;
import com.ebay.behavior.gds.mdm.contract.model.client.ControlPlaneRequest;
import com.ebay.behavior.gds.mdm.contract.model.client.ProcessType;
import com.ebay.behavior.gds.mdm.contract.model.client.WorkflowVariable;
import com.ebay.behavior.gds.mdm.contract.model.exception.ControlPlaneManagerException;
import com.ebay.behavior.gds.mdm.signal.service.SignalImportService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.ws.rs.ForbiddenException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.function.Consumer;

import static com.ebay.behavior.gds.mdm.common.model.Environment.PRODUCTION;
import static com.ebay.behavior.gds.mdm.common.model.Environment.STAGING;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.getRequestUser;
import static com.ebay.behavior.gds.mdm.contract.client.ControlPlaneManagerClient.CONTRACT_RESOURCE;
import static com.ebay.behavior.gds.mdm.contract.model.ContractSourceType.fromContract;
import static com.ebay.behavior.gds.mdm.contract.model.ContractStatus.ARCHIVED;
import static com.ebay.behavior.gds.mdm.contract.model.ContractStatus.TESTING_COMPLETE;
import static com.ebay.behavior.gds.mdm.contract.model.ContractStatus.getFailureStatus;
import static com.ebay.behavior.gds.mdm.contract.model.ContractUserAction.APPROVE;
import static com.ebay.behavior.gds.mdm.contract.model.ContractUserAction.ARCHIVE;
import static com.ebay.behavior.gds.mdm.contract.model.ContractUserAction.COMPLETE_TEST;
import static com.ebay.behavior.gds.mdm.contract.model.ContractUserAction.DEPLOY_PRODUCTION;
import static com.ebay.behavior.gds.mdm.contract.model.ContractUserAction.DEPLOY_STAGING;
import static com.ebay.behavior.gds.mdm.contract.model.ContractUserAction.REJECT;
import static com.ebay.behavior.gds.mdm.contract.model.ContractUserAction.SUBMIT;
import static com.ebay.behavior.gds.mdm.contract.model.ContractUserAction.TEST;
import static com.ebay.behavior.gds.mdm.contract.model.ContractUserAction.UPDATE;
import static com.ebay.behavior.gds.mdm.contract.model.DeployScope.RELEASE;
import static java.lang.String.valueOf;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Service
public class ContractActionService {

    @Autowired
    private ContractOwnershipService contractOwnershipService;

    @Autowired
    private UnstagedContractService unstagedContractService;

    @Autowired
    private ContractPipelineService contractPipelineService;

    @Autowired
    private ControlPlaneManagerClient controlPlaneManagerClient;

    @Autowired
    private SignalImportService signalImportService;

    @Autowired
    private ContractAutoGeneratorService contractAutoGeneratorService;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UnstagedContract update(@PositiveOrZero long contractId) {
        val contract = changeStatus(contractId, UPDATE);
        return unstagedContractService.update(contract);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UnstagedContract submit(@PositiveOrZero long contractId) {
        val contract = changeStatus(contractId, SUBMIT);
        return unstagedContractService.update(contract);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UnstagedContract approve(@PositiveOrZero long contractId) {
        val contract = changeStatus(contractId, APPROVE);
        return unstagedContractService.update(contract);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UnstagedContract reject(@PositiveOrZero long contractId, @NotNull @Valid ContractActionRequest request) {
        val contract = changeStatus(contractId, REJECT);
        contract.setComment(request.getComment());
        return unstagedContractService.update(contract);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UnstagedContract archive(@PositiveOrZero long contractId) {
        val contract = changeStatus(contractId, ARCHIVE);
        if (!(stopContract(contractId, STAGING) || stopContract(contractId, PRODUCTION))) {
            log.info("no need to stop contract {} , pipelines not exist", contractId);
            contract.setStatus(ARCHIVED);
        }
        return unstagedContractService.update(contract);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UnstagedContract deployStaging(@NotNull @Valid DeployContractRequest request) {
        val contract = changeStatus(request.getId(), DEPLOY_STAGING);
        deployContract(contract, request, STAGING, RELEASE);
        return unstagedContractService.update(contract);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UnstagedContract deployProduction(@NotNull @Valid DeployContractRequest request) {
        val contract = changeStatus(request.getId(), DEPLOY_PRODUCTION);
        deployContract(contract, request, PRODUCTION, RELEASE);
        return unstagedContractService.update(contract);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UnstagedContract test(@NotNull @Valid DeployContractRequest request) {
        val contract = changeStatus(request.getId(), TEST);
        deployContract(contract, request, STAGING, DeployScope.TEST);
        return unstagedContractService.update(contract);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UnstagedContract completeTest(@PositiveOrZero long contractId) {
        val contract = changeStatus(contractId, COMPLETE_TEST);
        if (!stopContract(contractId, STAGING, DeployScope.TEST)) {
            contract.setStatus(TESTING_COMPLETE);
        }
        return unstagedContractService.update(contract);
    }

    private void validateAction(UnstagedContract unstagedContract, ContractUserAction action) {
        var user = getRequestUser();
        val permissions = contractOwnershipService.getUserPermissions(unstagedContract, user);

        if (!permissions.contains(action)) {
            throw new ForbiddenException(String.format("User %s is not allowed to perform action %s on Contract %s status %s",
                    user, action, unstagedContract.getId(), unstagedContract.getStatus()));
        }
    }

    private UnstagedContract changeStatus(long contractId, ContractUserAction action) {
        val contract = unstagedContractService.getByIdAndLatestVersion(contractId);
        validateAction(contract, action);
        contract.setStatus(action.getContractStatus());
        return contract;
    }

    private boolean stopContract(long contractId, Environment environment) {
        return stopContract(contractId, environment, null);
    }

    private boolean stopContract(long contractId, Environment environment, DeployScope scope) {
        val contract = unstagedContractService.getByIdAndLatestVersion(contractId);

        Optional<ContractPipeline> pipelineOpt;
        if (isNull(scope)) {
            pipelineOpt = contractPipelineService.findPipelineByContract(contractId, contract.getVersion(), environment);
        } else {
            pipelineOpt = contractPipelineService.findPipelineByContract(contractId, contract.getVersion(), environment, scope);
        }
        if (pipelineOpt.isEmpty() || isBlank(pipelineOpt.get().getDlsPipelineId())) {
            log.info("no need to stop contract {}, environment {}, scope {}, pipeline not exist", contractId, environment, scope);
            return false;
        }
        val pipeline = pipelineOpt.get();
        updatePipeline(pipeline);
        triggerWorkflow(ProcessType.STOP, defaultWorkflowVariableBuilder(contractId, environment, scope, pipeline).needWait("false").build(), contractId);
        return true;
    }

    private void deployContract(UnstagedContract contract, DeployContractRequest request, Environment environment, DeployScope scope) {
        if (fromContract(contract) == ContractSourceType.HIVE) {
            autoGenForBatchContract(contract, environment);
        }

        val pipeline = getOrCreatePipeline(request, environment, scope);

        val workflowVariables = defaultWorkflowVariableBuilder(request.getId(), environment, scope, pipeline)
                .dlsAppNameId(request.getAppName())
                .dlsNamespaceId(request.getNamespace())
                .build();

        updatePipeline(pipeline);
        triggerWorkflow(ProcessType.DEPLOY, workflowVariables, request.getId(),
                throwable -> markDeployFailed(request.getId(), scope, environment));
    }

    private void autoGenForBatchContract(UnstagedContract contract, Environment environment) {
        contract.setEnvironment(environment);
        val signal = contractAutoGeneratorService.upsertUnstagedSignalsForContract(contract, environment);
        signalImportService.importStagedSignalIfAbsent(signal);
    }

    private void updatePipeline(ContractPipeline pipeline) {
        pipeline.setUpdateBy(null); // Leverage onUpdate of AbstractVersionedAuditable to set the updateBy and updateDate
        pipeline.setUpdateDate(null);
        contractPipelineService.update(pipeline);
    }

    private void markDeployFailed(long id, DeployScope scope, Environment environment) {
        unstagedContractService.updateLatestVersion(UpdateContractRequest.builder()
                .id(id)
                .status(getFailureStatus(scope, environment))
                .build());
    }

    private ContractPipeline getOrCreatePipeline(DeployContractRequest request, Environment environment, DeployScope scope) {
        val pipeline = contractPipelineService.findPipelineByContract(request.getId(),
                        request.getVersion(), environment)
                .orElseGet(() -> createNewPipeline(request, environment, scope));

        if (pipeline.getDeployScope() != scope) {
            pipeline.setDeployScope(scope);
        }

        return pipeline;
    }

    private ContractPipeline createNewPipeline(DeployContractRequest request, Environment environment, DeployScope scope) {
        return contractPipelineService.create(ContractPipeline.builder()
                .contractId(request.getId())
                .contractVersion(request.getVersion())
                .environment(environment)
                .deployScope(scope)
                .build());
    }

    private void triggerWorkflow(ProcessType processType, WorkflowVariable variables, long contractId) {
        triggerWorkflow(processType, variables, contractId, null);
    }

    private void triggerWorkflow(ProcessType processType, WorkflowVariable variables, long contractId, Consumer<Throwable> onError) {
        val request = ControlPlaneRequest.builder()
                .requester(getRequestUser())
                .resourceId(valueOf(contractId))
                .resourceType(CONTRACT_RESOURCE)
                .processType(processType.name())
                .context(variables)
                .build();

        try {
            val response = controlPlaneManagerClient.requestWorkflow(request);
            log.info("Succeed to request process, request id: {}", response.getId());
        } catch (ControlPlaneManagerException ex) {
            log.error("Failed to trigger workflow '{}' for contract {}", processType, contractId, ex);
            if (onError != null) {
                onError.accept(ex);
            } else {
                throw ex;
            }
        }
    }

    private WorkflowVariable.WorkflowVariableBuilder defaultWorkflowVariableBuilder(long contractId, Environment environment,
                                                                                    DeployScope scope, ContractPipeline pipeline) {
        return WorkflowVariable.builder()
                .contractId(valueOf(contractId))
                .environment(environment.name())
                .test(valueOf(scope == DeployScope.TEST))
                .pipelineId(valueOf(pipeline.getId()))
                .dlsPipelineId(pipeline.getDlsPipelineId());
    }
}