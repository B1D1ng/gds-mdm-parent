package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.contract.model.ContractStatus;
import com.ebay.behavior.gds.mdm.contract.model.HiveSource;
import com.ebay.behavior.gds.mdm.contract.model.Routing;
import com.ebay.behavior.gds.mdm.contract.model.Transformation;
import com.ebay.behavior.gds.mdm.contract.model.Transformer;
import com.ebay.behavior.gds.mdm.contract.model.UnstagedContract;
import com.ebay.behavior.gds.mdm.contract.model.manyToMany.ContractSignalMapping;
import com.ebay.behavior.gds.mdm.contract.repository.manyToMany.ContractSignalMappingRepository;
import com.ebay.behavior.gds.mdm.signal.common.model.Plan;
import com.ebay.behavior.gds.mdm.signal.common.model.PlatformLookup;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalDimValueLookup;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalPhysicalStorage;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalTypeLookup;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.service.DomainLookupService;
import com.ebay.behavior.gds.mdm.signal.service.PlanService;
import com.ebay.behavior.gds.mdm.signal.service.PlatformLookupService;
import com.ebay.behavior.gds.mdm.signal.service.SignalPhysicalStorageService;
import com.ebay.behavior.gds.mdm.signal.service.SignalTypeLookupService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedFieldService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedSignalService;

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
import java.util.Set;
import java.util.stream.Stream;

