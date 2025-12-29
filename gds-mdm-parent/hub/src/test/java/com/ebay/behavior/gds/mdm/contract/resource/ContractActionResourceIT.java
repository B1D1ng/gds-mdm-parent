package com.ebay.behavior.gds.mdm.contract.resource;

import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.contract.client.ControlPlaneManagerClient;
import com.ebay.behavior.gds.mdm.contract.model.ContractPipeline;
import com.ebay.behavior.gds.mdm.contract.model.DeployContractRequest;
import com.ebay.behavior.gds.mdm.contract.model.DeployScope;
import com.ebay.behavior.gds.mdm.contract.model.DoneFileType;
import com.ebay.behavior.gds.mdm.contract.model.HiveConfig;
import com.ebay.behavior.gds.mdm.contract.model.HiveSource;
import com.ebay.behavior.gds.mdm.contract.model.HiveStorage;
import com.ebay.behavior.gds.mdm.contract.model.Routing;
import com.ebay.behavior.gds.mdm.contract.model.UnstagedContract;
import com.ebay.behavior.gds.mdm.contract.model.client.ControlPlaneResponse;
import com.ebay.behavior.gds.mdm.contract.model.exception.ControlPlaneManagerException;
import com.ebay.behavior.gds.mdm.contract.repository.manyToMany.ContractSignalMappingRepository;
import com.ebay.behavior.gds.mdm.contract.service.ContractPipelineService;
import com.ebay.behavior.gds.mdm.contract.service.HiveConfigService;
import com.ebay.behavior.gds.mdm.contract.service.HiveSourceService;
import com.ebay.behavior.gds.mdm.contract.service.HiveStorageService;
import com.ebay.behavior.gds.mdm.contract.service.KafkaSourceService;
import com.ebay.behavior.gds.mdm.contract.service.RoutingService;
import com.ebay.behavior.gds.mdm.contract.service.TransformationService;
import com.ebay.behavior.gds.mdm.contract.service.TransformerService;
import com.ebay.behavior.gds.mdm.contract.service.UnstagedContractService;
import com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.SignalTypePhysicalStorageMappingRepository;
import com.ebay.behavior.gds.mdm.signal.service.SignalPhysicalStorageService;
import com.ebay.behavior.gds.mdm.signal.service.SignalTypeLookupService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedSignalService;

import com.google.common.collect.Lists;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.ebay.behavior.gds.mdm.common.model.Environment.PRODUCTION;
import static com.ebay.behavior.gds.mdm.common.model.Environment.STAGING;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomString;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.contract.model.ContractStatus.*;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.transformation;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.kafkaSource;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.routing;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.unstagedContract;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.LCM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration tests for ContractActionResource.
 * Tests contract lifecycle management actions including update, test, deploy, and archive.
 */
class ContractActionResourceIT extends AbstractResourceTest {

    @Autowired
    private UnstagedContractService unstagedContractService;

    @Autowired
    private ContractPipelineService contractPipelineService;

    @Autowired
    private SignalPhysicalStorageService physicalStorageService;

    @Autowired
    private SignalTypePhysicalStorageMappingRepository physicalStorageMappingRepository;

    @Autowired
    private SignalTypeLookupService lookupService;

    @Autowired
    private ContractSignalMappingRepository mappingRepository;

    @Autowired
    private HiveStorageService hiveStorageService;

    @Autowired
    private HiveConfigService hiveConfigService;

    @Autowired
    private HiveSourceService hiveSourceService;

    @Autowired
    private RoutingService routingService;

    @Autowired
    private TransformerService transformerService;

    @Autowired
    private TransformationService transformationService;

    @Autowired
    private UnstagedSignalService signalService;

    @Autowired
    private KafkaSourceService kafkaSourceService;

    @MockitoBean
    private ControlPlaneManagerClient controlPlaneManagerClient;

    private UnstagedContract streamingContract;
    private UnstagedContract batchContract;
    private DeployContractRequest deployRequest;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = getBaseUrl() + V1 + LCM + "/contract";

        // Create a streaming contract (Kafka-based)
        streamingContract = createStreamingContract();

        // Create a batch contract (Hive-based)
        batchContract = createBatchContract();

