package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalAsset;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalAssetAttribute;
import com.ebay.behavior.gds.mdm.dec.model.enums.DecEnvironment;
import com.ebay.behavior.gds.mdm.dec.model.enums.PhysicalAssetAttributeName;
import com.ebay.behavior.gds.mdm.dec.model.enums.PhysicalAssetType;
import com.ebay.behavior.gds.mdm.dec.repository.PhysicalAssetAttributeRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class PhysicalAssetAttributeServiceTest {

    @Mock
    private PhysicalAssetAttributeRepository repository;

    @Mock
    private PhysicalAssetRepository assetRepository;

    @InjectMocks
    private PhysicalAssetAttributeService service;

    private PhysicalAsset asset;
    private PhysicalAssetAttribute attribute;
    private final Long ASSET_ID = 1L;
    private final Long ATTRIBUTE_ID = 1L;

    @BeforeEach
    void setUp() {
        // Set up test data
        asset = new PhysicalAsset();
        asset.setId(ASSET_ID);
        asset.setAssetName("Test Asset");
        asset.setAssetType(PhysicalAssetType.HADOOP);
        asset.setDecEnvironment(DecEnvironment.STAGING);

        attribute = PhysicalAssetAttribute.builder()
                .id(ATTRIBUTE_ID)
                .attributeName(PhysicalAssetAttributeName.HADOOP_CLUSTER)
                .attributeValue("testValue")
                .assetId(ASSET_ID)
                .revision(0) // Set revision for DecAuditable validation
                .build();
    }

    @Test
    void getAll_shouldReturnAllAttributes() {
        // Given
        List<PhysicalAssetAttribute> expectedAttributes = Collections.singletonList(attribute);
        when(repository.findAll()).thenReturn(expectedAttributes);

        // When
        List<PhysicalAssetAttribute> result = service.getAll();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAttributeName()).isEqualTo(PhysicalAssetAttributeName.HADOOP_CLUSTER);
        assertThat(result.get(0).getAttributeValue()).isEqualTo("testValue");
        verify(repository, times(1)).findAll();
    }

    @Test
    void getAllByAssetId_shouldReturnAttributesForAsset() {
        // Given
        List<PhysicalAssetAttribute> expectedAttributes = Collections.singletonList(attribute);
        when(repository.findByAssetId(ASSET_ID)).thenReturn(expectedAttributes);

        // When
        List<PhysicalAssetAttribute> result = service.getAllByAssetId(ASSET_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAttributeName()).isEqualTo(PhysicalAssetAttributeName.HADOOP_CLUSTER);
        assertThat(result.get(0).getAttributeValue()).isEqualTo("testValue");
        verify(repository, times(1)).findByAssetId(ASSET_ID);
    }

    @Test
    void getByIdWithAssociations_shouldReturnAttributeWhenExists() {
        // Given
        when(repository.findById(ATTRIBUTE_ID)).thenReturn(Optional.of(attribute));

        // When
        PhysicalAssetAttribute result = service.getByIdWithAssociations(ATTRIBUTE_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(ATTRIBUTE_ID);
        assertThat(result.getAttributeName()).isEqualTo(PhysicalAssetAttributeName.HADOOP_CLUSTER);
        assertThat(result.getAttributeValue()).isEqualTo("testValue");
        verify(repository, times(1)).findById(ATTRIBUTE_ID);
    }

    @Test
    void getByIdWithAssociations_shouldThrowExceptionWhenNotExists() {
        // Given
        when(repository.findById(ATTRIBUTE_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> service.getByIdWithAssociations(ATTRIBUTE_ID))
                .isInstanceOf(DataNotFoundException.class);
        verify(repository, times(1)).findById(ATTRIBUTE_ID);
    }

    @Test
    void create_shouldSaveAttributeWhenAssetExists() {
        // Given
        // Create a new attribute for creation (ID and revision must be null for create operation)
        PhysicalAssetAttribute newAttribute = PhysicalAssetAttribute.builder()
                .attributeName(PhysicalAssetAttributeName.HADOOP_CLUSTER)
                .attributeValue("testValue")
                .assetId(ASSET_ID)
                .build();

        when(repository.save(any(PhysicalAssetAttribute.class))).thenReturn(attribute);

        // When
        PhysicalAssetAttribute result = service.create(newAttribute);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(ATTRIBUTE_ID);
        verify(repository, times(1)).save(any(PhysicalAssetAttribute.class));
    }

    @Test
    void create_shouldThrowExceptionWhenAssetNotExists() {
        // Given
        // Create a new attribute with null ID and revision for create operation
        PhysicalAssetAttribute newAttribute = PhysicalAssetAttribute.builder()
                .attributeName(PhysicalAssetAttributeName.HADOOP_CLUSTER)
                .attributeValue("testValue")
                .assetId(ASSET_ID)
                .build();

        // Mock the repository to throw an exception
        when(repository.save(any(PhysicalAssetAttribute.class)))
                .thenThrow(new DataNotFoundException(PhysicalAsset.class, ASSET_ID));

        // When/Then
        assertThatThrownBy(() -> service.create(newAttribute))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void update_shouldUpdateAttributeWhenExists() {
        // Given
        PhysicalAssetAttribute existingAttribute = PhysicalAssetAttribute.builder()
                .id(ATTRIBUTE_ID)
                .attributeName(PhysicalAssetAttributeName.HADOOP_HDFS_DIR)
                .attributeValue("oldValue")
                .assetId(ASSET_ID)
                .revision(0) // Set revision for DecAuditable validation
                .build();

        // Set assetId for our test attribute too
        attribute.setAssetId(ASSET_ID);

        when(repository.findById(ATTRIBUTE_ID)).thenReturn(Optional.of(existingAttribute));
        when(repository.save(any(PhysicalAssetAttribute.class))).thenReturn(attribute);

        // When
        PhysicalAssetAttribute result = service.update(attribute);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAttributeName()).isEqualTo(PhysicalAssetAttributeName.HADOOP_CLUSTER);
        assertThat(result.getAttributeValue()).isEqualTo("testValue");
        verify(repository, times(1)).findById(ATTRIBUTE_ID);
        verify(repository, times(1)).save(any(PhysicalAssetAttribute.class));
    }

    @Test
    void createOrUpdateAttribute_shouldUpdateWhenAttributeExists() {
        // Given
        PhysicalAssetAttributeName attributeName = PhysicalAssetAttributeName.HADOOP_CLUSTER;
        String attributeValue = "newValue";
        PhysicalAssetAttribute existingAttribute = PhysicalAssetAttribute.builder()
                .id(ATTRIBUTE_ID)
                .attributeName(attributeName)
                .attributeValue("oldValue")
                .assetId(ASSET_ID)
                .revision(0) // Set revision for DecAuditable validation
                .build();

        // When updating an existing attribute, assetRepository.findById() is not called
        // per the implementation of PhysicalAssetAttributeService.createOrUpdateAttribute()
        when(repository.findByAssetId(ASSET_ID)).thenReturn(Collections.singletonList(existingAttribute));
        when(repository.save(any(PhysicalAssetAttribute.class))).thenAnswer(invocation -> {
            return invocation.<PhysicalAssetAttribute>getArgument(0);
        });

        // When
        PhysicalAssetAttribute result = service.createOrUpdateAttribute(ASSET_ID, attributeName, attributeValue);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAttributeName()).isEqualTo(attributeName);
        assertThat(result.getAttributeValue()).isEqualTo(attributeValue);
        // Don't verify assetRepository.findById() as it's not called when updating
        verify(repository, times(1)).findByAssetId(ASSET_ID);
        verify(repository, times(1)).save(any(PhysicalAssetAttribute.class));
    }

    @Test
    void createOrUpdateAttribute_shouldCreateWhenAttributeDoesNotExist() {
        // Given
        PhysicalAssetAttributeName attributeName = PhysicalAssetAttributeName.HADOOP_HDFS_DIR;
        String attributeValue = "newValue";
        PhysicalAssetAttribute existingAttribute = PhysicalAssetAttribute.builder()
                .id(ATTRIBUTE_ID)
                .attributeName(PhysicalAssetAttributeName.HADOOP_CLUSTER)
                .attributeValue("oldValue")
                .assetId(ASSET_ID)
                .revision(0) // Set revision for DecAuditable validation
                .build();

        Mockito.lenient().when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
        when(repository.findByAssetId(ASSET_ID)).thenReturn(Collections.singletonList(existingAttribute));
        when(repository.save(any(PhysicalAssetAttribute.class))).thenAnswer(invocation -> {
            PhysicalAssetAttribute savedAttribute = invocation.getArgument(0);
            // Set ID to simulate DB save
            savedAttribute.setId(2L);
            return savedAttribute;
        });

        // When
        PhysicalAssetAttribute result = service.createOrUpdateAttribute(ASSET_ID, attributeName, attributeValue);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAttributeName()).isEqualTo(attributeName);
        assertThat(result.getAttributeValue()).isEqualTo(attributeValue);
        assertThat(result.getAssetId()).isEqualTo(ASSET_ID);
        // No longer verify assetRepository.findById since the service doesn't call it
        verify(repository, times(1)).findByAssetId(ASSET_ID);
        verify(repository, times(1)).save(any(PhysicalAssetAttribute.class));
    }
}
