package com.ebay.behavior.gds.mdm.dec.service;

import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.dec.model.DatasetIndex;
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
class DatasetIndexServiceIT {

    @Autowired
    private DatasetIndexService service;

    private DatasetIndex index;

    @BeforeEach
    void setUp() {
        index = TestModelUtils.datasetIndex();
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
        var index = service.initialize("Test Dataset Index");
        assertThat(index.getId()).isNotNull();
        assertThat(index.getCurrentVersion()).isEqualTo(1);
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
