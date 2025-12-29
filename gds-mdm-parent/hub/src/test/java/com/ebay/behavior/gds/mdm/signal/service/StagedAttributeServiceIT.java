package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedAttribute;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.CONTAINS;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.EXACT_MATCH;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.common.model.search.AttributeSearchBy.DESCRIPTION;
import static com.ebay.behavior.gds.mdm.signal.common.model.search.AttributeSearchBy.TAG;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.stagedAttribute;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class StagedAttributeServiceIT {

    private final Pageable pageable = PageRequest.of(0, 10);

    @Autowired
    private StagedAttributeService service;

    @Autowired
    private StagedEventService eventService;

    private long eventId;
    private long attributeId;
    private StagedAttribute attribute;

    @BeforeAll
    void setUpAll() {
        var event = TestModelUtils.stagedEvent();
        eventId = eventService.create(event).getId();
    }

    @BeforeEach
    void setUp() {
        attribute = stagedAttribute(eventId);
        attribute = service.create(attribute);
        attribute = service.getById(attribute.getId());
        attributeId = attribute.getId();
    }

    @Test
    void create() {
        assertThat(attribute.getId()).isNotNull();
        assertThat(service.findById(attributeId)).isPresent();
    }

    @Test
    void update_errorByDesign() {
        assertThatThrownBy(() -> service.update(attribute))
                .isInstanceOf(NotImplementedException.class);
    }

    @Test
    void delete_errorByDesign() {
        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(NotImplementedException.class);
    }

    @Test
    void getByIdWithAssociations_initializesAssociations() {
        var persisted = service.getByIdWithAssociations(attributeId);

        assertThat(persisted.getEvent().getId()).isEqualTo(eventId);
    }

    @Test
    void getById_nonExistentId_error() {
        assertThatThrownBy(() -> service.getById(9999L))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void getAll_byTagExactMatch() {
        var term = attribute.getTag();
        var search = new Search(TAG.name(), term, EXACT_MATCH, pageable);

        var page = service.getAll(search);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTag()).isEqualTo(term);
    }

    @Test
    void getAll_byDescriptionContains() {
        var term = attribute.getDescription().substring(3);
        var search = new Search(DESCRIPTION.name(), term, CONTAINS, pageable);

        var page = service.getAll(search);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getDescription()).contains(term);
    }
}