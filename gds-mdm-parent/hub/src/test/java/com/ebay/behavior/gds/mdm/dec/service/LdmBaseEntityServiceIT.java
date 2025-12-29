package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.model.Namespace;
import com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils;

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

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.CONTAINS;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestModelUtils.physicalAsset;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class LdmBaseEntityServiceIT {

    @Autowired
    private LdmBaseEntityService service;

    @Autowired
    private NamespaceService namespaceService;

    @Autowired
    private LdmEntityService entityService;

    @Autowired
    private PhysicalAssetService assetService;

    private Namespace namespace;
    private LdmBaseEntity baseEntity;

    @BeforeEach
    void setUp() {
        namespace = TestModelUtils.namespace();
        namespace = namespaceService.create(namespace);

        baseEntity = TestModelUtils.ldmBaseEntity(namespace.getId());
        service.create(baseEntity);
    }

    @Test
    void getByIdWithAssociations() {
        var entity = TestModelUtils.ldmEntityEmpty(namespace.getId());
        entity = entityService.create(entity);

        var persisted = service.getByIdWithAssociations(entity.getBaseEntityId());
        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getViews()).isNotEmpty();
    }

    @Test
    void getAll_Search() {
        assertThatThrownBy(() -> service.getAll(new Search("by", "term", CONTAINS, PageRequest.of(0, 10))))
                .isInstanceOf(NotImplementedException.class);
    }

    @Test
    void getAll() {
        var persisted = service.getAll();
        assertThat(persisted).isNotEmpty();
    }

    @Test
    void getAllExcludeTextFields() {
        var entity = TestModelUtils.ldmEntityEmpty(namespace.getId());
        entity.setBaseEntityId(baseEntity.getId());
        entity.setIr("testIr");
        entityService.create(entity);

        var persisted = service.getAll(true);

        assertThat(persisted).isNotEmpty();
        // Only then check the ir field
        assertThat(persisted.get(0).getViews().get(0).getIr()).isNull();
    }

    @Test
    void create() {
        var newBaseEntity = TestModelUtils.ldmBaseEntity(namespace.getId());
        newBaseEntity.setName("New Base Entity");
        var created = service.create(newBaseEntity);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("New Base Entity");
    }

    @Test
    void delete() {
        var baseEntity1 = TestModelUtils.ldmBaseEntity(namespace.getId());
        baseEntity1 = service.create(baseEntity1);
        var baseEntityId1 = baseEntity1.getId();

        var asset = physicalAsset();
        asset = assetService.create(asset);
        assetService.savePhysicalAssetLdmMappings(asset.getId(), Set.of(baseEntityId1));

        service.delete(baseEntityId1);
        assertThatThrownBy(() -> service.getById(baseEntityId1)).isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void searchByNameAndNamespace() {
        var persisted = service.searchByNameAndNamespace(baseEntity.getName(), namespace.getName());
        assertThat(persisted.size()).isGreaterThanOrEqualTo(1);

        var persisted2 = service.searchByNameAndNamespace(baseEntity.getName(), null);
        assertThat(persisted2.size()).isGreaterThanOrEqualTo(1);

        var persisted3 = service.searchByNameAndNamespace(null, namespace.getName());
        assertThat(persisted3.size()).isGreaterThanOrEqualTo(1);
    }
}
