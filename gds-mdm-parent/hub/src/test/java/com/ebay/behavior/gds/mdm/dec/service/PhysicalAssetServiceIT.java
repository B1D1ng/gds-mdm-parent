package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalAsset;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalAssetInfra;
import com.ebay.behavior.gds.mdm.dec.model.manyToMany.PhysicalAssetLdmMapping;
import com.ebay.behavior.gds.mdm.dec.repository.PhysicalAssetLdmMappingRepository;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.CONTAINS;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.*;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestPhsicalAssetInfraUtils.physicalAssetInfra;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.*;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PhysicalAssetServiceIT {

    @Autowired
    private PhysicalAssetService service;

    @Autowired
    private PhysicalStorageService storageService;

    @Autowired
    private LdmBaseEntityService baseEntityService;

    @Autowired
    private NamespaceService namespaceService;

    @Autowired
    private PhysicalAssetInfraService infraService;

    @Autowired
    private PhysicalAssetLdmMappingRepository ldmMappingRepository;

    private PhysicalAsset asset;
    private Long assetId;
    private PhysicalAssetInfra infra;
    private Long ldmId;
    private Namespace namespace;

    @BeforeEach
    void setUp() {
        asset = physicalAsset();
        asset = service.create(asset);
        assetId = asset.getId();

        infra = physicalAssetInfra();

        namespace = namespace();
        namespace = namespaceService.create(namespace);
        var baseEntity = ldmBaseEntity(namespace.getId());
        baseEntity = baseEntityService.create(baseEntity);
        ldmId = baseEntity.getId();
        var physicalAssetLdmMapping = new PhysicalAssetLdmMapping(asset, baseEntity);
        ldmMappingRepository.save(physicalAssetLdmMapping);
    }

    @Test
    void getById() {
        var persisted = service.getById(assetId);

        assertThat(persisted.getId()).isEqualTo(assetId);
    }

    @Test
    void getByIdWithAssociations() {
        var persisted = service.getByIdWithAssociations(assetId);

        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getLdmIds().size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void getAllWithAssociationsByLdmId() {
        var persisted = service.getAllWithAssociationsByLdmId(ldmId);

        assertThat(persisted.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void create_NameConflict() {
        var asset2 = physicalAsset();
        asset2.setAssetName(asset.getAssetName());
        assertThatThrownBy(() -> service.create(asset2)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update() {
        asset.setAssetName("Updated Name");
        var updated = service.update(asset);

        assertThat(updated.getId()).isEqualTo(assetId);
        assertThat(updated.getAssetName()).isEqualTo("Updated Name");
    }

    @Test
    void getAll_Search() {
        assertThatThrownBy(() -> service.getAll(new Search("by", "term", CONTAINS, PageRequest.of(0, 10))))
                .isInstanceOf(NotImplementedException.class);
    }

    @Test
    void delete() {
        var storage1 = physicalStorage();
        storage1.setStorageDetails("New Storage");
        storage1.setPhysicalAssetId(assetId);
        storageService.create(storage1);

        service.delete(assetId);
        assertThatThrownBy(() -> service.getById(assetId)).isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void savePipelineMappings_NewLdm() {
        var baseEntity2 = ldmBaseEntity(namespace.getId());
        baseEntity2 = baseEntityService.create(baseEntity2);
        var ldmId2 = baseEntity2.getId();

        var updated = service.savePhysicalAssetLdmMappings(assetId, Set.of(ldmId, ldmId2));

        assertThat(updated.getLdmIds().size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void createPhysicalAssetInfraMappings() {
        if (infra.getId() == null) {
            infra = service.createPhysicalAssetInfra(infra);
        }
        service.createPhysicalAssetInfraMappings(assetId, infra.getId());
        var persisted = service.getByIdWithAssociations(assetId);
        assertThat(persisted.getAssetInfras()).isNotEmpty();

        var infras = service.getPhysicalAssetInfrasByAssetId(assetId);
        assertThat(infras).isNotNull();
        assertThat(infras).isNotEmpty();
    }

    @Test
    void createPhysicalAssetInfra_with_exception() {
        PhysicalAssetInfra infraNew = physicalAssetInfra();
        List<PhysicalAssetInfra> existing = infraService.getAllByInfraTypeAndPropertyTypeAndEnvironment(
                infraNew.getInfraType(),
                infraNew.getPropertyType(),
                infraNew.getPlatformEnvironment());
        if (existing.isEmpty()) {
            infraNew = service.createPhysicalAssetInfra(infraNew);
        }
        PhysicalAssetInfra finalInfraNew = infraNew;
        assertThatThrownBy(() -> service.createPhysicalAssetInfra(finalInfraNew)).isInstanceOf(IllegalArgumentException.class);
    }
}