import static com.ebay.behavior.gds.mdm.common.model.Environment.PRODUCTION;
import static com.ebay.behavior.gds.mdm.common.model.Environment.STAGING;
import static com.ebay.behavior.gds.mdm.contract.model.ContractStatus.IN_DEVELOPMENT;
import static com.ebay.behavior.gds.mdm.contract.model.ContractStatus.STAGING_RELEASED;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.createHiveConfig;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.createHiveStorage;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.transformation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractAutoGeneratorServiceTest {

    @InjectMocks
    private ContractAutoGeneratorService contractAutoGeneratorService;

    @Mock
    private UnstagedSignalService signalService;

    @Mock
    private UnstagedFieldService fieldService;

    @Mock
    private SignalPhysicalStorageService physicalStorageService;

    @Mock
    private SignalTypeLookupService lookupService;

    @Mock
    private ContractSignalMappingRepository mappingRepository;

    @Mock
    private PlanService planService;

    @Mock
    private DomainLookupService domainService;

    @Mock
    private PlatformLookupService platformLookupService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private UnstagedContract createHiveContract(String name, String tableName, String donePath,
                                                Environment env, ContractStatus status,
                                                Set<Transformation> transformations) {
        val hiveStorage = createHiveStorage(tableName, donePath);
        val hiveConfig = createHiveConfig(env, hiveStorage);
        val hiveSource = HiveSource.builder().hiveConfigs(Set.of(hiveConfig)).build();

        val transformer = Transformer.builder().transformations(transformations).build();
        val routing = Routing.builder()
                .componentChain(List.of(hiveSource, transformer))
                .build();

        return UnstagedContract.builder()
                .id(123L)
                .version(1)
                .name(name)
                .description("Test contract for " + name)
                .status(status)
                .routings(Set.of(routing))
                .build();
    }

    // ===== Helper Methods for Mock Setup =====

    private void mockBatchDeploymentServices(UnstagedSignal signal, SignalTypeLookup lookup,
                                             SignalPhysicalStorage storage) {
        when(platformLookupService.findByName(anyString())).thenReturn(Optional.empty());
        when(platformLookupService.create(any())).thenReturn(PlatformLookup.builder().id(1L).name("HIVE").build());
        when(planService.getAllByName(anyString(), any())).thenReturn(org.springframework.data.domain.Page.empty());
        when(domainService.getDimensionTypeId()).thenReturn(1L);
        when(domainService.createIfAbsent(any())).thenReturn(SignalDimValueLookup.builder().id(1L).build());
        when(planService.create(any())).thenReturn(Plan.builder().id(1L).build());
        when(signalService.create(any(UnstagedSignal.class))).thenAnswer(invocation -> {
            UnstagedSignal arg = invocation.getArgument(0);
            arg.setId(signal.getId());
            arg.setVersion(signal.getVersion());
            return arg;
        });
        when(lookupService.create(any(SignalTypeLookup.class))).thenAnswer(invocation -> {
            SignalTypeLookup arg = invocation.getArgument(0);
            arg.setId(lookup.getId());
            return arg;
        });
        when(physicalStorageService.create(any(SignalPhysicalStorage.class))).thenAnswer(invocation -> {
            SignalPhysicalStorage arg = invocation.getArgument(0);
            arg.setId(storage.getId());
            return arg;
        });
    }

    private void mockBatchDeploymentForNewSignal(UnstagedSignal signal, SignalTypeLookup lookup,
                                                 SignalPhysicalStorage storage) {
        when(mappingRepository.findByContractIdAndContractVersion(123L, 1))
                .thenReturn(Optional.empty());
        mockBatchDeploymentServices(signal, lookup, storage);
    }

    // ===== Helper Methods for Verification =====
    private void verifyBatchDeploymentCreation(long lookupId, long storageId) {
        verify(signalService).create(any(UnstagedSignal.class));
        verify(lookupService).create(any(SignalTypeLookup.class));
        verify(physicalStorageService).create(any(SignalPhysicalStorage.class));
        verify(lookupService).createPhysicalStorageMapping(lookupId, storageId);
        verify(fieldService).create(any(), eq(null));
    }

    private void verifyBatchDeploymentUpdate() {
        verify(signalService).update(any(UnstagedSignal.class));
        verify(lookupService, never()).update(any(SignalTypeLookup.class));
        verify(physicalStorageService).update(any(SignalPhysicalStorage.class));
        verify(lookupService, never()).createPhysicalStorageMapping(anyLong(), anyLong());
        verify(fieldService).update(any(UnstagedField.class));
    }

    @Nested
    class NewSignalCreationTests {
        @ParameterizedTest
        @MethodSource("com.ebay.behavior.gds.mdm.contract.service.ContractAutoGeneratorServiceTest#newBatchDeploymentScenarios")
        void upsertUnstagedSignalsForContract_newSignal_createsSignalAndMappings(
                Environment env,
                ContractStatus initialStatus,
                String contractName,
                String tableName,
                String donePath,
                long signalId,
                long lookupId,
                long storageId) {

            val transformation = transformation("test_field", "Test field", "$event.test");
            val contract = createHiveContract(contractName, tableName, donePath, env, initialStatus,
                    Set.of(transformation));

            val newSignal = UnstagedSignal.builder().id(signalId).version(1).build();
            val newLookup = SignalTypeLookup.builder().id(lookupId)
                    .name("CONTRACT_AUTO_GEN_" + contractName).build();
            val newPhysicalStorage = SignalPhysicalStorage.builder().id(storageId).build();

            mockBatchDeploymentForNewSignal(newSignal, newLookup, newPhysicalStorage);

            val result = contractAutoGeneratorService.upsertUnstagedSignalsForContract(contract, env);

            verifyBatchDeploymentCreation(lookupId, storageId);
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(signalId);
            assertThat(result.getVersion()).isEqualTo(1);
        }
    }

    static Stream<Arguments> newBatchDeploymentScenarios() {
        return Stream.of(
                Arguments.of(STAGING, IN_DEVELOPMENT, "test_contract",
                        "test_table", "/path/to/done", 789L, 456L, 321L),
                Arguments.of(PRODUCTION, STAGING_RELEASED, "prod_contract",
                        "prod_table", "/prod/path/to/done", 888L, 555L, 444L)
        );
    }

    @Test
    void upsertUnstagedSignalsForContract_existingSignal_updatesSignalAndMappings() {
        val transformation = transformation("updated_field", "Updated field", "$event.updated");
        val contract = createHiveContract("updated_contract", "updated_table", "/updated/path",
                STAGING, IN_DEVELOPMENT, Set.of(transformation));

        val existingField1 = UnstagedField.builder().id(100L).name("updated_field").build();

        val existingSignal = UnstagedSignal.builder()
                .id(999L)
                .version(1)
                .type("CONTRACT_AUTO_GEN_updated_contract")
                .fields(Set.of(existingField1))
                .build();

        val existingPhysicalStorage = SignalPhysicalStorage.builder()
                .id(666L)
                .environment(STAGING)
                .hiveTableName("old_table")
                .doneFilePath("/old/path")
                .build();

        val existingLookup = SignalTypeLookup.builder()
                .id(777L)
                .name("CONTRACT_AUTO_GEN_updated_contract")
                .physicalStorages(Set.of(existingPhysicalStorage))
                .build();

        val existingMapping = ContractSignalMapping.builder()
                .contract(contract)
                .signal(existingSignal)
                .build();

        when(platformLookupService.findByName(anyString())).thenReturn(Optional.of(PlatformLookup.builder().id(1L).name("HIVE").build()));
        when(planService.getAllByName(anyString(), any())).thenReturn(org.springframework.data.domain.Page.empty());
        when(domainService.getDimensionTypeId()).thenReturn(1L);
        when(domainService.createIfAbsent(any())).thenReturn(SignalDimValueLookup.builder().id(1L).build());
        when(planService.create(any())).thenReturn(Plan.builder().id(1L).build());
        when(mappingRepository.findByContractIdAndContractVersion(123L, 1))
                .thenReturn(Optional.of(existingMapping));
        when(lookupService.findByName("CONTRACT_AUTO_GEN_updated_contract"))
                .thenReturn(Optional.of(existingLookup));

        val result = contractAutoGeneratorService.upsertUnstagedSignalsForContract(contract, STAGING);

        verifyBatchDeploymentUpdate();
        verify(signalService, never()).create(any());
        verify(fieldService, never()).delete(any());
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(999L);
    }

    @Test
    void upsertUnstagedSignalsForContract_existingSignalWithPhysicalStorage_updatesPhysicalStorage() {
        val transformation = transformation("field1", "Field 1", "$event.field1");
        val contract = createHiveContract("contract_with_storage", "modified_table", "/modified/path",
                STAGING, IN_DEVELOPMENT, Set.of(transformation));

        val existingField = UnstagedField.builder().id(100L).name("field1").build();

        val existingSignal = UnstagedSignal.builder()
                .id(101L)
                .version(1)
                .type("CONTRACT_AUTO_GEN_contract_with_storage")
                .fields(Set.of(existingField))
                .build();

        val existingPhysicalStorage = SignalPhysicalStorage.builder()
                .id(202L)
                .environment(STAGING)
                .hiveTableName("old_table")
                .doneFilePath("/old/path")
                .build();

        val existingLookup = SignalTypeLookup.builder()
                .id(303L)
                .name("CONTRACT_AUTO_GEN_contract_with_storage")
                .physicalStorages(Set.of(existingPhysicalStorage))
                .build();

        val existingMapping = ContractSignalMapping.builder()
                .contract(contract)
                .signal(existingSignal)
                .build();

        when(platformLookupService.findByName(anyString())).thenReturn(Optional.of(PlatformLookup.builder().id(1L).name("HIVE").build()));
        when(planService.getAllByName(anyString(), any())).thenReturn(org.springframework.data.domain.Page.empty());
        when(domainService.getDimensionTypeId()).thenReturn(1L);
        when(domainService.createIfAbsent(any())).thenReturn(SignalDimValueLookup.builder().id(1L).build());
        when(planService.create(any())).thenReturn(Plan.builder().id(1L).build());
        when(mappingRepository.findByContractIdAndContractVersion(123L, 1))
                .thenReturn(Optional.of(existingMapping));
        when(lookupService.findByName("CONTRACT_AUTO_GEN_contract_with_storage"))
                .thenReturn(Optional.of(existingLookup));

        val result = contractAutoGeneratorService.upsertUnstagedSignalsForContract(contract, STAGING);

        verify(signalService).update(existingSignal);
        verify(lookupService, never()).update(any());
        verify(physicalStorageService).update(existingPhysicalStorage);
        verify(physicalStorageService, never()).create(any());
        verify(lookupService, never()).createPhysicalStorageMapping(anyLong(), anyLong());
        assertThat(existingPhysicalStorage.getHiveTableName()).isEqualTo("modified_table");
        assertThat(existingPhysicalStorage.getDoneFilePath()).isEqualTo("/modified/path");
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(101L);
    }

    @Test
    void upsertUnstagedSignalsForContract_noTransformations_doesNotUpsertFields() {
        val contract = createHiveContract("no_transform_contract", "test_table", "/path/to/done",
                STAGING, IN_DEVELOPMENT, Set.of()); // Empty transformations

        val newSignal = UnstagedSignal.builder().id(111L).version(1).build();
        val newLookup = SignalTypeLookup.builder().id(222L)
                .name("CONTRACT_AUTO_GEN_no_transform_contract").build();
        val newPhysicalStorage = SignalPhysicalStorage.builder().id(333L).build();

        mockBatchDeploymentForNewSignal(newSignal, newLookup, newPhysicalStorage);

        val result = contractAutoGeneratorService.upsertUnstagedSignalsForContract(contract, STAGING);

        verify(signalService).create(any(UnstagedSignal.class));
        verify(fieldService, never()).create(any(), any());
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(111L);
    }
}
