package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.contract.model.ConfigStorageMapping;
import com.ebay.behavior.gds.mdm.contract.repository.ConfigStorageMappingRepository;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomString;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.hiveConfig;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.hiveStorage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfigStorageMappingServiceTest {

    @InjectMocks
    private ConfigStorageMappingService service;

    @Mock
    private ConfigStorageMappingRepository repository;

    private ConfigStorageMapping mapping;

    private static final Long TEST_ID = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        val hiveConfig = hiveConfig(getRandomLong()).toBuilder().id(123L).build();
        val hiveStorage = hiveStorage(getRandomString()).toBuilder().id(456L).build();
        mapping = ConfigStorageMapping.builder().configId(hiveConfig.getId()).storageId(hiveStorage.getId()).id(TEST_ID).revision(1).build();
    }

    @Test
    void test_create() {
        var newMapping = ConfigStorageMapping.builder()
                .configId(123L)
                .storageId(456L)
                .build();
        var savedMapping = newMapping.toBuilder().id(TEST_ID).build();

        when(repository.save(newMapping)).thenReturn(savedMapping);

        var result = service.create(newMapping);

        assertThat(result).isEqualTo(savedMapping);
        assertThat(result.getId()).isEqualTo(TEST_ID);
        verify(repository).save(newMapping);
    }

    @Test
    void test_update_existed() {
        var updatedMapping = mapping.toBuilder().build();
        when(repository.findById(TEST_ID)).thenReturn(Optional.of(mapping));
        when(repository.save(mapping)).thenReturn(updatedMapping);

        var result = service.update(mapping);

        assertThat(result).isEqualTo(updatedMapping);
        verify(repository).save(mapping);
    }

    @Test
    void test_delete_existed() {
        when(repository.findById(TEST_ID)).thenReturn(Optional.of(mapping));
        service.delete(TEST_ID);
        verify(repository).deleteById(TEST_ID);
    }
}
