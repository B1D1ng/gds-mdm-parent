package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntityIndex;
import com.ebay.behavior.gds.mdm.dec.model.LdmErrorHandlingStorageMapping;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalAsset;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalStorage;
import com.ebay.behavior.gds.mdm.dec.model.enums.PlatformEnvironment;
import com.ebay.behavior.gds.mdm.dec.model.enums.StorageContext;
import com.ebay.behavior.gds.mdm.dec.repository.LdmErrorHandlingStorageMappingRepository;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("IT")
@Transactional
class LdmErrorHandlingStorageMappingServiceIT {

    @Autowired
    private LdmErrorHandlingStorageMappingService service;

    @Autowired
    private LdmErrorHandlingStorageMappingRepository repository;

    @Autowired
    private NamespaceService namespaceService;

    @Autowired
    private LdmBaseEntityService baseEntityService;

    @Autowired
    private LdmEntityIndexService indexService;

    @Autowired
    private LdmEntityService entityService;

    @Autowired
    private PhysicalAssetService assetService;

    @Autowired
    private PhysicalStorageService storageService;

    private LdmEntity entity;
    private PhysicalAsset asset;
    private PhysicalStorage productionStorage;
    private LdmErrorHandlingStorageMapping stagingMapping;
    private LdmErrorHandlingStorageMapping productionMapping;

    @BeforeEach
    void setUp() {
        // Create namespace
        Namespace namespace = namespaceService.create(TestModelUtils.namespace());

        // Create LDM base entity
        LdmBaseEntity baseEntity = TestModelUtils.ldmBaseEntity(namespace.getId());
        baseEntity = baseEntityService.create(baseEntity);

        // Create LDM entity index
        LdmEntityIndex entityIndex = TestModelUtils.ldmEntityIndex(baseEntity.getId());
        entityIndex = indexService.create(entityIndex);

        // Create LDM entity
        entity = TestModelUtils.ldmEntityEmptyWithBaseEntity(namespace.getId(), baseEntity.getId());
        entity.setId(entityIndex.getId());
        entity = entityService.create(entity);

        // Create physical asset
        asset = TestModelUtils.physicalAsset();
        asset = assetService.create(asset);

        // Create physical storages
        PhysicalStorage stagingStorage = TestModelUtils.physicalStorage();
        stagingStorage.setPhysicalAssetId(asset.getId());
        stagingStorage.setStorageEnvironment(PlatformEnvironment.STAGING);
        stagingStorage.setStorageContext(StorageContext.SYSTEM_ERROR_HANDLING);
        stagingStorage = storageService.create(stagingStorage);

        productionStorage = TestModelUtils.physicalStorage();
        productionStorage.setPhysicalAssetId(asset.getId());
        productionStorage.setStorageEnvironment(PlatformEnvironment.PRODUCTION);
        productionStorage.setStorageContext(StorageContext.SYSTEM_ERROR_HANDLING);
        productionStorage = storageService.create(productionStorage);

        // Create error handling mappings
        stagingMapping = new LdmErrorHandlingStorageMapping();
        stagingMapping.setLdmEntityId(entity.getId());
        stagingMapping.setLdmVersion(entity.getVersion());
        stagingMapping.setPhysicalStorageId(stagingStorage.getId());
        stagingMapping = repository.save(stagingMapping);

        productionMapping = new LdmErrorHandlingStorageMapping();
        productionMapping.setLdmEntityId(entity.getId());
        productionMapping.setLdmVersion(entity.getVersion());
        productionMapping.setPhysicalStorageId(productionStorage.getId());
        productionMapping = repository.save(productionMapping);
    }

    @Test
    void getAllByLdmEntityIdCurrentVersion_shouldReturnMappings() {
        // Act
        List<LdmErrorHandlingStorageMapping> mappings = service.getAllByLdmEntityIdCurrentVersion(entity.getId());

        // Assert
        assertThat(mappings).isNotNull();
        assertThat(mappings).hasSize(2);
        assertThat(mappings.stream().map(LdmErrorHandlingStorageMapping::getId))
                .containsExactlyInAnyOrder(stagingMapping.getId(), productionMapping.getId());
    }

