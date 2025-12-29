package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmField;
import com.ebay.behavior.gds.mdm.dec.model.LdmFieldPhysicalStorageMapping;
import com.ebay.behavior.gds.mdm.dec.model.LdmFieldSignalMapping;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalStorage;
import com.ebay.behavior.gds.mdm.dec.model.dto.LdmEntityWrapper;
import com.ebay.behavior.gds.mdm.dec.model.dto.SignalStorage;
import com.ebay.behavior.gds.mdm.dec.model.enums.PlatformEnvironment;
import com.ebay.behavior.gds.mdm.dec.model.enums.ViewType;
import com.ebay.behavior.gds.mdm.dec.repository.LdmBaseEntityRepository;
import com.ebay.behavior.gds.mdm.dec.repository.LdmEntityRepository;
import com.ebay.behavior.gds.mdm.dec.repository.LdmFieldPhysicalStorageMappingRepository;
import com.ebay.behavior.gds.mdm.dec.repository.PhysicalStorageRepository;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalPhysicalStorage;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedSignal;
import com.ebay.behavior.gds.mdm.signal.service.SignalPhysicalStorageService;

import com.ebay.behavior.gds.mdm.signal.service.StagedSignalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LdmReadServiceTest {

    @Mock
    private SignalPhysicalStorageService signalStorageService;
    
    @Mock
    private StagedSignalService stagedSignalService;

    @Mock
    private LdmEntityRepository repository;

    @Mock
    private LdmBaseEntityRepository baseEntityRepository;

    @Mock
    private PhysicalStorageRepository physicalStorageRepository;

    @Mock
    private LdmFieldPhysicalStorageMappingRepository fieldPhysicalMappingRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private LdmReadService service;

    private LdmEntity ldmEntity;
    private LdmBaseEntity baseEntity;
    private List<LdmFieldPhysicalStorageMapping> physicalMappings;
    private PhysicalStorage physicalStorage;
    private SignalPhysicalStorage signalPhysicalStorage;
    private StagedSignal stagedSignal;

    @BeforeEach
    void setUp() {
        // Create base entity
        baseEntity = TestModelUtils.ldmBaseEntity(1L);
        baseEntity.setId(1L);
        
        // Create LDM entity
        ldmEntity = TestModelUtils.ldmEntityEmpty(1L);
        ldmEntity.setId(1L);
        ldmEntity.setVersion(1);
        ldmEntity.setBaseEntityId(baseEntity.getId());
        
        // Create fields with signal mappings
        Set<LdmField> fields = new HashSet<>();
        LdmField field1 = TestModelUtils.ldmFieldEmpty();
        field1.setId(1L);
        field1.setLdmEntityId(ldmEntity.getId());
        field1.setLdmVersion(ldmEntity.getVersion());
        
        Set<LdmFieldSignalMapping> signalMappings = new HashSet<>();
        LdmFieldSignalMapping signalMapping = TestModelUtils.ldmFieldSignalMappingEmpty();
        signalMapping.setId(1L);
        signalMapping.setLdmFieldId(field1.getId());
        signalMapping.setSignalDefinitionId(100L);
        signalMapping.setSignalVersion(1);
        signalMappings.add(signalMapping);
        field1.setSignalMapping(signalMappings);
        
        fields.add(field1);
        ldmEntity.setFields(fields);
        
        // Create physical storage
        physicalStorage = TestModelUtils.physicalStorage();
        physicalStorage.setId(1L);
        physicalStorage.setStorageEnvironment(PlatformEnvironment.STAGING);
        
        // Create physical mappings
        physicalMappings = List.of(
            TestModelUtils.ldmFieldPhysicalMapping(field1.getId(), physicalStorage.getId())
        );
        
        // Create signal physical storage
        signalPhysicalStorage = new SignalPhysicalStorage();
        signalPhysicalStorage.setKafkaTopic("test-topic");
        signalPhysicalStorage.setKafkaSchema("test-schema");
        signalPhysicalStorage.setHiveTableName("test-table");
        signalPhysicalStorage.setDoneFilePath("/path/to/done");
        
        stagedSignal = StagedSignal.builder()
                .id(111L)
                .name("test-signal")
                .version(2)
                .environment(Environment.PRODUCTION)
                .build();
    }

    @Test
    void getAllCurrentVersion_shouldReturnAllEntities() {
        // Given
        when(repository.findAllCurrentVersion()).thenReturn(List.of(ldmEntity));
        
        // When
        List<LdmEntity> result = service.getAllCurrentVersion();
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(ldmEntity);
        verify(repository).findAllCurrentVersion();
    }

    @Test
    void getAllCurrentVersion_shouldReturnAllEntities_withLite() {
        // Given
        ldmEntity.setIr("irText");
        ldmEntity.setCodeContent("codeText");
        ldmEntity.setGeneratedSql("generatedSql");
        when(repository.findAllCurrentVersion()).thenReturn(List.of(ldmEntity));

        // When
        List<LdmEntity> result = service.getAllCurrentVersion(true);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(ldmEntity);
        assertThat(result.get(0).getCodeContent()).isNull();
        assertThat(result.get(0).getIr()).isNull();
        assertThat(result.get(0).getGeneratedSql()).isNull();
        verify(repository).findAllCurrentVersion();
    }

    @Test
    void getByIdCurrentVersion_shouldReturnEntity_whenExists() {
        // Given
        when(repository.findByIdCurrentVersion(ldmEntity.getId())).thenReturn(Optional.of(ldmEntity));
        
        // When
        LdmEntity result = service.getByIdCurrentVersion(ldmEntity.getId());
        
        // Then
        assertThat(result).isEqualTo(ldmEntity);
        verify(repository).findByIdCurrentVersion(ldmEntity.getId());
    }

    @Test
    void getByIdCurrentVersion_shouldThrowException_whenNotExists() {
        // Given
        when(repository.findByIdCurrentVersion(anyLong())).thenReturn(Optional.empty());
        
        // Then
        assertThatThrownBy(() -> service.getByIdCurrentVersion(999L))
            .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void getByEntityIdWithAssociations_shouldReturnEntitiesWithAssociations() {
        // Given
        when(repository.findByEntityIdCurrentVersion(baseEntity.getId())).thenReturn(List.of(ldmEntity));
        when(fieldPhysicalMappingRepository.findByLdmFieldId(anyLong())).thenReturn(physicalMappings);
        when(physicalStorageRepository.findById(anyLong())).thenReturn(Optional.of(physicalStorage));
        
        // When
        List<LdmEntity> result = service.getByEntityIdWithAssociations(baseEntity.getId(), "STAGING");
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(ldmEntity);
        assertThat(result.get(0).getFields()).isNotEmpty();
        verify(repository).findByEntityIdCurrentVersion(baseEntity.getId());
        verify(fieldPhysicalMappingRepository).findByLdmFieldId(anyLong());
    }

    @Test
    void getByEntityId_shouldReturnEntities() {
        // Given
        when(repository.findByEntityIdCurrentVersion(baseEntity.getId())).thenReturn(List.of(ldmEntity));
        
        // When
        List<LdmEntity> result = service.getByEntityId(baseEntity.getId());
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(ldmEntity);
        verify(repository).findByEntityIdCurrentVersion(baseEntity.getId());
    }

    @Test
    void getByIdWithAssociations_shouldReturnEntityWithAssociations() {
        // Given
        VersionedId versionedId = VersionedId.of(ldmEntity.getId(), ldmEntity.getVersion());
        when(repository.findById(versionedId)).thenReturn(Optional.of(ldmEntity));
        when(baseEntityRepository.findById(baseEntity.getId())).thenReturn(Optional.of(baseEntity));
        when(fieldPhysicalMappingRepository.findByLdmFieldId(anyLong())).thenReturn(physicalMappings);
        when(physicalStorageRepository.findById(anyLong())).thenReturn(Optional.of(physicalStorage));
        
        // When
        LdmEntity result = service.getByIdWithAssociations(versionedId, "STAGING");
        
        // Then
        assertThat(result).isEqualTo(ldmEntity);
        assertThat(result.getBaseEntity()).isEqualTo(baseEntity);
        assertThat(result.getFields()).isNotEmpty();
        verify(repository).findById(versionedId);
        verify(baseEntityRepository).findById(baseEntity.getId());
        verify(fieldPhysicalMappingRepository).findByLdmFieldId(anyLong());
    }

    @Test
    void getByIdWithAssociationsCurrentVersion_shouldReturnEntityWithAssociations() {
        // Given
        when(repository.findByIdCurrentVersion(ldmEntity.getId())).thenReturn(Optional.of(ldmEntity));
        when(baseEntityRepository.findById(baseEntity.getId())).thenReturn(Optional.of(baseEntity));
        when(fieldPhysicalMappingRepository.findByLdmFieldId(anyLong())).thenReturn(physicalMappings);
        when(physicalStorageRepository.findById(anyLong())).thenReturn(Optional.of(physicalStorage));
        
        // When
        LdmEntity result = service.getByIdWithAssociationsCurrentVersion(ldmEntity.getId(), "STAGING");
        
        // Then
        assertThat(result).isEqualTo(ldmEntity);
        assertThat(result.getBaseEntity()).isEqualTo(baseEntity);
        assertThat(result.getFields()).isNotEmpty();
        verify(repository).findByIdCurrentVersion(ldmEntity.getId());
        verify(baseEntityRepository).findById(baseEntity.getId());
        verify(fieldPhysicalMappingRepository).findByLdmFieldId(anyLong());
    }

    @Test
    void getByIdInWrapper_withCurrentVersion_shouldReturnWrapper() {
        // Given
        when(repository.findByIdCurrentVersion(ldmEntity.getId())).thenReturn(Optional.of(ldmEntity));
        when(baseEntityRepository.findById(baseEntity.getId())).thenReturn(Optional.of(baseEntity));
        when(fieldPhysicalMappingRepository.findByLdmFieldId(anyLong())).thenReturn(physicalMappings);
        when(physicalStorageRepository.findById(anyLong())).thenReturn(Optional.of(physicalStorage));
        when(stagedSignalService.getLatestVersionById(anyLong(), any())).thenReturn(stagedSignal);
        when(signalStorageService.getBySignalId(any(VersionedId.class))).thenReturn(signalPhysicalStorage);
        when(physicalStorageRepository.findAllById(anyList())).thenReturn(List.of(physicalStorage));
        
        // When
        LdmEntityWrapper result = service.getByIdInWrapper(ldmEntity.getId(), true, "STAGING");
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEntity()).isEqualTo(ldmEntity);
        assertThat(result.getPhysicalStorages()).isNotEmpty();
        assertThat(result.getSignalStorages()).isNotEmpty();
        verify(repository).findByIdCurrentVersion(ldmEntity.getId());
        verify(baseEntityRepository).findById(baseEntity.getId());
        verify(fieldPhysicalMappingRepository).findByLdmFieldId(anyLong());
        verify(signalStorageService).getBySignalId(any(VersionedId.class));
        verify(physicalStorageRepository).findAllById(anyList());
    }

    @Test
    void getByIdInWrapper_withVersionedId_shouldReturnWrapper() {
        // Given
        VersionedId versionedId = VersionedId.of(ldmEntity.getId(), ldmEntity.getVersion());
        when(repository.findById(versionedId)).thenReturn(Optional.of(ldmEntity));
        when(baseEntityRepository.findById(baseEntity.getId())).thenReturn(Optional.of(baseEntity));
        when(fieldPhysicalMappingRepository.findByLdmFieldId(anyLong())).thenReturn(physicalMappings);
        when(physicalStorageRepository.findById(anyLong())).thenReturn(Optional.of(physicalStorage));
        when(stagedSignalService.getLatestVersionById(anyLong(), any())).thenReturn(stagedSignal);
        when(signalStorageService.getBySignalId(any(VersionedId.class))).thenReturn(signalPhysicalStorage);
        when(physicalStorageRepository.findAllById(anyList())).thenReturn(List.of(physicalStorage));
        
        // When
        LdmEntityWrapper result = service.getByIdInWrapper(versionedId, true, "STAGING");
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEntity()).isEqualTo(ldmEntity);
        assertThat(result.getPhysicalStorages()).isNotEmpty();
        assertThat(result.getSignalStorages()).isNotEmpty();
        verify(repository).findById(versionedId);
        verify(baseEntityRepository).findById(baseEntity.getId());
        verify(fieldPhysicalMappingRepository).findByLdmFieldId(anyLong());
        verify(signalStorageService).getBySignalId(any(VersionedId.class));
        verify(physicalStorageRepository).findAllById(anyList());
    }

    @Test
    void getLdmEntityWrapper_withExtendedInfo_shouldReturnWrapper() {
        // Given
        when(stagedSignalService.getLatestVersionById(anyLong(), any())).thenReturn(stagedSignal);
        when(signalStorageService.getBySignalId(any(VersionedId.class))).thenReturn(signalPhysicalStorage);
        when(physicalStorageRepository.findAllById(anyList())).thenReturn(List.of(physicalStorage));
        
        // When
        LdmEntityWrapper result = service.getLdmEntityWrapper(ldmEntity, true);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEntity()).isEqualTo(ldmEntity);
        assertThat(result.getPhysicalStorages()).isNotEmpty();
        assertThat(result.getSignalStorages()).isNotEmpty();
        verify(signalStorageService).getBySignalId(any(VersionedId.class));
        verify(physicalStorageRepository).findAllById(anyList());
    }

    @Test
    void getLdmEntityWrapper_withoutExtendedInfo_shouldReturnSimpleWrapper() {
        // When
        LdmEntityWrapper result = service.getLdmEntityWrapper(ldmEntity, false);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEntity()).isEqualTo(ldmEntity);
        assertThat(result.getPhysicalStorages()).isNull();
        assertThat(result.getSignalStorages()).isNull();
        verifyNoInteractions(signalStorageService);
        verifyNoInteractions(physicalStorageRepository);
    }

    @Test
    void getSignalStorage_shouldHandleDataNotFoundException() {
        // Given

        when(stagedSignalService.getLatestVersionById(anyLong(), any())).thenReturn(stagedSignal);
        when(signalStorageService.getBySignalId(any(VersionedId.class))).thenThrow(new DataNotFoundException("SignalPhysicalStorage"));
        
        // Create a field with signal mapping for testing
        LdmField field = TestModelUtils.ldmFieldEmpty();
        field.setId(1L);
        
        Set<LdmFieldSignalMapping> signalMappings = new HashSet<>();
        LdmFieldSignalMapping signalMapping = TestModelUtils.ldmFieldSignalMappingEmpty();
        signalMapping.setId(1L);
        signalMapping.setLdmFieldId(field.getId());
        signalMapping.setSignalDefinitionId(100L);
        signalMapping.setSignalVersion(1);
        signalMappings.add(signalMapping);
        field.setSignalMapping(signalMappings);
        
        Set<LdmField> testFields = new HashSet<>();
        testFields.add(field);
        
        LdmEntity testEntity = TestModelUtils.ldmEntityEmpty(1L);
        testEntity.setId(1L);
        testEntity.setFields(testFields);
        
        // When
        LdmEntityWrapper result = service.getLdmEntityWrapper(testEntity, true);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEntity()).isEqualTo(testEntity);
        assertThat(result.getSignalStorages()).isNotEmpty();
        
        SignalStorage signalStorage = result.getSignalStorages().get(0);
        assertThat(signalStorage.getSignalDefinitionId()).isEqualTo("100");
        assertThat(signalStorage.getStorageDetails()).isNull();
    }

    @Test
    void searchByNameAndNamespace_withNameAndViewTypeAndNamespace_shouldSearch() {
        // Given
        when(repository.findAllByNameAndTypeAndNamespaceCurrentVersion(anyString(), any(ViewType.class), anyString()))
            .thenReturn(List.of(ldmEntity));
        
        // When
        List<LdmEntity> result = service.searchByNameAndNamespace("testName", "RAW", "testNamespace");
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(ldmEntity);
        verify(repository).findAllByNameAndTypeAndNamespaceCurrentVersion("testName", ViewType.RAW, "testNamespace");
    }

    @Test
    void searchByNameAndNamespace_withNameAndViewType_shouldSearch() {
        // Given
        when(repository.findAllByNameAndTypeCurrentVersion(anyString(), any(ViewType.class)))
            .thenReturn(List.of(ldmEntity));
        
        // When
        List<LdmEntity> result = service.searchByNameAndNamespace("testName", "RAW", null);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(ldmEntity);
        verify(repository).findAllByNameAndTypeCurrentVersion("testName", ViewType.RAW);
    }
    
    @Test
    void searchByNameAndNamespace_withNameAndViewType_shouldSearch_withLite() {
        // Given
        ldmEntity.setIr("irText");
        ldmEntity.setCodeContent("codeText");
        ldmEntity.setGeneratedSql("generatedSql");
        when(repository.findAllByNameAndTypeCurrentVersion(anyString(), any(ViewType.class)))
                .thenReturn(List.of(ldmEntity));

        // When
        List<LdmEntity> result = service.searchByNameAndNamespace("testName", "RAW", null, true);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(ldmEntity);

        assertThat(result.get(0).getCodeContent()).isNull();
        assertThat(result.get(0).getIr()).isNull();
        assertThat(result.get(0).getGeneratedSql()).isNull();
        verify(repository).findAllByNameAndTypeCurrentVersion("testName", ViewType.RAW);
    }

    @Test
    void searchByNameAndNamespace_withNameAndNamespace_shouldSearch() {
        // Given
        when(repository.findAllByNameAndNamespaceCurrentVersion(anyString(), anyString()))
            .thenReturn(List.of(ldmEntity));
        
        // When
        List<LdmEntity> result = service.searchByNameAndNamespace("testName", null, "testNamespace");
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(ldmEntity);
        verify(repository).findAllByNameAndNamespaceCurrentVersion("testName", "testNamespace");
    }

    @Test
    void searchByNameAndNamespace_withNameOnly_shouldSearch() {
        // Given
        when(repository.findAllByNameCurrentVersion(anyString()))
            .thenReturn(List.of(ldmEntity));
        
        // When
        List<LdmEntity> result = service.searchByNameAndNamespace("testName", null, null);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(ldmEntity);
        verify(repository).findAllByNameCurrentVersion("testName");
    }

    @Test
    void searchByNameAndNamespace_withNamespaceOnly_shouldSearch() {
        // Given
        when(repository.findAllByNamespaceNameCurrentVersion(anyString()))
            .thenReturn(List.of(ldmEntity));
        
        // When
        List<LdmEntity> result = service.searchByNameAndNamespace(null, null, "testNamespace");
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(ldmEntity);
        verify(repository).findAllByNamespaceNameCurrentVersion("testNamespace");
    }
    @Test
    void initializeAssociations_shouldSetDcsLdms_whenDcsEntitiesExist() {
        // Given
        when(baseEntityRepository.findById(baseEntity.getId())).thenReturn(Optional.of(baseEntity));
        when(fieldPhysicalMappingRepository.findByLdmFieldId(anyLong())).thenReturn(physicalMappings);
        when(physicalStorageRepository.findById(anyLong())).thenReturn(Optional.of(physicalStorage));

        // Create mock DCS entities
        LdmEntity dcsEntity1 = TestModelUtils.ldmEntityEmpty(10L);
        dcsEntity1.setId(10L);
        dcsEntity1.setVersion(1);
        dcsEntity1.setIsDcs(true);
        dcsEntity1.setUpstreamLdm("," + ldmEntity.getId() + ",");

        LdmEntity dcsEntity2 = TestModelUtils.ldmEntityEmpty(20L);
        dcsEntity2.setId(20L);
        dcsEntity2.setVersion(1);
        dcsEntity2.setIsDcs(true);
        dcsEntity2.setUpstreamLdm("," + ldmEntity.getId() + ",30,");

        // Mock repository to return DCS entities
        when(repository.findDcsByUpstreamLdmId(ldmEntity.getId()))
                .thenReturn(List.of(dcsEntity1, dcsEntity2));

        VersionedId versionedId = VersionedId.of(ldmEntity.getId(), ldmEntity.getVersion());
        when(repository.findById(versionedId)).thenReturn(Optional.of(ldmEntity));

        // When
        LdmEntity result = service.getByIdWithAssociations(versionedId, "STAGING");

        // Then
        assertThat(result.getDcsLdms()).isNotNull();
        assertThat(result.getDcsLdms()).hasSize(2);
        assertThat(result.getDcsLdms()).contains(10L, 20L);
        verify(repository).findDcsByUpstreamLdmId(ldmEntity.getId());
    }

    @Test
    void initializeAssociations_shouldSetEmptyDcsLdms_whenNoDcsEntitiesExist() {
        // Given
        when(baseEntityRepository.findById(baseEntity.getId())).thenReturn(Optional.of(baseEntity));
        when(fieldPhysicalMappingRepository.findByLdmFieldId(anyLong())).thenReturn(physicalMappings);
        when(physicalStorageRepository.findById(anyLong())).thenReturn(Optional.of(physicalStorage));

        // Mock repository to return empty list of DCS entities
        when(repository.findDcsByUpstreamLdmId(ldmEntity.getId())).thenReturn(List.of());

        VersionedId versionedId = VersionedId.of(ldmEntity.getId(), ldmEntity.getVersion());
        when(repository.findById(versionedId)).thenReturn(Optional.of(ldmEntity));

        // When
        LdmEntity result = service.getByIdWithAssociations(versionedId, "STAGING");

        // Then
        assertThat(result.getDcsLdms()).isNotNull();
        assertThat(result.getDcsLdms()).isEmpty();
        verify(repository).findDcsByUpstreamLdmId(ldmEntity.getId());
    }
}
