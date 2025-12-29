package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.contract.model.ConfigStorageMapping;
import com.ebay.behavior.gds.mdm.contract.model.HiveStorage;
import com.ebay.behavior.gds.mdm.contract.repository.ConfigStorageMappingRepository;
import com.ebay.behavior.gds.mdm.contract.repository.HiveStorageRepository;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomString;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.hiveStorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;

class HiveStorageServiceTest {

    @InjectMocks
    private HiveStorageService service;

    @Mock
    private HiveStorageRepository repository;

    @Mock
    private ConfigStorageMappingRepository configStorageMappingRepository;

    private HiveStorage hiveStorage;

    private static final Long TEST_ID = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        hiveStorage = hiveStorage(getRandomString()).toBuilder().id(TEST_ID).revision(1).build();
    }

    @Test
    void test_create() {
        var newHiveStorage = hiveStorage(getRandomString());
        var savedHiveStorage = newHiveStorage.toBuilder().id(TEST_ID).build();

        when(repository.save(newHiveStorage)).thenReturn(savedHiveStorage);

        var result = service.create(newHiveStorage);

        assertThat(result).isEqualTo(savedHiveStorage);
        assertThat(result.getId()).isEqualTo(TEST_ID);
        verify(repository).save(newHiveStorage);
    }

    @Test
    void test_getById_exists() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.of(hiveStorage));

        var result = service.getById(TEST_ID);

        assertThat(result).isEqualTo(hiveStorage);
        verify(repository).findById(TEST_ID);
    }

    @Test
    void test_findById_exists() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.of(hiveStorage));

        var result = service.findById(TEST_ID);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(hiveStorage);
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
    void test_update_exists() {
        var updatedName = getRandomString();
        var updateRequest = hiveStorage.toBuilder().tableName(updatedName).revision(1).build();

        when(repository.findById(TEST_ID)).thenReturn(Optional.of(hiveStorage));
        when(repository.save(updateRequest)).thenReturn(updateRequest);

        var result = service.update(updateRequest);
        assertThat(result.getTableName()).isEqualTo(updatedName);
        verify(repository).findById(TEST_ID);
        verify(repository).save(updateRequest);
    }

    @Test
    void test_update_nonExists() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(hiveStorage))
                .isInstanceOf(DataNotFoundException.class);

        verify(repository).findById(TEST_ID);
        verify(repository, never()).save(any());
    }

    @Test
    void test_delete_exists_withoutHiveConfig() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.of(hiveStorage));
        when(configStorageMappingRepository.findByStorageId(TEST_ID)).thenReturn(null);
        service.delete(TEST_ID);

        verify(repository).findById(TEST_ID);
        verify(configStorageMappingRepository).findByStorageId(TEST_ID);
        verify(repository).deleteById(TEST_ID);
    }

    @Test
    void test_delete_notFound() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(TEST_ID))
                .isInstanceOf(DataNotFoundException.class);

        verify(repository).findById(TEST_ID);
        verify(repository, never()).deleteById(TEST_ID);
    }

    @Test
    void test_delete_withMapping() {
        val configStorageMapping = ConfigStorageMapping.builder()
                .id(2L)
                .configId(3L)
                .storageId(TEST_ID)
                .revision(0)
                .build();
        when(configStorageMappingRepository.findByStorageId(TEST_ID)).thenReturn(configStorageMapping);
        when(repository.findById(TEST_ID)).thenReturn(Optional.of(hiveStorage));

        assertThatThrownBy(() -> service.delete(TEST_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot delete HiveStorage with associated ConfigStorageMapping id");

        verify(configStorageMappingRepository).findByStorageId(TEST_ID);
        verify(repository).findById(TEST_ID);
        verify(repository, never()).deleteById(TEST_ID);
    }

    @Test
    void test_getModelType_returnsCorrectType() {
        assertThat(service.getModelType()).isEqualTo(HiveStorage.class);
    }

    @Test
    void test_getRepository_returnsCorrectRepository() {
        assertThat(service.getRepository()).isEqualTo(repository);
    }
}