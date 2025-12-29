package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.search.EventSearchBy;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;

import org.apache.commons.lang3.NotImplementedException;
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

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.CONTAINS;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.EXACT_MATCH;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.EXACT_MATCH_IGNORE_CASE;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.STARTS_WITH;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.common.model.search.EventSearchBy.DESCRIPTION;
import static com.ebay.behavior.gds.mdm.signal.common.model.search.EventSearchBy.PAGE_ID;
import static com.ebay.behavior.gds.mdm.signal.common.model.search.EventSearchBy.TYPE;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.stagedEvent;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class StagedEventServiceIT {

    private final Pageable pageable = PageRequest.of(0, 10);

    @Autowired
    private StagedEventService eventService;

    @Autowired
    private StagedAttributeService attributeService;

    private long eventId;
    private StagedEvent event;

    @BeforeEach
    void setUp() {
        event = stagedEvent().toBuilder()
                .pageIds(Set.of(getRandomLong(), getRandomLong()))
                .moduleIds(Set.of(getRandomLong()))
                .build();
        event = eventService.create(event);
        event = eventService.getByIdWithAssociations(event.getId());
        eventId = event.getId();
    }

    @Test
    void create() {
        assertThat(event.getId()).isNotNull();
        assertThat(event.getPageIds()).hasSize(2);
        assertThat(event.getModuleIds()).hasSize(1);
        assertThat(event.getClickIds()).isEmpty();
        assertThat(eventService.findById(eventId)).isPresent();
    }

    @Test
    void update_notImplemented_error() {
        assertThatThrownBy(() -> eventService.update(event))
                .isInstanceOf(NotImplementedException.class);
    }

    @Test
    void delete_errorByDesign() {
        assertThatThrownBy(() -> eventService.delete(1L))
                .isInstanceOf(NotImplementedException.class);
    }

    @Test
    void getById_nonExistentId_error() {
        assertThatThrownBy(() -> eventService.getById(9999L))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void getAttributes() {
        var attributes = eventService.getAttributes(eventId);
        assertThat(attributes).isEmpty();

        var attribute = TestModelUtils.stagedAttribute(eventId);
        attributeService.create(attribute);
        attributes = eventService.getAttributes(eventId);

        assertThat(attributes.size()).isEqualTo(1);
    }

    @Test
    void getAllByIds() {
        var fields = eventService.getAllByIds(Set.of(eventId));

        assertThat(fields).hasSize(1);
        assertThat(fields.iterator().next().getId()).isEqualTo(eventId);
    }

    @Test
    void getAllByIds_emptySet() {
        var fields = eventService.getAllByIds(Set.of());

        assertThat(fields).isEmpty();
    }

    @Test
    void getAll_byNameExactMatch() {
        var term = event.getName();
        var search = new Search(EventSearchBy.NAME.name(), term, EXACT_MATCH, pageable);

        var page = eventService.getAll(search);

        var persisted = page.getContent().get(0);
        assertThat(page.getContent()).hasSize(1);
        assertThat(persisted.getName()).isEqualTo(term);
    }

    @Test
    void getAll_byTypeStartsWith() {
        var term = event.getType().substring(0, 5);
        var search = new Search(TYPE.name(), term, STARTS_WITH, pageable);

        var page = eventService.getAll(search);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getType()).contains(term);
    }

    @Test
    void getAll_byDescriptionContains() {
        var term = event.getDescription().substring(3);
        var search = new Search(DESCRIPTION.name(), term, CONTAINS, pageable);

        var page = eventService.getAll(search);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getDescription()).contains(term);
    }

    @Test
    void getAll_byPageId() {
        var pageId = getRandomLong();
        var event1 = stagedEvent().toBuilder()
                .pageIds(Set.of(pageId, getRandomLong(), getRandomLong()))
                .moduleIds(Set.of(getRandomLong(), getRandomLong()))
                .build();
        var event2 = stagedEvent().toBuilder()
                .pageIds(Set.of(pageId, getRandomLong(), getRandomLong()))
                .moduleIds(Set.of(getRandomLong(), getRandomLong()))
                .build();
        eventService.create(event1);
        eventService.create(event2);

        var term = String.valueOf(pageId);
        var search = new Search(PAGE_ID.name(), term, EXACT_MATCH, pageable);

        var page = eventService.getAll(search);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getPageIds()).contains(pageId);
        assertThat(page.getContent().get(1).getPageIds()).contains(pageId);
    }

    @Test
    void getAll_byPageIdWithUnsupportedCriterion_error() {
        var search = new Search(PAGE_ID.name(), "123", EXACT_MATCH_IGNORE_CASE, pageable);

        assertThatThrownBy(() -> eventService.getAll(search))
                .isInstanceOf(NotImplementedException.class)
                .hasMessageContaining("not supported");
    }

    @Test
    void getAll_byModuleId() {
        var moduleId = getRandomLong();
        var event1 = stagedEvent().toBuilder().moduleIds(Set.of(moduleId, getRandomLong())).build();
        var event2 = stagedEvent().toBuilder().moduleIds(Set.of(moduleId, getRandomLong())).build();
        eventService.create(event1);
        eventService.create(event2);

        var term = String.valueOf(moduleId);
        var search = new Search(EventSearchBy.MODULE_ID.name(), term, EXACT_MATCH, pageable);

        var page = eventService.getAll(search);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getModuleIds()).contains(moduleId);
        assertThat(page.getContent().get(1).getModuleIds()).contains(moduleId);
    }

    @Test
    void getAll_byClickId() {
        var clickId = getRandomLong();
        var event1 = stagedEvent().toBuilder().clickIds(Set.of(clickId, getRandomLong())).build();
        var event2 = stagedEvent().toBuilder().clickIds(Set.of(clickId, getRandomLong())).build();
        eventService.create(event1);
        eventService.create(event2);

        var term = String.valueOf(clickId);
        var search = new Search(EventSearchBy.CLICK_ID.name(), term, EXACT_MATCH, pageable);

        var page = eventService.getAll(search);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getClickIds()).contains(clickId);
        assertThat(page.getContent().get(1).getClickIds()).contains(clickId);
    }
}