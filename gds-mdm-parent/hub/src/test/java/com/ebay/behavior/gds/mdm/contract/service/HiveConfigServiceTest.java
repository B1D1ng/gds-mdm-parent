package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.contract.model.ConfigStorageMapping;
import com.ebay.behavior.gds.mdm.contract.model.HiveConfig;
import com.ebay.behavior.gds.mdm.contract.model.HiveStorage;
import com.ebay.behavior.gds.mdm.contract.repository.ConfigStorageMappingRepository;
import com.ebay.behavior.gds.mdm.contract.repository.HiveConfigRepository;
import com.ebay.behavior.gds.mdm.contract.repository.HiveSourceRepository;
import com.ebay.behavior.gds.mdm.contract.repository.HiveStorageRepository;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomString;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.hiveConfig;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.hiveStorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HiveConfigServiceTest {

    @InjectMocks
    private HiveConfigService service;

    @InjectMocks
    private ConfigStorageMappingService mappingService;

    @Mock
    private HiveConfigRepository repository;

    @Mock
    private HiveStorageRepository storageRepository;

    @Mock
    private ConfigStorageMappingRepository configStorageMapingRepository;

    private HiveConfig hiveConfig;
    private HiveStorage hiveStorage;
    private static final Long TEST_ID = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        hiveConfig = hiveConfig(getRandomLong());
        hiveStorage = hiveStorage(getRandomString());
    }

    @Test
    void test_create() {
        var savedHiveConfig = hiveConfig.toBuilder().id(TEST_ID).build();
        when(repository.save(hiveConfig)).thenReturn(savedHiveConfig);

        var result = service.create(hiveConfig);

        assertThat(result).isEqualTo(savedHiveConfig);
        assertThat(result.getId()).isEqualTo(TEST_ID);
        verify(repository).save(hiveConfig);
    }

    @Test
    void test_create_DataIntegrityViolationException() {
        when(repository.save(hiveConfig))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violation"));

        try {
            service.create(hiveConfig);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalArgumentException.class);
            assertThat(e.getMessage()).contains("Unique constraint violation");
        }

        verify(repository).save(hiveConfig);
    }

    @Test
    void test_getById_withoutStorage() {
        var savedHiveConfig = hiveConfig.toBuilder().id(TEST_ID).build();
        when(repository.findById(TEST_ID)).thenReturn(Optional.of(savedHiveConfig));

        var result = service.getById(TEST_ID);

        assertThat(result).isEqualTo(savedHiveConfig);
        verify(repository).findById(TEST_ID);
    }

    @Test
    void test_getById_withStorage() {
        var savedNewHiveConfig = hiveConfig.toBuilder().id(TEST_ID).build();
        var savedNewHiveStorage = hiveStorage.toBuilder().id(2L).build();
        var finalHiveConfig = savedNewHiveConfig.toBuilder().hiveStorage(savedNewHiveStorage).build();

        when(repository.findById(TEST_ID)).thenReturn(Optional.of(finalHiveConfig));
        val result = service.getByIdWithAssociations(TEST_ID);

        assertThat(result).isEqualTo(finalHiveConfig);
        verify(repository).findById(TEST_ID);
    }

    @Test
    void test_update_withStorage() {
        var savedHiveConfig = hiveConfig.toBuilder().id(TEST_ID).revision(0).build();
        var savedHiveStorage = hiveStorage.toBuilder().id(2L).revision(0).build();
        var updateStorage = hiveStorage(getRandomString());
        var savedUpdateStorage = updateStorage.toBuilder().id(3L).revision(0).build();
        var updatedHiveConfigWithStorage = savedHiveConfig.toBuilder().hiveStorage(savedUpdateStorage).revision(1).build();

        when(storageRepository.save(updateStorage)).thenReturn(savedUpdateStorage);
        when(repository.save(updatedHiveConfigWithStorage)).thenReturn(updatedHiveConfigWithStorage);

        var mapping = ConfigStorageMapping.builder()
                .configId(savedHiveConfig.getId()).storageId(savedHiveStorage.getId()).id(1L).revision(0).build();
        var updateMapping = mapping.toBuilder().storageId(updateStorage.getId()).revision(1).build();

        when(repository.findById(TEST_ID)).thenReturn(Optional.of(updatedHiveConfigWithStorage));
        when(configStorageMapingRepository.findById(1L)).thenReturn(Optional.of(updateMapping));

        mappingService.update(updateMapping);

        assertThat(configStorageMapingRepository.findById(1L)).isEqualTo(Optional.of(updateMapping));
        assertThat(repository.findById(TEST_ID)).isEqualTo(Optional.of(updatedHiveConfigWithStorage));
        verify(configStorageMapingRepository).save(updateMapping);
        assertThat(updatedHiveConfigWithStorage.getHiveStorage()).isEqualTo(savedUpdateStorage);
    }
}
