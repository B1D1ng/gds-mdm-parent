package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmErrorHandlingStorageMapping;
import com.ebay.behavior.gds.mdm.dec.model.LdmField;
import com.ebay.behavior.gds.mdm.dec.model.LdmFieldPhysicalStorageMapping;
import com.ebay.behavior.gds.mdm.dec.model.enums.ViewType;
import com.ebay.behavior.gds.mdm.dec.repository.LdmEntityRepository;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LdmEntityVersioningServiceTest {

    @Mock
    private LdmEntityRepository repository;

    @Mock
    private LdmEntityIndexService indexService;

    @Mock
    private LdmReadService readService;

    @Mock
    private LdmFieldService fieldService;

    @Mock
    private LdmErrorHandlingStorageMappingService errHandlingStorageService;
    
    @Mock
    private LdmEntityBasicInfoService basicInfoService;

    @InjectMocks
    private LdmEntityVersioningService versioningService;

    @Captor
    private ArgumentCaptor<LdmEntity> entityCaptor;

    private LdmEntity existingEntity;
    private LdmEntity newEntity;
    private Long changeRequestId;

    @BeforeEach
    void setUp() {
        existingEntity = TestModelUtils.ldmEntityEmpty(1L);
        existingEntity.setId(1L);
        existingEntity.setVersion(1);
        existingEntity.setBaseEntityId(100L);
        existingEntity.setViewType(ViewType.RAW);
        existingEntity.setCreateBy("original-user");
        existingEntity.setCreateDate(java.sql.Timestamp.valueOf("2023-01-01 00:00:00"));
        
        newEntity = TestModelUtils.ldmEntityEmpty(1L);
        newEntity.setId(1L);
        newEntity.setName("updated-entity");
        
        changeRequestId = 123L;
    }

    @Test
    void saveAsNewVersion_shouldIncrementVersionAndPreserveMetadata() {
        // Arrange
        when(readService.getByIdWithAssociationsCurrentVersion(anyLong())).thenReturn(existingEntity);
        when(repository.save(any(LdmEntity.class))).thenReturn(newEntity);
        
        // Act
        LdmEntity result = versioningService.saveAsNewVersion(newEntity, changeRequestId, false);
        
        // Assert
        verify(indexService).updateVersion(eq(1L), eq(2)); // Version incremented from 1 to 2
        verify(basicInfoService).handleBasicInfoUpdate(newEntity, existingEntity.getBaseEntityId(), existingEntity.getViewType());
        verify(repository).save(entityCaptor.capture());
        
        LdmEntity capturedEntity = entityCaptor.getValue();
        assertThat(capturedEntity.getVersion()).isEqualTo(2);
        assertThat(capturedEntity.getBaseEntityId()).isEqualTo(existingEntity.getBaseEntityId());
        assertThat(capturedEntity.getCreateBy()).isEqualTo(existingEntity.getCreateBy());
        assertThat(capturedEntity.getCreateDate()).isEqualTo(existingEntity.getCreateDate());
        assertThat(capturedEntity.getRequestId()).isEqualTo(changeRequestId);
        assertThat(capturedEntity.getName()).isEqualTo(newEntity.getName().toLowerCase(Locale.US));
        
        assertThat(result).isEqualTo(newEntity);
    }

    @Test
    void saveAsNewVersion_withPhysicalMappings_shouldCarryOverMappings() {
        // Arrange
        Set<LdmField> fields = new HashSet<>();
        LdmField field = TestModelUtils.ldmFieldEmpty();
        field.setName("test-field");
        fields.add(field);
        newEntity.setFields(fields);
        
        Set<LdmErrorHandlingStorageMapping> errorMappings = new HashSet<>();
        LdmErrorHandlingStorageMapping errorMapping = TestModelUtils.ldmErrorHandlingStorageMappingEmpty(1L);
        errorMapping.setPhysicalStorageId(101L);
        errorMappings.add(errorMapping);
        existingEntity.setErrorHandlingStorageMappings(errorMappings);
        
        Map<String, Set<LdmFieldPhysicalStorageMapping>> fieldPhysicalMapping = new HashMap<>();
        Set<LdmFieldPhysicalStorageMapping> mappings = new HashSet<>();
        mappings.add(TestModelUtils.ldmFieldPhysicalMappingEmpty(1L));
        fieldPhysicalMapping.put("test-field", mappings);
        
        when(readService.getByIdWithAssociationsCurrentVersion(anyLong())).thenReturn(existingEntity);
        when(fieldService.getSystemFieldPhysicalMapping(existingEntity)).thenReturn(fieldPhysicalMapping);
        when(repository.save(any(LdmEntity.class))).thenReturn(newEntity);
        
        // Act
        versioningService.saveAsNewVersion(newEntity, changeRequestId, false);
        
        // Assert
        verify(repository).save(entityCaptor.capture());
        verify(errHandlingStorageService).saveErrorHandlingStorageMappings(
                eq(newEntity.getId()), 
                eq(existingEntity.getVersion() + 1), 
                any(Set.class));
                
        LdmEntity capturedEntity = entityCaptor.getValue();
        assertThat(capturedEntity.getFields().iterator().next().getPhysicalStorageMapping()).isEqualTo(mappings);
    }

    @Test
    void saveAsNewVersion_withRollback_shouldNotCarryOverMappings() {
        // Arrange
        when(readService.getByIdWithAssociationsCurrentVersion(anyLong())).thenReturn(existingEntity);
        when(repository.save(any(LdmEntity.class))).thenReturn(newEntity);
        
        // Act
        versioningService.saveAsNewVersion(newEntity, changeRequestId, true);
        
        // Assert
        verify(fieldService, never()).getSystemFieldPhysicalMapping(any(LdmEntity.class));
    }

    @Test
    void saveVersion_shouldSaveEntityWithCorrectMetadata() {
        // Arrange
        Long entityId = 1L;
        Integer newVersion = 2;
        
        Set<LdmField> fields = new HashSet<>();
        LdmField field = TestModelUtils.ldmFieldEmpty();
        fields.add(field);
        newEntity.setFields(fields);
        
        Set<LdmErrorHandlingStorageMapping> errorMappings = new HashSet<>();
        errorMappings.add(TestModelUtils.ldmErrorHandlingStorageMappingEmpty(1L));
        newEntity.setErrorHandlingStorageMappings(errorMappings);
        
        when(repository.save(any(LdmEntity.class))).thenReturn(newEntity);
        
        // Act
        LdmEntity result = versioningService.saveVersion(entityId, newVersion, newEntity, changeRequestId);
        
        // Assert
        verify(repository).save(entityCaptor.capture());
        verify(fieldService).saveAll(eq(entityId), anySet());
        
        LdmEntity capturedEntity = entityCaptor.getValue();
        assertThat(capturedEntity.getId()).isEqualTo(entityId);
        assertThat(capturedEntity.getVersion()).isEqualTo(newVersion);
        assertThat(capturedEntity.getRequestId()).isEqualTo(changeRequestId);
        
        assertThat(result).isEqualTo(newEntity);
    }

    @Test
    void saveVersion_withNoFieldsOrMappings_shouldOnlySaveEntity() {
        // Arrange
        Long entityId = 1L;
        Integer newVersion = 2;
        newEntity.setFields(null);
        newEntity.setErrorHandlingStorageMappings(null);
        
        when(repository.save(any(LdmEntity.class))).thenReturn(newEntity);
        
        // Act
        LdmEntity result = versioningService.saveVersion(entityId, newVersion, newEntity, changeRequestId);
        
        // Assert
        verify(repository).save(any(LdmEntity.class));
        verify(fieldService, never()).saveAll(anyLong(), anySet());
        verify(errHandlingStorageService, never()).saveErrorHandlingStorageMappings(
            anyLong(), anyInt(), anySet());
        
        assertThat(result).isEqualTo(newEntity);
    }

    @Test
    void saveVersion_withEmptyFieldsAndMappings_shouldOnlySaveEntity() {
        // Arrange
        Long entityId = 1L;
        Integer newVersion = 2;
        newEntity.setFields(new HashSet<>());
        newEntity.setErrorHandlingStorageMappings(new HashSet<>());
        
        when(repository.save(any(LdmEntity.class))).thenReturn(newEntity);
        
        // Act
        LdmEntity result = versioningService.saveVersion(entityId, newVersion, newEntity, changeRequestId);
        
        // Assert
        verify(repository).save(any(LdmEntity.class));
        verify(fieldService, never()).saveAll(anyLong(), anySet());
        verify(errHandlingStorageService, never()).saveErrorHandlingStorageMappings(
            anyLong(), anyInt(), anySet());
        
        assertThat(result).isEqualTo(newEntity);
    }

    @Test
    void carryOverPhysicalMapping_withEmptyMapping_shouldNotModifyFields() {
        // Arrange
        Set<LdmField> fields = new HashSet<>();
        LdmField field = TestModelUtils.ldmFieldEmpty();
        field.setName("test-field");
        fields.add(field);
        newEntity.setFields(fields);
        
        when(readService.getByIdWithAssociationsCurrentVersion(anyLong())).thenReturn(existingEntity);
        when(fieldService.getSystemFieldPhysicalMapping(existingEntity)).thenReturn(new HashMap<>());
        when(repository.save(any(LdmEntity.class))).thenReturn(newEntity);
        
        // Act
        versioningService.saveAsNewVersion(newEntity, changeRequestId, false);
        
        // Assert
        assertThat(newEntity.getFields().iterator().next().getPhysicalStorageMapping()).isNull();
    }
}
