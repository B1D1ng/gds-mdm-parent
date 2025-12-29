package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.contract.model.ConfigStorageMapping;
import com.ebay.behavior.gds.mdm.contract.model.HiveSource;
import com.ebay.behavior.gds.mdm.contract.model.Routing;
import com.ebay.behavior.gds.mdm.contract.model.manyToMany.RoutingComponentMapping;
import com.ebay.behavior.gds.mdm.contract.repository.ConfigStorageMappingRepository;
import com.ebay.behavior.gds.mdm.contract.repository.HiveConfigRepository;
import com.ebay.behavior.gds.mdm.contract.repository.HiveSourceRepository;
import com.ebay.behavior.gds.mdm.contract.repository.manyToMany.RoutingComponentMappingRepository;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.hiveSource;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.hiveConfig;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.getRandomString;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

class HiveSourceServiceTest {

    @InjectMocks
    private HiveSourceService service;

    @Mock
    private HiveSourceRepository repository;

    @Mock
    private RoutingComponentMappingRepository mappingRepository;

    @Mock
    private HiveConfigRepository configRepository;

    @Mock
    private ConfigStorageMappingRepository configStorageMappingRepository;

    private HiveSource hiveSource;
    private static final Long TEST_ID = 123L;

    @BeforeEach
    void setUp() {
         MockitoAnnotations.openMocks(this);
         hiveSource = hiveSource(getRandomString()).toBuilder().id(TEST_ID).revision(1).build();
    }

    @Test
    void test_created() {
        var newHiveSource = hiveSource(getRandomString());
        var savedHiveSource = newHiveSource.toBuilder().id(TEST_ID).build();

        when(repository.save(newHiveSource)).thenReturn(savedHiveSource);

        var result = service.create(newHiveSource);
        assertThat(result).isEqualTo(savedHiveSource);
        assertThat(result.getId()).isEqualTo(TEST_ID);
        verify(repository).save(newHiveSource);
    }

    @Test
    void test_create_invalidType_throwsException() {
        var invalidHiveSource = hiveSource(getRandomString()).toBuilder().type("InvalidType").build();

        assertThatThrownBy(() -> service.create(invalidHiveSource))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type mismatch from");
        verify(repository, never()).save(any());
    }

    @Test
    void test_create_dataIntegrityViolation_throwsException() {
        var newHiveSource = hiveSource(getRandomString());

        when(repository.save(newHiveSource)).thenThrow(new DataIntegrityViolationException("FK violation"));

        assertThatThrownBy(() -> service.create(newHiveSource)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void test_update() {
        var updatedName = getRandomString();
        var updatedHiveSource = hiveSource.toBuilder().name(updatedName).revision(1).build();

        when(repository.findById(TEST_ID)).thenReturn(Optional.of(hiveSource));
        when(repository.save(updatedHiveSource)).thenReturn(updatedHiveSource);

        var result = service.update(updatedHiveSource);

        assertThat(result).isEqualTo(updatedHiveSource);
        assertThat(result.getName()).isEqualTo(updatedName);
        verify(repository).findById(TEST_ID);
        verify(repository).save(updatedHiveSource);
    }

    @Test
    void test_update_invalidType_throwsException() {
        var updatedHiveSource = hiveSource.toBuilder().type("WrongType").revision(1).build();

        assertThatThrownBy(() -> service.update(updatedHiveSource))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type mismatch from");
        verify(repository, never()).save(any());
    }

    @Test
    void test_update_notFound() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(hiveSource))
                .isInstanceOf(DataNotFoundException.class);

        verify(repository).findById(TEST_ID);
        verify(repository, never()).save(any());
    }

    @Test
    void test_getById_existed() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.of(hiveSource));

        var result = service.getById(TEST_ID);

        assertThat(result).isEqualTo(hiveSource);
        verify(repository).findById(TEST_ID);
    }

    @Test
    void test_findById_existed() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.of(hiveSource));

        var result = service.findById(TEST_ID);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(hiveSource);
        verify(repository).findById(TEST_ID);
    }

    @Test
    void test_getById_notFound() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(TEST_ID))
                .isInstanceOf(DataNotFoundException.class);

        verify(repository).findById(TEST_ID);
    }

    @Test
    void test_delete_existed_withoutRoutings_withoutConfig() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.of(hiveSource));
        when(configRepository.getHiveConfigByComponentId(TEST_ID)).thenReturn(List.of());

        service.delete(TEST_ID);

        verify(repository, times(2)).findById(TEST_ID);
        verify(configRepository).getHiveConfigByComponentId(TEST_ID);
        verify(repository).deleteById(TEST_ID);
    }

    @Test
    void test_delete_existed_withoutRoutings_withConfig() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.of(hiveSource));
        val hiveConfig = hiveConfig(hiveSource.getId()).toBuilder().id(456L).build();
        val mapping = ConfigStorageMapping.builder().id(789L).configId(hiveConfig.getId()).storageId(3L).build();
        when(configRepository.getHiveConfigByComponentId(TEST_ID)).thenReturn(List.of(hiveConfig));
        when(configStorageMappingRepository.findByConfigId(hiveConfig.getId())).thenReturn(mapping);

        service.delete(TEST_ID);
        verify(repository, times(2)).findById(TEST_ID);
        verify(configRepository).getHiveConfigByComponentId(TEST_ID);
        verify(configStorageMappingRepository).findByConfigId(hiveConfig.getId());

        verify(configStorageMappingRepository).deleteById(mapping.getId());
        verify(configRepository).deleteAllByComponentId(TEST_ID);
        verify(repository).deleteById(TEST_ID);
    }

    @Test
    void test_delete_existed_withRouting() {
        var routing = Routing.builder().id(1L).name("Test Routing").build();
        var mapping = RoutingComponentMapping.builder().routing(routing).build();

        when(repository.findById(TEST_ID)).thenReturn(Optional.of(hiveSource));
        when(mappingRepository.findByComponentId(TEST_ID)).thenReturn(List.of(mapping));

        assertThatThrownBy(() -> service.delete(TEST_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot delete component with associated routings.");

        verify(configRepository, never()).deleteAllByComponentId(anyLong());
        verify(repository, never()).deleteById(anyLong());
    }

    @Test
    void test_delete_nonExisted() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(TEST_ID))
                .isInstanceOf(DataNotFoundException.class);

        verify(configRepository, never()).deleteAllByComponentId(anyLong());
        verify(repository, never()).deleteById(anyLong());
    }

    @Test
    void getModelType_returnsHiveSourceClass() {
        assertThat(service.getModelType()).isEqualTo(HiveSource.class);
    }

    @Test
    void getRepository_retruensHiveSourceRepository() {
        assertThat(service.getRepository()).isEqualTo(repository);
    }
}