        // Mock control plane responses
        ControlPlaneResponse response = new ControlPlaneResponse();
        response.setId(1L);
        when(controlPlaneManagerClient.requestWorkflow(any()))
                .thenReturn(response);
    }

    private UnstagedContract createStreamingContract() {
        val contract = unstagedContract("streaming_contract_" + getRandomString())
                .setStatus(IN_DEVELOPMENT)
                .setEnvironment(Environment.UNSTAGED);

        unstagedContractService.create(contract);

        val routing = routing("test_routing_" + getRandomString())
                .setContractId(contract.getId())
                .setContractVersion(contract.getVersion());

        routingService.create(routing);
        val kafkaSource = kafkaSource("test_kafka_source_" + getRandomString());
        kafkaSourceService.create(kafkaSource);

        routingService.updateComponentsMapping(routing.getId(), Lists.newArrayList(kafkaSource.getId()));

        return contract;
    }

    private UnstagedContract createBatchContract() {
        // 1. Create and save HiveStorage first
        val stagingHiveStorage = HiveStorage.builder()
                .tableName("test_table_" + getRandomString())
                .doneFilePath("/path/to/done_" + getRandomString())
                .dbName("test_db")
                .dataCenter(Lists.newArrayList("apollo"))
                .format("parquet")
                .doneFileType(DoneFileType.FILE)
                .build();
        hiveStorageService.create(stagingHiveStorage);

        val prodHiveStorage = HiveStorage.builder()
                .tableName("prod_table_" + getRandomString())
                .doneFilePath("/path/to/done_" + getRandomString())
                .dbName("prod_db")
                .dataCenter(Lists.newArrayList("apollo"))
                .format("parquet")
                .doneFileType(DoneFileType.FILE)
                .build();
        hiveStorageService.create(prodHiveStorage);

        // 2. Create and save HiveSource (must be saved before HiveConfig due to componentId dependency)
        val hiveSource = HiveSource.builder()
                .name("test_hive_source_" + getRandomString())
                .dl("dl")
                .description("Test hive source")
                .owners("test@ebay.com")
                .entityType("item")
                .type("HiveSource")
                .build();
        hiveSourceService.create(hiveSource);

        // 3. Create and save HiveConfig with componentId from saved HiveSource
        val stagingHiveConfig = HiveConfig.builder()
                .env(STAGING)
                .componentId(hiveSource.getId())
                .build();
        hiveConfigService.create(stagingHiveConfig);

        val prodHiveConfig = HiveConfig.builder()
                .env(PRODUCTION)
                .componentId(hiveSource.getId())
                .build();
        hiveConfigService.create(prodHiveConfig);

        // 4. Update the config-storage mapping
        hiveConfigService.updateMapping(stagingHiveConfig.getId(), stagingHiveStorage.getId());
        hiveConfigService.updateMapping(prodHiveConfig.getId(), prodHiveStorage.getId());

        val transformer = TestModelUtils.transformer("test_transformer_" + getRandomString());
        transformerService.create(transformer);

        val transformation = transformation("test_field", "Test field description", "$event.test")
                .setComponentId(transformer.getId());

        transformationService.create(transformation);

        val contract = UnstagedContract.builder()
                .name("batch_contract_" + getRandomString())
                .dl("dl")
                .environment(Environment.UNSTAGED)
                .domain("domain")
                .description("Test batch contract")
                .status(IN_DEVELOPMENT)
                .owners("test@ebay.com")
                .build();

        unstagedContractService.create(contract);

        val routing = Routing.builder()
                .name("test_batch_routing_" + getRandomString())
                .contractId(contract.getId())
                .contractVersion(contract.getVersion())
                .build();

        routingService.create(routing);

        routingService.updateComponentsMapping(routing.getId(), Lists.newArrayList(hiveSource.getId(), transformer.getId()));

        return contract;
    }

    @Nested
    class UpdateActionTests {
        @Test
        void update_withValidContract_updatesContractStatus() {
            val contract = streamingContract;

            val response = requestSpec()
                    .when().put(baseUrl + "/" + contract.getId() + "/action/update")
                    .then().statusCode(HttpStatus.OK.value())
                    .extract().body().jsonPath().getObject(".", UnstagedContract.class);

            assertThat(response.getStatus()).isEqualTo(IN_DEVELOPMENT);
            assertThat(response.getId()).isEqualTo(contract.getId());
        }

        @Test
        void update_withInvalidId_returnsExpectationFailed() {
            requestSpec()
                    .when().put(baseUrl + "/999999999/action/update")
                    .then().statusCode(HttpStatus.EXPECTATION_FAILED.value());
        }
    }

    @Nested
    class TestActionTests {
        @Test
        void test_withStreamingContract_deploysToTestEnvironment() {
            val contract = streamingContract;
            contract.setStatus(ONBOARDING);
            unstagedContractService.update(contract);

            deployRequest = DeployContractRequest.builder()
                    .id(contract.getId())
                    .version(contract.getVersion())
                    .appName("test-app")
                    .namespace("test-namespace")
                    .build();

            val response = requestSpecWithBody(deployRequest)
                    .when().put(baseUrl + "/" + contract.getId() + "/action/test")
                    .then().statusCode(HttpStatus.OK.value())
                    .extract().body().jsonPath().getObject(".", UnstagedContract.class);

            assertThat(response.getStatus()).isEqualTo(DEPLOYING_TEST);
            assertThat(response.getId()).isEqualTo(contract.getId());
        }

        @Test
        void test_withMismatchedId_returnsBadRequest() {
            val contract = streamingContract;

            deployRequest = DeployContractRequest.builder()
                    .id(999999L)  // Different ID
                    .version(contract.getVersion())
                    .appName("test-app")
                    .namespace("test-namespace")
                    .build();

            requestSpecWithBody(deployRequest)
                    .when().put(baseUrl + "/" + contract.getId() + "/action/test")
                    .then().statusCode(HttpStatus.BAD_REQUEST.value());
        }
    }

    @Nested
    class CompleteTestActionTests {
        @Test
        void completeTest_withTestingContract_stopsTestDeployment() {
            val contract = streamingContract;
            contract.setStatus(TESTING);
            unstagedContractService.update(contract);

            // Create a test pipeline
            val pipeline = ContractPipeline.builder()
                    .contractId(contract.getId())
                    .contractVersion(contract.getVersion())
                    .environment(STAGING)
                    .deployScope(DeployScope.TEST)
                    .dlsPipelineId("test-pipeline-123")
                    .build();
            contractPipelineService.create(pipeline);

            val response = requestSpec()
                    .when().put(baseUrl + "/" + contract.getId() + "/action/complete-test")
                    .then().statusCode(HttpStatus.OK.value())
                    .extract().body().jsonPath().getObject(".", UnstagedContract.class);

            assertThat(response.getStatus()).isEqualTo(STOPPING_TEST);
            assertThat(response.getId()).isEqualTo(contract.getId());
        }
    }

    @Nested
    class DeployStagingActionTests {
        @Test
        void deployStaging_withStreamingContract_deploysToStagingEnvironment() {
            val contract = streamingContract;
            contract.setStatus(TESTING_COMPLETE);
            unstagedContractService.update(contract);

            deployRequest = DeployContractRequest.builder()
                    .id(contract.getId())
                    .version(contract.getVersion())
                    .appName("staging-app")
                    .namespace("staging-namespace")
                    .build();

            val response = requestSpecWithBody(deployRequest)
                    .when().put(baseUrl + "/" + contract.getId() + "/action/deploy-staging")
                    .then().statusCode(HttpStatus.OK.value())
                    .extract().body().jsonPath().getObject(".", UnstagedContract.class);

            assertThat(response.getStatus()).isEqualTo(DEPLOYING_STAGING);
            assertThat(response.getId()).isEqualTo(contract.getId());

            // Verify pipeline was created
            val pipelineOpt = contractPipelineService.findPipelineByContract(
                    contract.getId(), contract.getVersion(), STAGING);
            assertThat(pipelineOpt).isPresent();
            assertThat(pipelineOpt.get().getDeployScope()).isEqualTo(DeployScope.RELEASE);
        }

        @Test
        void deployStaging_withBatchContract_createsSignalAndDeploysToStaging() {
            val contract = batchContract;
            contract.setStatus(TESTING_COMPLETE);
            unstagedContractService.update(contract);

            deployRequest = DeployContractRequest.builder()
                    .id(contract.getId())
                    .version(contract.getVersion())
                    .build();

            requestSpecWithBody(deployRequest)
                    .when().put(baseUrl + "/" + contract.getId() + "/action/deploy-staging")
                    .then().statusCode(HttpStatus.OK.value());

            // Verify complete signal hierarchy
            val mappingOpt = mappingRepository.findByContractIdAndContractVersion(
                    contract.getId(), contract.getVersion());
            assertThat(mappingOpt).isPresent();

            val signal = mappingOpt.get().getSignal();
            assertThat(signal).isNotNull();
            assertThat(signal.getName()).contains("CONTRACT_AUTO_GEN_");
            assertThat(signal.getType()).contains("CONTRACT_AUTO_GEN_");
            assertThat(signalService.getFields(signal.getSignalId())).isNotEmpty();

            // Verify signal type lookup was created
            val lookupOpt = lookupService.findByName(signal.getType());
            assertThat(lookupOpt).isPresent();

            // Verify physical storage was created
            val lookup = lookupOpt.get();
            val storages = physicalStorageMappingRepository.findBySignalTypeId(lookup.getId());
            assertThat(storages).isNotEmpty();

            val stagingStorage = storages.stream().map(st -> physicalStorageService.getById(st.getPhysicalStorageId()))
                    .filter(ps -> ps.getEnvironment() == STAGING)
                    .findFirst();
            assertThat(stagingStorage).isPresent();
        }

        @Test
        void deployStaging_withExistingPipeline_updatesPipeline() {
            val contract = streamingContract;
            contract.setStatus(TESTING_COMPLETE);
            unstagedContractService.update(contract);

            // Create an existing pipeline with TEST scope
            val existingPipeline = ContractPipeline.builder()
                    .contractId(contract.getId())
                    .contractVersion(contract.getVersion())
                    .environment(STAGING)
                    .deployScope(DeployScope.TEST)
                    .dlsPipelineId("existing-pipeline-123")
                    .build();
            contractPipelineService.create(existingPipeline);

            deployRequest = DeployContractRequest.builder()
                    .id(contract.getId())
                    .version(contract.getVersion())
                    .appName("staging-app")
                    .namespace("staging-namespace")
                    .build();

            requestSpecWithBody(deployRequest)
                    .when().put(baseUrl + "/" + contract.getId() + "/action/deploy-staging")
                    .then().statusCode(HttpStatus.OK.value());

            // Verify pipeline scope was updated to RELEASE
            val updatedPipeline = contractPipelineService.findPipelineByContract(
                    contract.getId(), contract.getVersion(), STAGING);
            assertThat(updatedPipeline).isPresent();
            assertThat(updatedPipeline.get().getDeployScope()).isEqualTo(DeployScope.RELEASE);
        }

        @Test
        void deployStaging_withMismatchedId_returnsBadRequest() {
            val contract = streamingContract;

            deployRequest = DeployContractRequest.builder()
                    .id(999999L)  // Different ID
                    .version(contract.getVersion())
                    .appName("staging-app")
                    .namespace("staging-namespace")
                    .build();

            requestSpecWithBody(deployRequest)
                    .when().put(baseUrl + "/" + contract.getId() + "/action/deploy-staging")
                    .then().statusCode(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void deployStaging_withControlPlaneFailure_marksDeployFailed() {
            val contract = streamingContract;
            contract.setStatus(TESTING_COMPLETE);
            unstagedContractService.update(contract);

            // Mock control plane failure
            Mockito.reset(controlPlaneManagerClient);
            when(controlPlaneManagerClient.requestWorkflow(any()))
                    .thenThrow(new ControlPlaneManagerException("Workflow failed"));

            deployRequest = DeployContractRequest.builder()
                    .id(contract.getId())
                    .version(contract.getVersion())
                    .appName("staging-app")
                    .namespace("staging-namespace")
                    .build();

            requestSpecWithBody(deployRequest)
                    .when().put(baseUrl + "/" + contract.getId() + "/action/deploy-staging")
                    .then().statusCode(HttpStatus.OK.value());

            // Verify contract status was updated to failed
            val updatedContract = unstagedContractService.getByIdAndLatestVersion(contract.getId());
            assertThat(updatedContract.getStatus()).isEqualTo(DEPLOY_STAGING_FAILED);
        }
    }

    @Nested
    class DeployProductionActionTests {
        @Test
        void deployProduction_withStreamingContract_deploysToProductionEnvironment() {
            val contract = streamingContract;
            contract.setStatus(STAGING_RELEASED);
            unstagedContractService.update(contract);

            deployRequest = DeployContractRequest.builder()
                    .id(contract.getId())
                    .version(contract.getVersion())
                    .appName("prod-app")
                    .namespace("prod-namespace")
                    .build();

            val response = requestSpecWithBody(deployRequest)
                    .when().put(baseUrl + "/" + contract.getId() + "/action/deploy-production")
                    .then().statusCode(HttpStatus.OK.value())
                    .extract().body().jsonPath().getObject(".", UnstagedContract.class);

            assertThat(response.getStatus()).isEqualTo(DEPLOYING_PRODUCTION);
            assertThat(response.getId()).isEqualTo(contract.getId());

            // Verify pipeline was created
            val pipelineOpt = contractPipelineService.findPipelineByContract(
                    contract.getId(), contract.getVersion(), PRODUCTION);
            assertThat(pipelineOpt).isPresent();
            assertThat(pipelineOpt.get().getDeployScope()).isEqualTo(DeployScope.RELEASE);
        }

        @Test
        void deployProduction_withBatchContract_createsSignalAndDeploysToProduction() {
            val contract = batchContract;
            deployRequest = DeployContractRequest.builder()
                    .id(contract.getId())
                    .version(contract.getVersion())
                    .build();
            // First deploy to staging
            contract.setStatus(STAGING_RELEASED);
            unstagedContractService.update(contract);

            deployRequest = DeployContractRequest.builder()
                    .id(contract.getId())
                    .version(contract.getVersion())
                    .build();

            requestSpecWithBody(deployRequest)
                    .when().put(baseUrl + "/" + contract.getId() + "/action/deploy-production")
                    .then().statusCode(HttpStatus.OK.value());

            // Verify physical storage exists for both environments
            val mappingOpt = mappingRepository.findByContractIdAndContractVersion(
                    contract.getId(), contract.getVersion());
            assertThat(mappingOpt).isPresent();

            val signal = mappingOpt.get().getSignal();
            val lookupOpt = lookupService.findByName(signal.getType());
            assertThat(lookupOpt).isPresent();

            val lookup = lookupOpt.get();
            val storages = physicalStorageMappingRepository.findBySignalTypeId(lookup.getId());
            assertThat(storages).isNotEmpty();

            val storage = storages.stream().map(st -> physicalStorageService.getById(st.getPhysicalStorageId()))
                    .filter(ps -> ps.getEnvironment() == PRODUCTION)
                    .findFirst();
            assertThat(storage).isPresent();
        }

        @Test
        void deployProduction_withMismatchedId_returnsBadRequest() {
            val contract = streamingContract;

            deployRequest = DeployContractRequest.builder()
                    .id(999999L)  // Different ID
                    .version(contract.getVersion())
                    .appName("prod-app")
                    .namespace("prod-namespace")
                    .build();

            requestSpecWithBody(deployRequest)
                    .when().put(baseUrl + "/" + contract.getId() + "/action/deploy-production")
                    .then().statusCode(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void deployProduction_withControlPlaneFailure_marksDeployFailed() {
            val contract = streamingContract;
            contract.setStatus(STAGING_RELEASED);
            unstagedContractService.update(contract);

            // Mock control plane failure
            Mockito.reset(controlPlaneManagerClient);
            when(controlPlaneManagerClient.requestWorkflow(any()))
                    .thenThrow(new ControlPlaneManagerException("Workflow failed"));

            deployRequest = DeployContractRequest.builder()
                    .id(contract.getId())
                    .version(contract.getVersion())
                    .appName("prod-app")
                    .namespace("prod-namespace")
                    .build();

            requestSpecWithBody(deployRequest)
                    .when().put(baseUrl + "/" + contract.getId() + "/action/deploy-production")
                    .then().statusCode(HttpStatus.OK.value());

            // Verify contract status was updated to failed
            val updatedContract = unstagedContractService.getByIdAndLatestVersion(contract.getId());
            assertThat(updatedContract.getStatus()).isEqualTo(DEPLOY_PRODUCTION_FAILED);
        }
    }

    @Nested
    class ArchiveActionTests {
        @Test
        void archive_withDeployedContract_archivesContract() {
            val contract = streamingContract;
            contract.setStatus(RELEASED);
            unstagedContractService.update(contract);

            // Create pipelines for both environments
            val stagingPipeline = ContractPipeline.builder()
                    .contractId(contract.getId())
                    .contractVersion(contract.getVersion())
                    .environment(STAGING)
                    .deployScope(DeployScope.RELEASE)
                    .dlsPipelineId("staging-pipeline-123")
                    .build();
            contractPipelineService.create(stagingPipeline);

            val prodPipeline = ContractPipeline.builder()
                    .contractId(contract.getId())
                    .contractVersion(contract.getVersion())
                    .environment(PRODUCTION)
                    .deployScope(DeployScope.RELEASE)
                    .dlsPipelineId("prod-pipeline-123")
                    .build();
            contractPipelineService.create(prodPipeline);

            val response = requestSpec()
                    .when().put(baseUrl + "/" + contract.getId() + "/action/archive")
                    .then().statusCode(HttpStatus.OK.value())
                    .extract().body().jsonPath().getObject(".", UnstagedContract.class);

            assertThat(response.getStatus()).isEqualTo(ARCHIVING);
            assertThat(response.getId()).isEqualTo(contract.getId());
        }

        @Test
        void archive_withInvalidId_returnsExpectationFailed() {
            requestSpec()
                    .when().put(baseUrl + "/999999999/action/archive")
                    .then().statusCode(HttpStatus.EXPECTATION_FAILED.value());
        }
    }
}
