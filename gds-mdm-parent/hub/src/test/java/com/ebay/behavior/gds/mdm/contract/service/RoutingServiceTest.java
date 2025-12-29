package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.contract.model.ContractStatus;
import com.ebay.behavior.gds.mdm.contract.model.Routing;
import com.ebay.behavior.gds.mdm.contract.model.manyToMany.RoutingComponentMapping;
import com.ebay.behavior.gds.mdm.contract.repository.RoutingRepository;
import com.ebay.behavior.gds.mdm.contract.repository.UnstagedContractRepository;
import com.ebay.behavior.gds.mdm.contract.repository.manyToMany.RoutingComponentMappingRepository;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomString;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.filter;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.kafkaSink;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.kafkaSource;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.routing;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.streamingConfig;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.transformation;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.transformer;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.unstagedContract;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoutingServiceTest {

    @InjectMocks
    private RoutingService service;

    @Mock
    private RoutingRepository repository;
    @Mock
    private UnstagedContractRepository contractRepository;
    @Mock
    private RoutingComponentMappingRepository mappingRepository;

    private Routing routing;

    private static final Long TEST_ID = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        routing = routing(getRandomString()).toBuilder().id(TEST_ID).revision(0).build();
    }

    @Test
    void create_withoutSla() {
        val newRouting = routing(getRandomString());
        val savedNewRouting = newRouting.toBuilder().id(TEST_ID).revision(0).build();

        when(repository.save(newRouting)).thenReturn(savedNewRouting);

        val result = service.create(newRouting);

        assertThat(result).isEqualTo(savedNewRouting);
        assertThat(result.getId()).isEqualTo(TEST_ID);
        verify(repository).save(newRouting);
    }

    @Test
    void getById_exists_withoutAssociations() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.of(routing));

        val result = service.getById(TEST_ID);

        assertThat(result).isEqualTo(routing);
        verify(repository).findById(TEST_ID);
    }

    @Test
    void getById_nonExists_throwsException() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(TEST_ID))
                .isInstanceOf(DataNotFoundException.class);

        verify(repository).findById(TEST_ID);
    }

    @Test
    void getById_exists_withAssociations_complete() {
        val streamConfig = streamingConfig(getRandomString()).toBuilder().id(9L).build();
        val kafkaSource = kafkaSource(getRandomString()).toBuilder().id(10L).streamingConfigs(Set.of(streamConfig)).build();
        val kafkaSink = kafkaSink(getRandomString()).toBuilder().id(11L).streamingConfigs(Set.of(streamConfig)).build();
        val transformation = transformation("test field", "description", "expression").toBuilder().id(12L).build();
        val filter = filter(getRandomString()).toBuilder().id(13L).build();
        val transformer = transformer(getRandomString()).toBuilder()
                .transformations(Set.of(transformation))
                .filters(Set.of(filter)).build();
        val savedRouting = routing.toBuilder().componentChain(List.of(kafkaSource, transformer, kafkaSink)).build();

        when(repository.findById(TEST_ID)).thenReturn(Optional.of(savedRouting));

        val result = service.getByIdWithAssociations(TEST_ID, true);

        assertThat(result).isEqualTo(savedRouting);
        verify(repository).findById(TEST_ID);
    }

    @Test
    void test_update_withoutSla() {
        val updatedName = getRandomString();
        val updateRequest = routing.toBuilder().name(updatedName).revision(1).build();

        when(repository.findById(TEST_ID)).thenReturn(Optional.of(routing));
        when(repository.save(updateRequest)).thenReturn(updateRequest);

        val result = service.update(updateRequest);
        assertThat(result.getName()).isEqualTo(updatedName);
        verify(repository).findById(TEST_ID);
        verify(repository).save(updateRequest);
    }

    @Test
    void delete_existed_in_development() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.of(routing));
        val unstagedContract = unstagedContract("TestContract")
                .toBuilder()
                .id(1L)
                .version(1)
                .status(ContractStatus.IN_DEVELOPMENT)
                .build();
        when(contractRepository.findById(VersionedId.of(TEST_ID, 1))).thenReturn(Optional.of(unstagedContract));

        service.delete(TEST_ID);
        verify(repository).deleteById(TEST_ID);
    }

    @Test
    void delete_existed_in_testing() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.of(routing));
        val unstagedContract = unstagedContract("TestContract")
                .toBuilder()
                .id(1L)
                .version(1)
                .status(ContractStatus.TESTING)
                .build();
        when(contractRepository.findById(VersionedId.of(TEST_ID, 1))).thenReturn(Optional.of(unstagedContract));

        assertThatThrownBy(() -> service.delete(TEST_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot delete routing in non-development contract");

        verify(repository, never()).deleteById(anyLong());
    }

    @Test
    void delete_notFound() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.empty());
        val unstagedContract = unstagedContract("TestContract")
                .toBuilder()
                .id(1L)
                .version(1)
                .status(ContractStatus.IN_DEVELOPMENT)
                .build();
        when(contractRepository.findById(VersionedId.of(TEST_ID, 1))).thenReturn(Optional.of(unstagedContract));

        assertThatThrownBy(() -> service.delete(TEST_ID))
                .isInstanceOf(DataNotFoundException.class);
        verify(repository, never()).deleteById(anyLong());
    }

    @Test
    void updateComponentsMapping_withValidComponentIds_shouldCreateMappingsInOrder() {
        // Given
        List<Long> componentIds = List.of(10L, 11L, 12L);
        when(repository.findById(TEST_ID)).thenReturn(Optional.of(routing));

        // When
        service.updateComponentsMapping(TEST_ID, componentIds);

        // Then
        verify(repository).findById(TEST_ID);
        verify(mappingRepository).deleteAllByRoutingId(TEST_ID);
        verify(mappingRepository).saveAll(argThat(mappings -> {
            List<RoutingComponentMapping> mappingList = (List<RoutingComponentMapping>) mappings;
            return mappingList.size() == 3 &&
                    mappingList.get(0).getRoutingId().equals(TEST_ID) &&
                    mappingList.get(0).getComponentId().equals(10L) &&
                    mappingList.get(0).getOrderIndex().equals(0) &&
                    mappingList.get(1).getRoutingId().equals(TEST_ID) &&
                    mappingList.get(1).getComponentId().equals(11L) &&
                    mappingList.get(1).getOrderIndex().equals(1) &&
                    mappingList.get(2).getRoutingId().equals(TEST_ID) &&
                    mappingList.get(2).getComponentId().equals(12L) &&
                    mappingList.get(2).getOrderIndex().equals(2);
        }));
    }

    @Test
    void updateComponentsMapping_withEmptyComponentIds_shouldOnlyDeleteExistingMappings() {
        // Given
        List<Long> componentIds = List.of();
        when(repository.findById(TEST_ID)).thenReturn(Optional.of(routing));

        // When
        service.updateComponentsMapping(TEST_ID, componentIds);

        // Then
        verify(repository).findById(TEST_ID);
        verify(mappingRepository).deleteAllByRoutingId(TEST_ID);
        verify(mappingRepository, never()).saveAll(any());
    }

    @Test
    void updateComponentsMapping_withNullComponentIds_shouldOnlyDeleteExistingMappings() {
        // Given
        when(repository.findById(TEST_ID)).thenReturn(Optional.of(routing));

        // When
        service.updateComponentsMapping(TEST_ID, null);

        // Then
        verify(repository).findById(TEST_ID);
        verify(mappingRepository).deleteAllByRoutingId(TEST_ID);
        verify(mappingRepository, never()).saveAll(any());
    }

    @Test
    void updateComponentsMapping_withNonExistentRoutingId_shouldThrowException() {
        // Given
        List<Long> componentIds = List.of(10L, 11L);
        when(repository.findById(TEST_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> service.updateComponentsMapping(TEST_ID, componentIds))
                .isInstanceOf(DataNotFoundException.class);

        verify(repository).findById(TEST_ID);
        verify(mappingRepository, never()).deleteAllByRoutingId(any());
        verify(mappingRepository, never()).saveAll(any());
    }

    @Test
    void updateComponentsMapping_withSingleComponent_shouldCreateSingleMapping() {
        // Given
        List<Long> componentIds = List.of(100L);
        when(repository.findById(TEST_ID)).thenReturn(Optional.of(routing));

        // When
        service.updateComponentsMapping(TEST_ID, componentIds);

        // Then
        verify(repository).findById(TEST_ID);
        verify(mappingRepository).deleteAllByRoutingId(TEST_ID);
        verify(mappingRepository).saveAll(argThat(mappings -> {
            List<RoutingComponentMapping> mappingList = (List<RoutingComponentMapping>) mappings;
            return mappingList.size() == 1 &&
                    mappingList.get(0).getRoutingId().equals(TEST_ID) &&
                    mappingList.get(0).getComponentId().equals(100L) &&
                    mappingList.get(0).getOrderIndex().equals(0);
        }));
    }

    @Test
    void test_modelType() {
        assertThat(service.getModelType()).isEqualTo(Routing.class);
    }

    @Test
    void test_repository() {
        assertThat(service.getRepository()).isEqualTo(repository);
    }
}
