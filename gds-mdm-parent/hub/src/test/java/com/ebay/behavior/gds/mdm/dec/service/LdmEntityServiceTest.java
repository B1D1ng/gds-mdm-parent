package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntityIndex;
import com.ebay.behavior.gds.mdm.dec.model.LdmField;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.model.enums.NamespaceType;
import com.ebay.behavior.gds.mdm.dec.model.enums.ViewType;
import com.ebay.behavior.gds.mdm.dec.repository.LdmEntityRepository;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LdmEntityServiceTest {

    @Mock
    private LdmEntityRepository repository;

    @Mock
    private LdmBaseEntityService baseEntityService;

    @Mock
    private LdmReadService readService;

    @Mock
    private LdmFieldService fieldService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private LdmEntityVersioningService versioningService;

    @Mock
    private LdmEntityBasicInfoService basicInfoService;

    @Mock
    private LdmEntityValidationService validationService;

    @Mock
    private LdmEntityIndexService indexService;

    @Mock
    private NamespaceService namespaceService;

    @Spy
    @InjectMocks
    private LdmEntityService service;

    private LdmBaseEntity baseEntity;
    private LdmEntity ldmEntity;
    private LdmEntityIndex ldmEntityIndex;

    @BeforeEach
    void setUp() {
        baseEntity = TestModelUtils.ldmBaseEntity(1L);
        baseEntity.setId(1L);
        baseEntity.setRevision(0);

        ldmEntity = TestModelUtils.ldmEntityEmpty(1L);
        ldmEntity.setId(1L);
        ldmEntity.setVersion(1);
        ldmEntity.setBaseEntityId(1L);

        ldmEntityIndex = TestModelUtils.ldmEntityIndex(1L);
        ldmEntityIndex.setId(1L);
    }

    @Test
    void getAllCurrentVersion_shouldReturnAllEntities() {
        // Arrange
        List<LdmEntity> expectedEntities = List.of(ldmEntity);
        when(readService.getAllCurrentVersion()).thenReturn(expectedEntities);

        // Act
        List<LdmEntity> result = service.getAllCurrentVersion();

        // Assert
        assertThat(result).isEqualTo(expectedEntities);
        verify(readService).getAllCurrentVersion();
    }

    @Test
    void getByIdCurrentVersion_shouldReturnEntity() {
        // Arrange
        when(readService.getByIdCurrentVersion(anyLong())).thenReturn(ldmEntity);

        // Act
        LdmEntity result = service.getByIdCurrentVersion(1L);

        // Assert
        assertThat(result).isEqualTo(ldmEntity);
        verify(readService).getByIdCurrentVersion(1L);
    }

    @Test
    void validateModelForUpdate_shouldCallValidationService() {
        // Act
        service.validateModelForUpdate(ldmEntity);

        // Assert
        verify(validationService).validateModelForUpdate(ldmEntity);
    }

    @Test
    void create_shouldInitializeAndSaveVersion() {
        // Arrange
        LdmEntity entityToCreate = TestModelUtils.ldmEntityEmpty(1L);
        entityToCreate.setViewType(null); // Test default ViewType.NONE

        when(indexService.initialize(any(LdmEntity.class))).thenReturn(ldmEntityIndex);
        when(versioningService.saveVersion(anyLong(), anyInt(), any(LdmEntity.class), any())).thenReturn(ldmEntity);

        // Act
        LdmEntity result = service.create(entityToCreate);

        // Assert
        assertThat(result).isEqualTo(ldmEntity);
        verify(indexService).initialize(any(LdmEntity.class));
        verify(versioningService).saveVersion(eq(1L), anyInt(), any(LdmEntity.class), any());
        assertThat(entityToCreate.getViewType()).isEqualTo(ViewType.NONE);
    }

    @Test
    void create_withViewType_shouldUseProvidedViewType() {
        // Arrange
        LdmEntity entityToCreate = TestModelUtils.ldmEntityEmpty(1L);
        entityToCreate.setViewType(ViewType.RAW);

        when(indexService.initialize(any(LdmEntity.class))).thenReturn(ldmEntityIndex);
        when(versioningService.saveVersion(anyLong(), anyInt(), any(LdmEntity.class), any())).thenReturn(ldmEntity);

        // Act
        LdmEntity result = service.create(entityToCreate);

        // Assert
        assertThat(result).isEqualTo(ldmEntity);
        verify(indexService).initialize(any(LdmEntity.class));
        verify(versioningService).saveVersion(eq(1L), anyInt(), any(LdmEntity.class), any());
        assertThat(entityToCreate.getViewType()).isEqualTo(ViewType.RAW);
    }

    @Test
    void update_shouldUpdateEntityWithFields() {
        // Arrange
        LdmEntity existingEntity = TestModelUtils.ldmEntityEmpty(1L);
        existingEntity.setId(1L);
        existingEntity.setVersion(1);
        existingEntity.setBaseEntityId(1L);
        existingEntity.setCreateBy("user1");
        existingEntity.setCreateDate(java.sql.Timestamp.valueOf("2023-01-01 00:00:00"));

        LdmEntity entityToUpdate = TestModelUtils.ldmEntityEmpty(1L);
        entityToUpdate.setId(1L);
        entityToUpdate.setVersion(1);
        entityToUpdate.setDescription("Updated description");
        
        Set<LdmField> fields = new HashSet<>();
        fields.add(TestModelUtils.ldmFieldEmpty());
        entityToUpdate.setFields(fields);

        when(readService.getByIdCurrentVersion(anyLong())).thenReturn(existingEntity);
        when(repository.save(any(LdmEntity.class))).thenReturn(entityToUpdate);

        // Act
        LdmEntity result = service.update(entityToUpdate);

        // Assert
        assertThat(result).isEqualTo(entityToUpdate);
        verify(validationService).validateVersionAndRevision(entityToUpdate, existingEntity);
        verify(basicInfoService).handleBasicInfoUpdate(entityToUpdate, existingEntity.getBaseEntityId(), existingEntity.getViewType());
        verify(fieldService).updateFields(eq(1L), eq(existingEntity), eq(fields));
        verify(entityManager).detach(existingEntity);
        verify(repository).save(entityToUpdate);
        
        // Verify that base entity id, create user and time are preserved
        assertThat(entityToUpdate.getBaseEntityId()).isEqualTo(existingEntity.getBaseEntityId());
        assertThat(entityToUpdate.getCreateBy()).isEqualTo(existingEntity.getCreateBy());
        assertThat(entityToUpdate.getCreateDate()).isEqualTo(existingEntity.getCreateDate());
    }

    @Test
    void saveVersion_shouldDelegateToVersioningService() {
        // Arrange
        when(versioningService.saveVersion(anyLong(), anyInt(), any(LdmEntity.class), anyLong())).thenReturn(ldmEntity);

        // Act
        LdmEntity result = service.saveVersion(1L, 2, ldmEntity, 123L);

        // Assert
        assertThat(result).isEqualTo(ldmEntity);
        verify(versioningService).saveVersion(1L, 2, ldmEntity, 123L);
    }

    @Test
    void saveAsNewVersion_shouldDelegateToVersioningService() {
        // Arrange
        when(versioningService.saveAsNewVersion(any(LdmEntity.class), anyLong(), anyBoolean())).thenReturn(ldmEntity);

        // Act
        LdmEntity result = service.saveAsNewVersion(ldmEntity, 123L, false);

        // Assert
        assertThat(result).isEqualTo(ldmEntity);
        verify(versioningService).saveAsNewVersion(ldmEntity, 123L, false);
    }

    @Test
    void updateFields_shouldUpdateFieldsAndReturnUpdatedSet() {
        // Arrange
        Set<LdmField> fields = new HashSet<>();
        fields.add(TestModelUtils.ldmFieldEmpty());
        
        when(readService.getByIdCurrentVersion(anyLong())).thenReturn(ldmEntity);
        when(fieldService.updateFields(anyLong(), any(LdmEntity.class), anySet())).thenReturn(fields);

        // Act
        Set<LdmField> result = service.updateFields(1L, fields);

        // Assert
        assertThat(result).isEqualTo(fields);
        verify(readService).getByIdCurrentVersion(1L);
        verify(fieldService).updateFields(eq(1L), eq(ldmEntity), eq(fields));
    }

    @Test
    void updateBaseEntity_namespaceChange_error() {
        // Arrange
        when(baseEntityService.getById(anyLong())).thenReturn(baseEntity);

        var updatedEntity = new LdmBaseEntity();
        BeanUtils.copyProperties(baseEntity, updatedEntity);
        updatedEntity.setNamespaceId(1000L);

        // Act & Assert
        assertThatThrownBy(() -> service.updateBaseEntity(updatedEntity)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateBaseEntity_nameChange_shouldUpdateAllViews() {
        // Arrange
        baseEntity.setName("OriginalName");
        
        LdmBaseEntity updatedEntity = new LdmBaseEntity();
        BeanUtils.copyProperties(baseEntity, updatedEntity);
        updatedEntity.setName("UpdatedName");
        updatedEntity.setUpdateBy("testUser");
        
        LdmEntity ldmView = TestModelUtils.ldmEntityEmpty(1L);
        ldmView.setName("OriginalName_RAW");
        ldmView.setViewType(ViewType.RAW);
        
        List<LdmEntity> ldmViews = List.of(ldmView);
        
        when(baseEntityService.getById(anyLong())).thenReturn(baseEntity);
        // Mock the baseEntityService.validateName method
        doNothing().when(baseEntityService).validateName(anyString(), anyLong());

        when(readService.getByEntityIdWithAssociations(anyLong(), any())).thenReturn(ldmViews);
        when(baseEntityService.update(any(LdmBaseEntity.class))).thenReturn(updatedEntity);
        
        // Mock the service.saveAsNewVersion method to avoid the EntityUtils.copyLdm static method issue
        doReturn(ldmView).when(service).saveAsNewVersion(any(LdmEntity.class), isNull(), eq(false));
        
        // Act
        LdmBaseEntity result = service.updateBaseEntity(updatedEntity);

        // Assert
        assertThat(result).isEqualTo(updatedEntity);
        verify(baseEntityService).validateName("UpdatedName", 1L);
        verify(readService).getByEntityIdWithAssociations(1L, null);
        verify(service).saveAsNewVersion(any(LdmEntity.class), isNull(), eq(false));
        verify(baseEntityService).update(updatedEntity);
    }

    @Test
    void updateBaseEntity_noNameChange_shouldJustUpdate() {
        // Arrange
        baseEntity.setName("SameName");
        
        LdmBaseEntity updatedEntity = new LdmBaseEntity();
        BeanUtils.copyProperties(baseEntity, updatedEntity);
        updatedEntity.setName("SameName");
        updatedEntity.setDescription("Updated description");
        
        when(baseEntityService.getById(anyLong())).thenReturn(baseEntity);
        when(baseEntityService.update(any(LdmBaseEntity.class))).thenReturn(updatedEntity);
        
        // Act
        LdmBaseEntity result = service.updateBaseEntity(updatedEntity);

        // Assert
        assertThat(result).isEqualTo(updatedEntity);
        verify(baseEntityService, never()).validateName(anyString(), anyLong());
        verify(readService, never()).getByEntityIdWithAssociations(anyLong(), any());
        verify(baseEntityService).update(updatedEntity);
    }

    @Test
    void validateAndInitializeLdm_nonDcsLdm_lowercaseName() {
        LdmEntity entity = TestModelUtils.ldmEntityEmpty(1L);
        entity.setName("TEST");
        service.validateAndInitializeLdm(entity);
        assertThat(entity.getName()).isEqualTo("test");
    }
}
