package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.contract.model.ContractPipeline;
import com.ebay.behavior.gds.mdm.contract.model.DeployScope;
import com.ebay.behavior.gds.mdm.contract.model.UpdatePipelineRequest;
import com.ebay.behavior.gds.mdm.contract.repository.ContractPipelineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomString;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.unstagedContract;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

class ContractPipelineServiceTest {

    @InjectMocks
    private ContractPipelineService service;

    @Mock
    private ContractPipelineRepository repository;

    private ContractPipeline contractPipeline;

    private static final Long TEST_ID = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        var createdContract = unstagedContract(getRandomString()).toBuilder().id(TEST_ID).version(1).build();
        contractPipeline = ContractPipeline.builder()
                .contractId(createdContract.getId())
                .contractVersion(createdContract.getVersion())
                .environment(Environment.UNSTAGED)
                .deployScope(DeployScope.TEST)
                .build();
    }

    @Test
    void test_getById_succeed() {
        var created = contractPipeline.toBuilder().id(TEST_ID).build();
        when(repository.findById(TEST_ID)).thenReturn(Optional.of(created));

        var result = service.getById(TEST_ID);
        assertThat(result).isEqualTo(created);
        verify(repository).findById(TEST_ID);
    }

    @Test
    void test_getById_notFound() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(TEST_ID)).isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("ContractPipeline id=%d doesn't found".formatted(TEST_ID));

        verify(repository).findById(TEST_ID);
    }

    @Test
    void test_update_notFound() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.empty());

        var updateRequest = UpdatePipelineRequest.builder().id(TEST_ID).build();

        assertThatThrownBy(() -> service.update(updateRequest)).isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("ContractPipeline id=%d doesn't found".formatted(TEST_ID));

        verify(repository).findById(TEST_ID);
        verify(repository, never()).save(any());
    }

    @Test
    void test_update_succeed() {
        var existingPipeline = contractPipeline.toBuilder().id(TEST_ID).build();
        when(repository.findById(TEST_ID)).thenReturn(Optional.of(existingPipeline));

        var updateRequest = UpdatePipelineRequest.builder()
                .id(TEST_ID)
                .environment(Environment.STAGING)
                .deployScope(DeployScope.TEST)
                .build();

        var updatedPipeline = existingPipeline.toBuilder()
                .environment(updateRequest.getEnvironment())
                .deployScope(updateRequest.getDeployScope())
                .build();

        when(repository.save(any(ContractPipeline.class))).thenReturn(updatedPipeline);

        var result = service.update(updateRequest);

        assertThat(result.getEnvironment()).isEqualTo(updateRequest.getEnvironment());
        assertThat(result.getDeployScope()).isEqualTo(updateRequest.getDeployScope());

        verify(repository).findById(TEST_ID);
        verify(repository).save(any(ContractPipeline.class));
    }
}