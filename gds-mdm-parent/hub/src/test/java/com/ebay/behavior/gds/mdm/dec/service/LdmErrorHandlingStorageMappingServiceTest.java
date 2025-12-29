package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.dec.model.LdmErrorHandlingStorageMapping;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalStorage;
import com.ebay.behavior.gds.mdm.dec.model.enums.PlatformEnvironment;
import com.ebay.behavior.gds.mdm.dec.repository.LdmErrorHandlingStorageMappingRepository;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LdmErrorHandlingStorageMappingServiceTest {

    @Mock
    private LdmErrorHandlingStorageMappingRepository repository;

    @Mock
    private PhysicalStorageService storageService;

    @Spy
    @InjectMocks
    private LdmErrorHandlingStorageMappingService service;

    private final Long ldmEntityId = 1L;
    private final Integer ldmVersion = 1;
    private final Long physicalStorageId1 = 101L;
    private final Long physicalStorageId2 = 102L;
    private final Long physicalStorageId3 = 103L;
    
    private LdmErrorHandlingStorageMapping mapping1;
    private LdmErrorHandlingStorageMapping mapping2;
    private List<LdmErrorHandlingStorageMapping> mappings;
    private PhysicalStorage storage1;
    private PhysicalStorage storage2;
    private PhysicalStorage storage3;

    @BeforeEach
    void setUp() {
        // Create test mappings
        mapping1 = createMapping(1L, ldmEntityId, ldmVersion, physicalStorageId1);
        mapping2 = createMapping(2L, ldmEntityId, ldmVersion, physicalStorageId2);
        mappings = List.of(mapping1, mapping2);
        
        // Create test storages
        storage1 = TestModelUtils.physicalStorage();
        storage1.setId(physicalStorageId1);
        storage1.setStorageEnvironment(PlatformEnvironment.STAGING);
        
        storage2 = TestModelUtils.physicalStorage();
        storage2.setId(physicalStorageId2);
        storage2.setStorageEnvironment(PlatformEnvironment.PRODUCTION);
        
        storage3 = TestModelUtils.physicalStorage();
        storage3.setId(physicalStorageId3);
        storage3.setStorageEnvironment(PlatformEnvironment.PRE_PRODUCTION);
    }

    @Test
    void getAllByLdmEntityIdCurrentVersion_shouldReturnMappings() {
        // Arrange
        when(repository.findByLdmEntityIdCurrentVersion(ldmEntityId)).thenReturn(mappings);

        // Act
        List<LdmErrorHandlingStorageMapping> result = service.getAllByLdmEntityIdCurrentVersion(ldmEntityId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrderElementsOf(mappings);
        verify(repository).findByLdmEntityIdCurrentVersion(ldmEntityId);
    }

    @Test
    void getAllByLdmEntityIdCurrentVersion_whenNoMappingsExist_shouldReturnEmptyList() {
        // Arrange
        when(repository.findByLdmEntityIdCurrentVersion(ldmEntityId)).thenReturn(Collections.emptyList());

        // Act
        List<LdmErrorHandlingStorageMapping> result = service.getAllByLdmEntityIdCurrentVersion(ldmEntityId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(repository).findByLdmEntityIdCurrentVersion(ldmEntityId);
    }

    @Test
    void getAllByLdmEntityId_withLatestTrue_andCurrentMappingsExist_shouldReturnCurrentMappings() {
        // Arrange
        when(repository.findByLdmEntityIdCurrentVersion(ldmEntityId)).thenReturn(mappings);

        // Act
        List<LdmErrorHandlingStorageMapping> result = service.getAllByLdmEntityIdCurrentVersion(ldmEntityId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrderElementsOf(mappings);
        verify(repository).findByLdmEntityIdCurrentVersion(ldmEntityId);
        verify(repository, never()).findByLdmEntityId(ldmEntityId);
    }

    @Test
    void getAllByLdmEntityIdAndEnvironment_shouldFilterByEnvironment() {
        // Arrange
        when(repository.findByLdmEntityIdCurrentVersion(ldmEntityId)).thenReturn(mappings);
        when(storageService.getById(physicalStorageId1)).thenReturn(storage1);
        when(storageService.getById(physicalStorageId2)).thenReturn(storage2);

        // Act
        List<LdmErrorHandlingStorageMapping> result = service.getAllByLdmEntityIdAndEnvironment(ldmEntityId, "STAGING", false);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(mapping1);
        verify(repository).findByLdmEntityIdCurrentVersion(ldmEntityId);
        verify(storageService, times(2)).getById(anyLong());
    }

    @Test
    void getAllByLdmEntityIdAndEnvironment_withNullEnvironment_shouldReturnAllMappings() {
        // Arrange
        when(repository.findByLdmEntityIdCurrentVersion(ldmEntityId)).thenReturn(mappings);

        // Act
        List<LdmErrorHandlingStorageMapping> result = service.getAllByLdmEntityIdAndEnvironment(ldmEntityId, null, false);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrderElementsOf(mappings);
        verify(repository).findByLdmEntityIdCurrentVersion(ldmEntityId);
        verify(storageService, never()).getById(anyLong());
    }

    @Test
    void getAllByLdmEntityIdAndEnvironment_withNullMappings_shouldReturnNull() {
        // Arrange
        when(repository.findByLdmEntityIdCurrentVersion(ldmEntityId)).thenReturn(null);

        // Act
        List<LdmErrorHandlingStorageMapping> result = service.getAllByLdmEntityIdAndEnvironment(ldmEntityId, "STAGING", false);

        // Assert
        assertThat(result).isNull();
        verify(repository).findByLdmEntityIdCurrentVersion(ldmEntityId);
        verify(storageService, never()).getById(anyLong());
    }

    @Test
    void getAllByLdmEntityIdAndEnvironment_withPreProductionEnvironment_shouldFilterCorrectly() {
        // Arrange
        LdmErrorHandlingStorageMapping mapping3 = createMapping(3L, ldmEntityId, ldmVersion, physicalStorageId3);
        List<LdmErrorHandlingStorageMapping> allMappings = List.of(mapping1, mapping2, mapping3);
        
        when(repository.findByLdmEntityIdCurrentVersion(ldmEntityId)).thenReturn(allMappings);
        when(storageService.getById(physicalStorageId1)).thenReturn(storage1);
        when(storageService.getById(physicalStorageId2)).thenReturn(storage2);
        when(storageService.getById(physicalStorageId3)).thenReturn(storage3);

        // Act
        List<LdmErrorHandlingStorageMapping> result = service.getAllByLdmEntityIdAndEnvironment(ldmEntityId, "PRE_PRODUCTION", false);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(mapping3);
        verify(repository).findByLdmEntityIdCurrentVersion(ldmEntityId);
        verify(storageService, times(3)).getById(anyLong());
    }

    @Test
    void getAllByLdmEntityIdAndVersion_shouldReturnMappingsForVersion() {
        // Arrange
        when(repository.findByLdmEntityIdAndLdmVersion(ldmEntityId, ldmVersion)).thenReturn(mappings);

        // Act
        List<LdmErrorHandlingStorageMapping> result = service.getAllByLdmEntityIdAndVersion(ldmEntityId, ldmVersion);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrderElementsOf(mappings);
        verify(repository).findByLdmEntityIdAndLdmVersion(ldmEntityId, ldmVersion);
    }

    @Test
    void getAllByLdmEntityIdAndVersion_whenNoMappingsExist_shouldReturnEmptyList() {
        // Arrange
        when(repository.findByLdmEntityIdAndLdmVersion(ldmEntityId, ldmVersion)).thenReturn(Collections.emptyList());

        // Act
        List<LdmErrorHandlingStorageMapping> result = service.getAllByLdmEntityIdAndVersion(ldmEntityId, ldmVersion);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(repository).findByLdmEntityIdAndLdmVersion(ldmEntityId, ldmVersion);
    }

    @Test
    void getAllByLdmEntityIdAndVersionAndEnvironment_shouldFilterByEnvironment() {
        // Arrange
        when(repository.findByLdmEntityIdAndLdmVersion(ldmEntityId, ldmVersion)).thenReturn(mappings);
        when(storageService.getById(physicalStorageId1)).thenReturn(storage1);
        when(storageService.getById(physicalStorageId2)).thenReturn(storage2);

        // Act
        List<LdmErrorHandlingStorageMapping> result = service.getAllByLdmEntityIdAndVersionAndEnvironment(
                ldmEntityId, ldmVersion, "PRODUCTION");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(mapping2);
        verify(repository).findByLdmEntityIdAndLdmVersion(ldmEntityId, ldmVersion);
        verify(storageService, times(2)).getById(anyLong());
    }

    @Test
    void getAllByLdmEntityIdAndVersionAndEnvironment_withNullEnvironment_shouldReturnAllMappings() {
        // Arrange
        when(repository.findByLdmEntityIdAndLdmVersion(ldmEntityId, ldmVersion)).thenReturn(mappings);

        // Act
        List<LdmErrorHandlingStorageMapping> result = service.getAllByLdmEntityIdAndVersionAndEnvironment(
                ldmEntityId, ldmVersion, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrderElementsOf(mappings);
        verify(repository).findByLdmEntityIdAndLdmVersion(ldmEntityId, ldmVersion);
        verify(storageService, never()).getById(anyLong());
    }

    @Test
    void getAllByLdmEntityIdAndVersionAndEnvironment_withNullMappings_shouldReturnNull() {
        // Arrange
        when(repository.findByLdmEntityIdAndLdmVersion(ldmEntityId, ldmVersion)).thenReturn(null);

        // Act
        List<LdmErrorHandlingStorageMapping> result = service.getAllByLdmEntityIdAndVersionAndEnvironment(
                ldmEntityId, ldmVersion, "STAGING");

        // Assert
        assertThat(result).isNull();
        verify(repository).findByLdmEntityIdAndLdmVersion(ldmEntityId, ldmVersion);
        verify(storageService, never()).getById(anyLong());
    }

    @Test
    void getAllByLdmEntityIdAndVersionAndEnvironment_withPreProductionEnvironment_shouldFilterCorrectly() {
        // Arrange
        LdmErrorHandlingStorageMapping mapping3 = createMapping(3L, ldmEntityId, ldmVersion, physicalStorageId3);
        List<LdmErrorHandlingStorageMapping> allMappings = List.of(mapping1, mapping2, mapping3);
        
        when(repository.findByLdmEntityIdAndLdmVersion(ldmEntityId, ldmVersion)).thenReturn(allMappings);
        when(storageService.getById(physicalStorageId1)).thenReturn(storage1);
        when(storageService.getById(physicalStorageId2)).thenReturn(storage2);
        when(storageService.getById(physicalStorageId3)).thenReturn(storage3);

        // Act
        List<LdmErrorHandlingStorageMapping> result = service.getAllByLdmEntityIdAndVersionAndEnvironment(
                ldmEntityId, ldmVersion, "PRE_PRODUCTION");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(mapping3);
        verify(repository).findByLdmEntityIdAndLdmVersion(ldmEntityId, ldmVersion);
        verify(storageService, times(3)).getById(anyLong());
    }

    @Test
    void saveErrorHandlingStorageMappings_withNewMappings_shouldSaveAndReturnMappings() {
        // Arrange
        Set<LdmErrorHandlingStorageMapping> newMappings = new HashSet<>();
        LdmErrorHandlingStorageMapping newMapping = createMapping(null, null, null, physicalStorageId1);
        newMappings.add(newMapping);
        
        when(repository.findByLdmEntityIdAndLdmVersion(ldmEntityId, ldmVersion)).thenReturn(new ArrayList<>());
        when(storageService.getById(physicalStorageId1)).thenReturn(storage1);
        when(repository.saveAll(anyList())).thenReturn(List.of(mapping1));

        // Act
        List<LdmErrorHandlingStorageMapping> result = service.saveErrorHandlingStorageMappings(ldmEntityId, ldmVersion, newMappings);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(repository).findByLdmEntityIdAndLdmVersion(ldmEntityId, ldmVersion);
        verify(storageService).getById(physicalStorageId1);
        verify(repository).saveAll(anyList());
        verify(repository, never()).deleteAll(anyList());
    }

    @Test
    void saveErrorHandlingStorageMappings_withExistingAndNewMappings_shouldDeleteAndSave() {
        // Arrange
        // Existing mapping to keep
        LdmErrorHandlingStorageMapping existingMapping = createMapping(1L, ldmEntityId, ldmVersion, physicalStorageId1);
        // Existing mapping to delete
        LdmErrorHandlingStorageMapping mappingToDelete = createMapping(2L, ldmEntityId, ldmVersion, physicalStorageId2);
        List<LdmErrorHandlingStorageMapping> currentMappings = List.of(existingMapping, mappingToDelete);
        
        // New mapping to add
        Long newPhysicalStorageId = 103L;
        PhysicalStorage newStorage = TestModelUtils.physicalStorage();
        newStorage.setId(newPhysicalStorageId);
        
        Set<LdmErrorHandlingStorageMapping> requestMappings = new HashSet<>();
        // Keep existing mapping
        requestMappings.add(createMapping(null, null, null, physicalStorageId1));
        // Add new mapping
        LdmErrorHandlingStorageMapping newMapping = createMapping(null, null, null, newPhysicalStorageId);
        requestMappings.add(newMapping);
        
        when(repository.findByLdmEntityIdAndLdmVersion(ldmEntityId, ldmVersion)).thenReturn(currentMappings);
        when(storageService.getById(anyLong())).thenReturn(storage1);
        when(repository.saveAll(anyList())).thenReturn(List.of(newMapping));

        // Act
        List<LdmErrorHandlingStorageMapping> result = service.saveErrorHandlingStorageMappings(ldmEntityId, ldmVersion, requestMappings);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(repository).findByLdmEntityIdAndLdmVersion(ldmEntityId, ldmVersion);
        verify(storageService, times(2)).getById(anyLong());
        verify(repository).saveAll(anyList());
        verify(repository).deleteAll(anyList());
    }

    @Test
    void saveErrorHandlingStorageMappings_withNoChanges_shouldNotSaveOrDelete() {
        // Arrange
        // Existing mappings
        LdmErrorHandlingStorageMapping existingMapping1 = createMapping(1L, ldmEntityId, ldmVersion, physicalStorageId1);
        LdmErrorHandlingStorageMapping existingMapping2 = createMapping(2L, ldmEntityId, ldmVersion, physicalStorageId2);
        List<LdmErrorHandlingStorageMapping> currentMappings = List.of(existingMapping1, existingMapping2);
        
        // Same mappings in request
        Set<LdmErrorHandlingStorageMapping> requestMappings = new HashSet<>();
        requestMappings.add(createMapping(null, null, null, physicalStorageId1));
        requestMappings.add(createMapping(null, null, null, physicalStorageId2));
        
        when(repository.findByLdmEntityIdAndLdmVersion(ldmEntityId, ldmVersion)).thenReturn(currentMappings);
        when(storageService.getById(anyLong())).thenReturn(storage1);

        // Act
        List<LdmErrorHandlingStorageMapping> result = service.saveErrorHandlingStorageMappings(
                ldmEntityId, ldmVersion, requestMappings);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty(); // No new mappings to save
        verify(repository).findByLdmEntityIdAndLdmVersion(ldmEntityId, ldmVersion);
        verify(storageService, times(2)).getById(anyLong());
        verify(repository, never()).deleteAll(anyList());
        verify(repository).saveAll(Collections.emptyList());
    }

    @Test
    void getByIdWithAssociations_shouldThrowNotImplementedException() {
        // Act & Assert
        assertThatThrownBy(() -> service.getByIdWithAssociations(1L))
                .isInstanceOf(NotImplementedException.class)
                .hasMessage("Not implemented by design");
    }

    private LdmErrorHandlingStorageMapping createMapping(Long id, Long entityId, Integer version, Long storageId) {
        LdmErrorHandlingStorageMapping mapping = new LdmErrorHandlingStorageMapping();
        mapping.setId(id);
        mapping.setLdmEntityId(entityId);
        mapping.setLdmVersion(version);
        mapping.setPhysicalStorageId(storageId);
        return mapping;
    }
}