    @Test
    void getAllByLdmEntityId_withLatestTrue_shouldReturnMappings() {
        // Act
        List<LdmErrorHandlingStorageMapping> mappings = service.getAllByLdmEntityIdCurrentVersion(entity.getId());

        // Assert
        assertThat(mappings).isNotNull();
        assertThat(mappings).hasSize(2);
        assertThat(mappings.stream().map(LdmErrorHandlingStorageMapping::getId))
                .containsExactlyInAnyOrder(stagingMapping.getId(), productionMapping.getId());
    }

    @Test
    void getAllByLdmEntityIdAndEnvironment_shouldFilterByEnvironment() {
        // Act
        List<LdmErrorHandlingStorageMapping> stagingMappings = service.getAllByLdmEntityIdAndEnvironment(
                entity.getId(), "STAGING", false);
        List<LdmErrorHandlingStorageMapping> productionMappings = service.getAllByLdmEntityIdAndEnvironment(
                entity.getId(), "PRODUCTION", false);

        // Assert
        assertThat(stagingMappings).isNotNull();
        assertThat(stagingMappings).hasSize(1);
        assertThat(stagingMappings.get(0).getId()).isEqualTo(stagingMapping.getId());

        assertThat(productionMappings).isNotNull();
        assertThat(productionMappings).hasSize(1);
        assertThat(productionMappings.get(0).getId()).isEqualTo(productionMapping.getId());
    }

    @Test
    void getAllByLdmEntityIdAndVersion_shouldReturnMappingsForVersion() {
        // Act
        List<LdmErrorHandlingStorageMapping> mappings = service.getAllByLdmEntityIdAndVersion(
                entity.getId(), entity.getVersion());

        // Assert
        assertThat(mappings).isNotNull();
        assertThat(mappings).hasSize(2);
        assertThat(mappings.stream().map(LdmErrorHandlingStorageMapping::getId))
                .containsExactlyInAnyOrder(stagingMapping.getId(), productionMapping.getId());
    }

    @Test
    void saveErrorHandlingStorageMappings_shouldUpdateMappings() {
        // Arrange
        // Create a new storage
        PhysicalStorage newStorage = TestModelUtils.physicalStorage();
        newStorage.setPhysicalAssetId(asset.getId());
        newStorage.setStorageEnvironment(PlatformEnvironment.STAGING);
        newStorage.setStorageContext(StorageContext.SYSTEM_ERROR_HANDLING_OVERLOAD);
        newStorage.setStorageDetails(
                "{\"table_name\": \"table_error_handling_storage\", \"hadoop_queue\":\"\", \"retention\":\"\", \"offline_schedule\":\"\"}");
        newStorage = storageService.create(newStorage);

        // Create new mappings set with one existing and one new mapping
        Set<LdmErrorHandlingStorageMapping> newMappings = new HashSet<>();
        
        // Keep production mapping
        LdmErrorHandlingStorageMapping keepMapping = new LdmErrorHandlingStorageMapping();
        keepMapping.setPhysicalStorageId(productionStorage.getId());
        newMappings.add(keepMapping);
        
        // Add new mapping
        LdmErrorHandlingStorageMapping addMapping = new LdmErrorHandlingStorageMapping();
        addMapping.setPhysicalStorageId(newStorage.getId());
        newMappings.add(addMapping);

        // Act
        List<LdmErrorHandlingStorageMapping> result = service.saveErrorHandlingStorageMappings(
                entity.getId(), entity.getVersion(), newMappings);

        // Assert
        // Verify the new mapping was saved
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1); // Only the new mapping is returned
        assertThat(result.get(0).getPhysicalStorageId()).isEqualTo(newStorage.getId());

        // Verify all current mappings
        List<LdmErrorHandlingStorageMapping> currentMappings = service.getAllByLdmEntityIdCurrentVersion(entity.getId());
        assertThat(currentMappings).hasSize(2);
        assertThat(currentMappings.stream().map(LdmErrorHandlingStorageMapping::getPhysicalStorageId))
                .containsExactlyInAnyOrder(productionStorage.getId(), newStorage.getId());
        
        // Verify staging mapping was deleted
        assertThat(currentMappings.stream().map(LdmErrorHandlingStorageMapping::getId))
                .doesNotContain(stagingMapping.getId());
    }
}
