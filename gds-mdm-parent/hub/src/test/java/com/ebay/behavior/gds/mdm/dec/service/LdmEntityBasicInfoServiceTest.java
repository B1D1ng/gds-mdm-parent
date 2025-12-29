package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.model.enums.NamespaceType;
import com.ebay.behavior.gds.mdm.dec.model.enums.ViewType;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LdmEntityBasicInfoServiceTest {

    @Mock
    private LdmBaseEntityService baseEntityService;

    @Mock
    private NamespaceService namespaceService;

    @Mock
    private LdmEntityValidationService validationService;

    @InjectMocks
    private LdmEntityBasicInfoService basicInfoService;

    @Captor
    private ArgumentCaptor<LdmBaseEntity> baseEntityCaptor;

    private LdmEntity ldmEntity;
    private LdmBaseEntity baseEntity;
    private Namespace namespace;

    @BeforeEach
    void setUp() {
        ldmEntity = TestModelUtils.ldmEntityEmpty(1L);
        ldmEntity.setId(1L);
        ldmEntity.setName("test-entity");
        ldmEntity.setViewType(ViewType.RAW);
        ldmEntity.setNamespaceId(1L);
        ldmEntity.setTeam("Test Team");
        ldmEntity.setTeamDl("test-team@example.com");
        ldmEntity.setOwners("owner1,owner2");
        ldmEntity.setDomain("Test Domain");
        ldmEntity.setJiraProject("TEST");
        ldmEntity.setPk("ID");

        baseEntity = TestModelUtils.ldmBaseEntity(1L);
        baseEntity.setId(100L);
        baseEntity.setName("test-entity");
        baseEntity.setNamespaceId(1L);
        baseEntity.setTeam("Test Team");
        baseEntity.setTeamDl("test-team@example.com");
        baseEntity.setOwners("owner1,owner2");
        baseEntity.setDomain("Test Domain");
        baseEntity.setJiraProject("TEST");
        baseEntity.setPk("ID");

        namespace = TestModelUtils.namespace();
        namespace.setId(1L);
        namespace.setType(NamespaceType.DOMAIN);
    }

    @Test
    void handleBasicInfoUpdate_whenNamespaceTypeIsBase_shouldDoNothing() {
        // Arrange
        namespace.setType(NamespaceType.BASE);
        
        when(baseEntityService.getById(anyLong())).thenReturn(baseEntity);
        when(namespaceService.getById(anyLong())).thenReturn(namespace);

        // Act
        basicInfoService.handleBasicInfoUpdate(ldmEntity, baseEntity.getId(), ViewType.RAW);

        // Assert
        verify(baseEntityService, never()).update(any(LdmBaseEntity.class));
        verify(validationService, never()).validateName(anyString(), any(ViewType.class), anyLong());
    }

    @Test
    void handleBasicInfoUpdate_whenNoChanges_shouldDoNothing() {
        // Arrange
        ldmEntity.setName(baseEntity.getName());
        ldmEntity.setNamespaceId(baseEntity.getNamespaceId());
        ldmEntity.setTeam(baseEntity.getTeam());
        ldmEntity.setTeamDl(baseEntity.getTeamDl());
        ldmEntity.setOwners(baseEntity.getOwners());
        ldmEntity.setDomain(baseEntity.getDomain());
        ldmEntity.setJiraProject(baseEntity.getJiraProject());
        ldmEntity.setPk(baseEntity.getPk());
        ldmEntity.setViewType(ViewType.RAW);
        
        when(baseEntityService.getById(anyLong())).thenReturn(baseEntity);
        when(namespaceService.getById(anyLong())).thenReturn(namespace);

        // Act
        basicInfoService.handleBasicInfoUpdate(ldmEntity, baseEntity.getId(), ViewType.RAW);

        // Assert
        verify(baseEntityService, never()).update(any(LdmBaseEntity.class));
        verify(validationService, never()).validateName(anyString(), any(ViewType.class), anyLong());
    }

    @Test
    void handleBasicInfoUpdate_whenNameChanged_shouldValidateAndUpdate() {
        // Arrange
        ldmEntity.setName("new-name");
        
        when(baseEntityService.getById(anyLong())).thenReturn(baseEntity);
        when(namespaceService.getById(anyLong())).thenReturn(namespace);

        // Act
        basicInfoService.handleBasicInfoUpdate(ldmEntity, baseEntity.getId(), ViewType.RAW);

        // Assert
        verify(validationService).validateName(eq(ldmEntity.getName()), eq(ldmEntity.getViewType()), eq(ldmEntity.getNamespaceId()));
        verify(baseEntityService).update(baseEntityCaptor.capture());
        
        LdmBaseEntity updatedBaseEntity = baseEntityCaptor.getValue();
        assertThat(updatedBaseEntity.getName()).isEqualTo(ldmEntity.getName());
    }

    @Test
    void handleBasicInfoUpdate_whenNamespaceIdChanged_shouldValidateAndUpdate() {
        // Arrange
        ldmEntity.setNamespaceId(2L);
        
        when(baseEntityService.getById(anyLong())).thenReturn(baseEntity);
        when(namespaceService.getById(anyLong())).thenReturn(namespace);

        // Act
        basicInfoService.handleBasicInfoUpdate(ldmEntity, baseEntity.getId(), ViewType.RAW);

        // Assert
        verify(validationService).validateName(eq(ldmEntity.getName()), eq(ldmEntity.getViewType()), eq(ldmEntity.getNamespaceId()));
        verify(baseEntityService).update(baseEntityCaptor.capture());
        
        LdmBaseEntity updatedBaseEntity = baseEntityCaptor.getValue();
        assertThat(updatedBaseEntity.getNamespaceId()).isEqualTo(ldmEntity.getNamespaceId());
    }

    @Test
    void handleBasicInfoUpdate_whenViewTypeChanged_shouldValidateAndNotUpdateBaseEntity() {
        // Arrange
        ldmEntity.setViewType(ViewType.SNAPSHOT);
        
        when(baseEntityService.getById(anyLong())).thenReturn(baseEntity);
        when(namespaceService.getById(anyLong())).thenReturn(namespace);

        // Act
        basicInfoService.handleBasicInfoUpdate(ldmEntity, baseEntity.getId(), ViewType.RAW);

        // Assert
        verify(validationService).validateName(eq(ldmEntity.getName()), eq(ldmEntity.getViewType()), eq(ldmEntity.getNamespaceId()));
        verify(baseEntityService, never()).update(any(LdmBaseEntity.class));
    }

    @Test
    void handleBasicInfoUpdate_whenTeamDlChanged_shouldUpdateBaseEntity() {
        // Arrange
        ldmEntity.setTeamDl("new-team@example.com");
        
        when(baseEntityService.getById(anyLong())).thenReturn(baseEntity);
        when(namespaceService.getById(anyLong())).thenReturn(namespace);

        // Act
        basicInfoService.handleBasicInfoUpdate(ldmEntity, baseEntity.getId(), ViewType.RAW);

        // Assert
        verify(validationService, never()).validateName(anyString(), any(ViewType.class), anyLong());
        verify(baseEntityService).update(baseEntityCaptor.capture());
        
        LdmBaseEntity updatedBaseEntity = baseEntityCaptor.getValue();
        assertThat(updatedBaseEntity.getTeamDl()).isEqualTo(ldmEntity.getTeamDl());
    }

    @Test
    void handleBasicInfoUpdate_whenOwnersChanged_shouldUpdateBaseEntity() {
        // Arrange
        ldmEntity.setOwners("new-owner1,new-owner2");
        
        when(baseEntityService.getById(anyLong())).thenReturn(baseEntity);
        when(namespaceService.getById(anyLong())).thenReturn(namespace);

        // Act
        basicInfoService.handleBasicInfoUpdate(ldmEntity, baseEntity.getId(), ViewType.RAW);

        // Assert
        verify(validationService, never()).validateName(anyString(), any(ViewType.class), anyLong());
        verify(baseEntityService).update(baseEntityCaptor.capture());
        
        LdmBaseEntity updatedBaseEntity = baseEntityCaptor.getValue();
        assertThat(updatedBaseEntity.getOwners()).isEqualTo(ldmEntity.getOwners());
    }

    @Test
    void handleBasicInfoUpdate_whenDomainChanged_shouldUpdateBaseEntity() {
        // Arrange
        ldmEntity.setDomain("New Domain");
        
        when(baseEntityService.getById(anyLong())).thenReturn(baseEntity);
        when(namespaceService.getById(anyLong())).thenReturn(namespace);

        // Act
        basicInfoService.handleBasicInfoUpdate(ldmEntity, baseEntity.getId(), ViewType.RAW);

        // Assert
        verify(validationService, never()).validateName(anyString(), any(ViewType.class), anyLong());
        verify(baseEntityService).update(baseEntityCaptor.capture());
        
        LdmBaseEntity updatedBaseEntity = baseEntityCaptor.getValue();
        assertThat(updatedBaseEntity.getDomain()).isEqualTo(ldmEntity.getDomain());
    }

    @Test
    void handleBasicInfoUpdate_whenJiraProjectChanged_shouldUpdateBaseEntity() {
        // Arrange
        ldmEntity.setJiraProject("NEW");
        
        when(baseEntityService.getById(anyLong())).thenReturn(baseEntity);
        when(namespaceService.getById(anyLong())).thenReturn(namespace);

        // Act
        basicInfoService.handleBasicInfoUpdate(ldmEntity, baseEntity.getId(), ViewType.RAW);

        // Assert
        verify(validationService, never()).validateName(anyString(), any(ViewType.class), anyLong());
        verify(baseEntityService).update(baseEntityCaptor.capture());
        
        LdmBaseEntity updatedBaseEntity = baseEntityCaptor.getValue();
        assertThat(updatedBaseEntity.getJiraProject()).isEqualTo(ldmEntity.getJiraProject());
    }

    @Test
    void handleBasicInfoUpdate_whenPkChanged_shouldUpdateBaseEntity() {
        // Arrange
        ldmEntity.setPk("NEW_ID");
        
        when(baseEntityService.getById(anyLong())).thenReturn(baseEntity);
        when(namespaceService.getById(anyLong())).thenReturn(namespace);

        // Act
        basicInfoService.handleBasicInfoUpdate(ldmEntity, baseEntity.getId(), ViewType.RAW);

        // Assert
        verify(validationService, never()).validateName(anyString(), any(ViewType.class), anyLong());
        verify(baseEntityService).update(baseEntityCaptor.capture());
        
        LdmBaseEntity updatedBaseEntity = baseEntityCaptor.getValue();
        assertThat(updatedBaseEntity.getPk()).isEqualTo(ldmEntity.getPk());
    }

    @Test
    void handleBasicInfoUpdate_whenMultipleFieldsChanged_shouldUpdateAllChangedFields() {
        // Arrange
        ldmEntity.setName("new-name");
        ldmEntity.setTeam("New Team");
        ldmEntity.setDomain("New Domain");
        
        when(baseEntityService.getById(anyLong())).thenReturn(baseEntity);
        when(namespaceService.getById(anyLong())).thenReturn(namespace);

        // Act
        basicInfoService.handleBasicInfoUpdate(ldmEntity, baseEntity.getId(), ViewType.RAW);

        // Assert
        verify(validationService).validateName(eq(ldmEntity.getName()), eq(ldmEntity.getViewType()), eq(ldmEntity.getNamespaceId()));
        verify(baseEntityService).update(baseEntityCaptor.capture());
        
        LdmBaseEntity updatedBaseEntity = baseEntityCaptor.getValue();
        assertThat(updatedBaseEntity.getName()).isEqualTo(ldmEntity.getName());
        assertThat(updatedBaseEntity.getTeam()).isEqualTo(ldmEntity.getTeam());
        assertThat(updatedBaseEntity.getDomain()).isEqualTo(ldmEntity.getDomain());
        // Other fields should remain unchanged
        assertThat(updatedBaseEntity.getTeamDl()).isEqualTo(baseEntity.getTeamDl());
        assertThat(updatedBaseEntity.getOwners()).isEqualTo(baseEntity.getOwners());
        assertThat(updatedBaseEntity.getJiraProject()).isEqualTo(baseEntity.getJiraProject());
        assertThat(updatedBaseEntity.getPk()).isEqualTo(baseEntity.getPk());
    }

}
