package com.ebay.behavior.gds.mdm.signal.common.service;

import com.ebay.behavior.gds.mdm.common.config.UdcConfiguration;
import com.ebay.behavior.gds.mdm.common.model.EsPageable;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.service.udc.UdcQueryBuilder;
import com.ebay.behavior.gds.mdm.signal.common.testUtil.TestUtils;

import lombok.val;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestModelUtils.unstagedEvent;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestUtils.INTEGRATION_TEST;
import static java.util.Locale.US;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class StagedUdcEventServiceIT {

    @Autowired
    private MetadataWriteService writeService;

    @Autowired
    private StagedUdcEventService eventService;

    @Autowired
    private UdcQueryBuilder udcQueryBuilder;

    @Autowired
    private UdcConfiguration config;

    private final long eventId1 = 3L;
    private final long eventId2 = 4L;
    private UdcDataSourceType dataSource;
    private final String name = "Test_event_1";

    @BeforeAll
    void setUpAll() {
        dataSource = config.getDataSource();
        var randomString = TestUtils.getRandomSmallString().toLowerCase(US);
        var description = "test_desc_" + randomString + " POSTFIX";

        var unstagedEvent1 = unstagedEvent().toBuilder()
                .id(eventId1)
                .name(name)
                .description(description)
                .attributes(Set.of())
                .pageIds(Set.of())
                .moduleIds(Set.of())
                .clickIds(Set.of())
                .build();

        var unstagedEvent2 = unstagedEvent().toBuilder()
                .id(eventId2)
                .name("test_event_2")
                .description(description)
                .attributes(Set.of())
                .pageIds(Set.of())
                .moduleIds(Set.of())
                .clickIds(Set.of())
                .build();

        writeService.upsertEvent(unstagedEvent1);
        writeService.upsertEvent(unstagedEvent2);
        TestUtils.sleep(5);
    }

    @Test
    void getById() {
        var persisted = eventService.getById(eventId1);

        assertThat(persisted.getId()).isEqualTo(eventId1);
    }

    @Test
    void search_termByName() {
        val term = name.toLowerCase(US); // we must search lowercase with term since name is a processed property
        var pageable = EsPageable.of(0, 10);
        var queryBuilder = QueryBuilders.termQuery(NAME, term);
        var searchBuilder = udcQueryBuilder.toSearchSourceBuilder(pageable, queryBuilder);

        var page = eventService.search(searchBuilder.toString());

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(page.getContent().get(0).getName().toLowerCase(US)).contains(term);
    }

    @Test
    void getAll() {
        var pageable = EsPageable.of(0, 2); // two out of two
        var page = eventService.getAll(dataSource, pageable);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
        assertThat(page.getContent()).hasSize(2);

        pageable = EsPageable.of(0, 1); // one out of two
        page = eventService.getAll(dataSource, pageable);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
        assertThat(page.getContent()).hasSize(1);

        pageable = EsPageable.of(2, 1); // last page
        page = eventService.getAll(dataSource, pageable);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
        assertThat(page.getContent()).hasSize(1);

        pageable = EsPageable.of((int) page.getTotalElements() + 1, 1); // no such offset
        page = eventService.getAll(dataSource, pageable);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
        assertThat(page.getContent()).isEmpty();
    }
}