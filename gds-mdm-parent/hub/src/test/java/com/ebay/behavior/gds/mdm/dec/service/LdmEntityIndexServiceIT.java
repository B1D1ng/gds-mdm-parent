package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.dec.model.LdmBaseEntity;
import com.ebay.behavior.gds.mdm.dec.model.LdmEntityIndex;
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

import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.CONTAINS;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.dec.testUtil.TestUtils.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class LdmEntityIndexServiceIT {

    @Autowired
    private LdmEntityIndexService service;

    @Autowired
    private LdmBaseEntityService baseEntityService;

    @Autowired
    private NamespaceService namespaceService;

    private LdmEntityIndex index;
    private Namespace namespace;
    private LdmBaseEntity baseEntity;

    @BeforeEach
    void setUp() {
        namespace = TestModelUtils.namespace();
        namespace = namespaceService.create(namespace);

        baseEntity = TestModelUtils.ldmBaseEntity(namespace.getId());
        baseEntity = baseEntityService.create(baseEntity);

        index = TestModelUtils.ldmEntityIndex(baseEntity.getId());
        index = service.create(index);
    }

    @Test
    void getById() {
        var persisted = service.getById(index.getId());
        assertThat(persisted.getId()).isNotNull();
    }

    @Test
    void updateVersion() {
        service.updateVersion(index.getId(), 2);
        var persisted = service.getById(index.getId());
        assertThat(persisted.getCurrentVersion()).isEqualTo(2);
    }

    @Test
    void initialize() {
        var ldmEntity = TestModelUtils.ldmEntityEmpty(namespace.getId());
        var index1 = service.initialize(ldmEntity);
        assertThat(index1.getId()).isNotNull();
        assertThat(index1.getCurrentVersion()).isEqualTo(1);
        assertThat(index1.getBaseEntityId()).isNotNull();
    }

    @Test
    void initialize_NameConflict() {
        var ldmEntity = TestModelUtils.ldmEntityEmpty(namespace.getId());
        service.initialize(ldmEntity);

        var ldmEntity2 = TestModelUtils.ldmEntityEmpty(namespace.getId());
        ldmEntity2.setName(ldmEntity.getName());
        assertThatThrownBy(() -> service.initialize(ldmEntity2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void initialize_BaseEntityNotNull() {
        var ldmEntity = TestModelUtils.ldmEntityEmptyWithBaseEntity(namespace.getId(), baseEntity.getId());
        var index1 = service.initialize(ldmEntity);
        assertThat(index1.getId()).isNotNull();
        assertThat(index1.getCurrentVersion()).isEqualTo(1);
        assertThat(index1.getBaseEntityId()).isEqualTo(baseEntity.getId());
    }

    @Test
    void initialize_BaseEntityNotValid() {
        var ldmEntity = TestModelUtils.ldmEntityEmptyWithBaseEntity(namespace.getId(), 99L);
        assertThatThrownBy(() -> service.initialize(ldmEntity)).isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void getByIdWithAssociations() {
        assertThatThrownBy(() -> service.getByIdWithAssociations(index.getId()))
                .isInstanceOf(NotImplementedException.class);
    }

    @Test
    void getAll_Search() {
        assertThatThrownBy(() -> service.getAll(new Search("by", "term", CONTAINS, PageRequest.of(0, 10))))
                .isInstanceOf(NotImplementedException.class);
    }
}
