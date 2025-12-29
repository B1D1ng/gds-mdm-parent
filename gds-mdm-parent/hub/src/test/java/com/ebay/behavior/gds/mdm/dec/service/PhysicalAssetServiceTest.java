package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.dec.model.PhysicalAsset;
import com.ebay.behavior.gds.mdm.dec.model.enums.DecEnvironment;
import com.ebay.behavior.gds.mdm.dec.model.enums.PhysicalAssetType;
import com.ebay.behavior.gds.mdm.dec.model.manyToMany.PhysicalAssetLdmMapping;
import com.ebay.behavior.gds.mdm.dec.repository.PhysicalAssetInfraMappingRepository;
import com.ebay.behavior.gds.mdm.dec.repository.PhysicalAssetInfraRepository;
import com.ebay.behavior.gds.mdm.dec.repository.PhysicalAssetLdmMappingRepository;
import com.ebay.behavior.gds.mdm.dec.repository.PhysicalAssetRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class PhysicalAssetServiceTest {

    @Mock
    private PhysicalAssetRepository assetRepository;

    @Mock
    private PhysicalAssetInfraRepository assetInfraRepository;

    @Mock
    private PhysicalAssetLdmMappingRepository mappingRepository;

    @Mock
    private PhysicalAssetInfraMappingRepository infraMappingRepository;

    @Mock
    private PhysicalStorageService storageService;

    @Mock
    private LdmBaseEntityService ldmBaseEntityService;

    @InjectMocks
    private PhysicalAssetService service;

    private Long testLdmId;
    private PhysicalAsset physicalAsset;
    private PhysicalAssetLdmMapping mapping;

    @BeforeEach
    void setUp() {
        testLdmId = 1L;

        // Create physical asset with STAGING environment
        physicalAsset = new PhysicalAsset();
        physicalAsset.setId(1L);
        physicalAsset.setAssetName("Test Asset");
        physicalAsset.setAssetType(PhysicalAssetType.HADOOP);
        physicalAsset.setDecEnvironment(DecEnvironment.STAGING);

        // Create mapping between LDM and physical asset using lenient stubbing
        // This prevents UnnecessaryStubbingException if not all tests use this stub
        mapping = mock(PhysicalAssetLdmMapping.class);
        Mockito.lenient().when(mapping.getPhysicalAsset()).thenReturn(physicalAsset);
    }

    @Test
    void getAllWithAssociationsByLdmIdAndPlatform_shouldReturnEmptyList_whenNoMatchingPlatform() {
        // Given
        List<PhysicalAssetLdmMapping> mappings = Collections.singletonList(mapping);
        when(mappingRepository.findByLdmBaseEntityId(testLdmId)).thenReturn(mappings);

        // When - request assets for PRODUCTION environment but our test asset is in STAGING
        List<PhysicalAsset> result = service.getAllWithAssociationsByLdmIdAndPlatform(testLdmId, DecEnvironment.PRODUCTION);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        // Verify the repository was called with correct ldmId
        verify(mappingRepository, times(1)).findByLdmBaseEntityId(testLdmId);

        // Verify that initializePhysicalAssetInfraMappings and initializeLdmIds were not called
        // since there were no assets matching the platform
        verify(infraMappingRepository, times(0)).findByPhysicalAssetId(physicalAsset.getId());
    }

    @Test
    void getAllWithAssociationsByLdmIdAndPlatform_shouldReturnAssets_whenMatchingPlatform() {
        // Given
        List<PhysicalAssetLdmMapping> mappings = Collections.singletonList(mapping);
        when(mappingRepository.findByLdmBaseEntityId(testLdmId)).thenReturn(mappings);

        // Mock empty results for both initialization methods
        when(infraMappingRepository.findByPhysicalAssetId(physicalAsset.getId()))
                .thenReturn(Collections.emptyList());
        when(mappingRepository.findByPhysicalAssetId(physicalAsset.getId()))
                .thenReturn(Collections.emptyList());

        // When - request assets for DEV environment which matches our test asset
        List<PhysicalAsset> result = service.getAllWithAssociationsByLdmIdAndPlatform(testLdmId, DecEnvironment.STAGING);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(physicalAsset);
        assertThat(result.get(0).getDecEnvironment()).isEqualTo(DecEnvironment.STAGING);

        // Verify repository calls
        verify(mappingRepository, times(1)).findByLdmBaseEntityId(testLdmId);

        // Verify that initialization methods were called
        verify(infraMappingRepository, times(1)).findByPhysicalAssetId(physicalAsset.getId());
        verify(mappingRepository, times(1)).findByPhysicalAssetId(physicalAsset.getId());
    }

    @Test
    void getAllWithAssociationsByLdmIdAndPlatform_shouldReturnEmptyList_whenNoMappingsExist() {
        // Given
        when(mappingRepository.findByLdmBaseEntityId(testLdmId)).thenReturn(Collections.emptyList());

        // When
        List<PhysicalAsset> result = service.getAllWithAssociationsByLdmIdAndPlatform(testLdmId, DecEnvironment.PRODUCTION);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        // Verify repository calls
        verify(mappingRepository, times(1)).findByLdmBaseEntityId(testLdmId);
    }
}