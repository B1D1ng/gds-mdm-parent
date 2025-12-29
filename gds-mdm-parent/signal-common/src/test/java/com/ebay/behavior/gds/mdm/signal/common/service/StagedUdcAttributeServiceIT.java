package com.ebay.behavior.gds.mdm.signal.common.service;

import com.ebay.behavior.gds.mdm.common.config.UdcConfiguration;
import com.ebay.behavior.gds.mdm.common.model.EsPageable;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.service.udc.UdcQueryBuilder;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedAttribute;
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

import java.util.Locale;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestModelUtils.unstagedAttribute;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestModelUtils.unstagedEvent;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestUtils.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class StagedUdcAttributeServiceIT {

    @Autowired
    private MetadataWriteService writeService;

    @Autowired
    private StagedUdcAttributeService attributeService;

    @Autowired
    private UdcQueryBuilder udcQueryBuilder;

    @Autowired
    private UdcConfiguration config;

    private final long attrId1 = 1L;
    private UdcDataSourceType dataSource;
    private final String description = "test_desc_POSTFIX";

    @BeforeAll
    void setUpAll() {
        dataSource = config.getDataSource();
        long eventId = 1L;
        var event = unstagedEvent().toBuilder()
                .id(eventId)
                .name("Test_event_1")
                .description("Test_event_1")
                .attributes(Set.of())
                .pageIds(Set.of())
                .moduleIds(Set.of())
                .clickIds(Set.of())
                .build();
        writeService.upsertEvent(event);

        var attr1 = unstagedAttribute(eventId).toBuilder()
                .id(attrId1)
                .tag("Test_attribute_1")
                .description(description)
                .build();
        var attr2 = unstagedAttribute(eventId).toBuilder()
                .id(2L)
                .tag("test_attribute_2")
                .description(description)
                .build();
        var attr3 = unstagedAttribute(eventId).toBuilder()
                .id(3L)
                .tag("test_attribute_3")
                .description(description)
                .build();
        var attr4 = unstagedAttribute(eventId).toBuilder()
                .id(4L)
                .tag("test_attribute_4")
                .description(description)
                .build();
        var attr5 = unstagedAttribute(eventId).toBuilder()
                .id(5L)
                .tag("test_attribute_5")
                .description(description)
                .build();

        writeService.upsertAttribute(eventId, attr1);
        writeService.upsertAttribute(eventId, attr2);
        writeService.upsertAttribute(eventId, attr3);
        writeService.upsertAttribute(eventId, attr4);
        writeService.upsertAttribute(eventId, attr5);
        TestUtils.sleep(5);
    }

    @Test
    void getById() {
        var persisted = attributeService.getById(attrId1);

        assertThat(persisted.getId()).isEqualTo(attrId1);
    }

    @Test
    void getAll() {
        var pageable = EsPageable.of(0, 2); // two out of two
        var page = attributeService.getAll(dataSource, pageable);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
        assertThat(page.getContent()).hasSize(2);

        pageable = EsPageable.of(0, 1); // one out of two
        page = attributeService.getAll(dataSource, pageable);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
        assertThat(page.getContent()).hasSize(1);

        pageable = EsPageable.of(2, 1); // last page
        page = attributeService.getAll(dataSource, pageable);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
        assertThat(page.getContent()).hasSize(1);

        pageable = EsPageable.of((int) page.getTotalElements() + 1, 1); // no such offset
        page = attributeService.getAll(dataSource, pageable);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void search_termByTag() {
        val term = "test_attribute_2";
        var pageable = EsPageable.of(0, 10);
        var queryBuilder = QueryBuilders.termQuery("tag", term);
        var searchBuilder = udcQueryBuilder.toSearchSourceBuilder(pageable, queryBuilder);

        var page = attributeService.search(searchBuilder.toString());

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(page.getContent().get(0).getTag()).contains(term);
    }

    @Test
    void search_termByDescription() {
        val term = description.toLowerCase(Locale.US);
        var pageable = EsPageable.of(0, 5);
        var queryBuilder = QueryBuilders.termQuery("description", term);
        var searchBuilder = udcQueryBuilder.toSearchSourceBuilder(pageable, queryBuilder);

        var page = attributeService.search(searchBuilder.toString());

        assertThat(page.getTotalElements()).isGreaterThan(1);
        assertThat(page.getContent()).extracting(UnstagedAttribute::getDescription).containsOnly(description);
    }
}