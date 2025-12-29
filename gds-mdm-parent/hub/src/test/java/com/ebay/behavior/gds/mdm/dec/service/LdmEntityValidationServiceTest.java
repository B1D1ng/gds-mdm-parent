package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.enums.ViewType;
import com.ebay.behavior.gds.mdm.dec.repository.LdmEntityRepository;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LdmEntityValidationServiceTest {

    @Mock
    private LdmEntityRepository repository;

    @InjectMocks
    private LdmEntityValidationService validationService;

    private LdmEntity entity;
    private LdmEntity existingEntity;

    @BeforeEach
    void setUp() {
        entity = TestModelUtils.ldmEntityEmpty(1L);
        entity.setId(1L);
        entity.setVersion(1);
        entity.setRevision(1);
        entity.setName("test-entity");
        entity.setViewType(ViewType.RAW);
        entity.setNamespaceId(1L);

        existingEntity = TestModelUtils.ldmEntityEmpty(1L);
        existingEntity.setId(1L);
        existingEntity.setVersion(1);
        existingEntity.setRevision(1);
        existingEntity.setName("test-entity");
        existingEntity.setViewType(ViewType.RAW);
        existingEntity.setNamespaceId(1L);
    }

    @Test
    void validateName_whenNameDoesNotExist_shouldNotThrowException() {
        // Arrange
        String name = "unique-name";
        ViewType viewType = ViewType.RAW;
        Long namespaceId = 1L;
        
        when(repository.findAllByNameAndTypeAndNamespaceIdCurrentVersion(eq(name), eq(viewType), eq(namespaceId)))
            .thenReturn(Collections.emptyList());

        // Act & Assert
        validationService.validateName(name, viewType, namespaceId);
        
        // Verify repository was called with correct parameters
        verify(repository).findAllByNameAndTypeAndNamespaceIdCurrentVersion(name, viewType, namespaceId);
    }

    @Test
    void validateName_whenNameExists_shouldThrowException() {
        // Arrange
        String name = "existing-name";
        ViewType viewType = ViewType.RAW;
        Long namespaceId = 1L;
        
        LdmEntity existingEntity = TestModelUtils.ldmEntityEmpty(namespaceId);
        existingEntity.setName(name);
        existingEntity.setViewType(viewType);
        
        when(repository.findAllByNameAndTypeAndNamespaceIdCurrentVersion(eq(name), eq(viewType), eq(namespaceId)))
            .thenReturn(List.of(existingEntity));

        // Act & Assert
        assertThatThrownBy(() -> validationService.validateName(name, viewType, namespaceId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Entity with name " + name + " and type " + viewType + " already exists in namespace " + namespaceId);
        
        // Verify repository was called with correct parameters
        verify(repository).findAllByNameAndTypeAndNamespaceIdCurrentVersion(name, viewType, namespaceId);
    }

    @Test
    void validateModelForUpdate_shouldNotThrowException() {
        // Act & Assert
        validationService.validateModelForUpdate(entity);
        // This method is intentionally left empty in the implementation
        // Just verifying it doesn't throw an exception
    }

    @Test
    void validateVersionAndRevision_whenVersionsMatch_andRevisionsMatch_shouldNotThrowException() {
        // Arrange
        entity.setVersion(1);
        entity.setRevision(1);
        existingEntity.setVersion(1);
        existingEntity.setRevision(1);

        // Act & Assert
        validationService.validateVersionAndRevision(entity, existingEntity);
    }

    @Test
    void validateVersionAndRevision_whenVersionsDoNotMatch_shouldThrowException() {
        // Arrange
        entity.setVersion(2);
        entity.setRevision(1);
        existingEntity.setVersion(1);
        existingEntity.setRevision(1);

        // Act & Assert
        assertThatThrownBy(() -> validationService.validateVersionAndRevision(entity, existingEntity))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Version mismatch. Expected: 1, Actual: 2");
    }

    @Test
    void validateVersionAndRevision_whenVersionsMatch_butRevisionsDoNotMatch_shouldThrowException() {
        // Arrange
        entity.setVersion(1);
        entity.setRevision(2);
        existingEntity.setVersion(1);
        existingEntity.setRevision(1);

        // Act & Assert
        assertThatThrownBy(() -> validationService.validateVersionAndRevision(entity, existingEntity))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Revision mismatch. Expected: 1, Actual: 2");
    }

    @Test
    void validateVersionAndRevision_whenRevisionsAreNull_shouldNotThrowException() {
        // Arrange
        entity.setVersion(1);
        entity.setRevision(null);
        existingEntity.setVersion(1);
        existingEntity.setRevision(null);

        // Act & Assert
        validationService.validateVersionAndRevision(entity, existingEntity);
    }

    @Test
    void validateVersionAndRevision_whenOneRevisionIsNull_shouldNotThrowException() {
        // Arrange
        entity.setVersion(1);
        entity.setRevision(null);
        existingEntity.setVersion(1);
        existingEntity.setRevision(1);

        // Act & Assert
        validationService.validateVersionAndRevision(entity, existingEntity);
    }

    @Test
    void validateDcsModel_dcsFieldsIsEmpty_shouldThrowException() {
        entity.setDcsFields(null);
        assertThatThrownBy(() -> validationService.validateDcsModel(entity)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateDcsModel_nameDoesNotEndWithDeltaChangeStream_shouldThrowException() {
        entity.setDcsFields(List.of("field1"));
        entity.setName("invalidName");
        entity.setUpstreamLdm("123");
        assertThatThrownBy(() -> validationService.validateDcsModel(entity)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateDcsModel_upstreamLdmIsNull_shouldThrowException() {
        entity.setDcsFields(List.of("field1"));
        entity.setName("valid_DeltaChangeStream");
        entity.setUpstreamLdm(null);
        assertThatThrownBy(() -> validationService.validateDcsModel(entity)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateDcsModel_upstreamLdmIsEmpty_shouldThrowException() {
        entity.setDcsFields(List.of("field1"));
        entity.setName("valid_DeltaChangeStream");
        entity.setUpstreamLdm("");
        assertThatThrownBy(() -> validationService.validateDcsModel(entity)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateDcsModel_upstreamLdmHasMultipleElements_shouldThrowException() {
        entity.setDcsFields(List.of("field1"));
        entity.setName("valid_DeltaChangeStream");
        entity.setUpstreamLdm("123,456");
        assertThatThrownBy(() -> validationService.validateDcsModel(entity)).isInstanceOf(IllegalArgumentException.class);
    }
}
