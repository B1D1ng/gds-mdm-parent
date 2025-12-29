package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
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
class NamespaceServiceIT {

    @Autowired
    private NamespaceService service;

    private Namespace namespace;

    @BeforeEach
    void setUp() {
        namespace = TestModelUtils.namespace();
        namespace = service.create(namespace);
    }

    @Test
    void update() {
        namespace.setName("New Name");
        var updated = service.update(namespace);

        assertThat(updated.getName()).isEqualTo("New Name");
    }

    @Test
    void update_NameConflict() {
        Namespace namespace1 = TestModelUtils.namespace();
        namespace1 = service.create(namespace1);
        namespace.setName(namespace1.getName());

        assertThatThrownBy(() -> service.update(namespace)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getById() {
        var persisted = service.getById(namespace.getId());

        assertThat(persisted).isEqualTo(namespace);
    }

    @Test
    void getAll() {
        var namespaces = service.getAll();

        assertThat(namespaces.size()).isEqualTo(1);
    }

    @Test
    void getAllByName() {
        var namespaces = service.getAllByName(namespace.getName());

        assertThat(namespaces.size()).isEqualTo(1);
    }

    @Test
    void getByIdWithAssociations() {
        assertThatThrownBy(() -> service.getByIdWithAssociations(namespace.getId()))
                .isInstanceOf(NotImplementedException.class);
    }

    @Test
    void getAll_Search() {
        assertThatThrownBy(() -> service.getAll(new Search("by", "term", CONTAINS, PageRequest.of(0, 10))))
                .isInstanceOf(NotImplementedException.class);
    }
}