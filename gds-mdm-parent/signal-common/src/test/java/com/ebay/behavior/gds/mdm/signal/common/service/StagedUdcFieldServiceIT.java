package com.ebay.behavior.gds.mdm.signal.common.service;

import com.ebay.behavior.gds.mdm.common.config.UdcConfiguration;
import com.ebay.behavior.gds.mdm.common.model.EsPageable;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
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

import java.util.Locale;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.Model.COMMA;
import static com.ebay.behavior.gds.mdm.common.model.VersionedId.MIN_VERSION;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.SIGNAL;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestModelUtils.unstagedAttribute;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestModelUtils.unstagedEvent;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestModelUtils.unstagedField;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestModelUtils.unstagedSignal;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestUtils.INTEGRATION_TEST;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestUtils.PAGE_VIEW_ENTRY;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestUtils.PAGE_VIEW_EXIT;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class StagedUdcFieldServiceIT {

    @Autowired
    private MetadataWriteService writeService;

    @Autowired
    private StagedUdcFieldService fieldService;

    @Autowired
    private UdcQueryBuilder udcQueryBuilder;

    @Autowired
    private UdcConfiguration config;

    private final long fieldId1 = 33L;
    private final long fieldId2 = 34L;
    private UdcDataSourceType dataSource;

    @BeforeAll
    void setUpAll() {
        dataSource = config.getDataSource();
        long planId = 1L;
        VersionedId signalId = VersionedId.of(34L, MIN_VERSION);
        long eventId = 34L;
        long attributeId = 34L;

        var unstagedEvent = unstagedEvent().toBuilder()
                .id(eventId)
                .name("Test_event_1")
                .description("Test_event_1")
                .attributes(Set.of())
                .pageIds(Set.of())
                .moduleIds(Set.of())
                .clickIds(Set.of())
                .build();
        writeService.upsertEvent(unstagedEvent);

        var unstagedAttribute = unstagedAttribute(eventId).toBuilder()
                .id(attributeId)
                .tag("Test_attribute_1")
                .description("Test_attribute_1")
                .build();
        writeService.upsertAttribute(eventId, unstagedAttribute);

        var unstagedSignal = unstagedSignal(planId).toBuilder()
                .name("Test_signal_1")
                .description("Test_signal_1")
                .events(Set.of(unstagedEvent))
                .fields(Set.of())
                .build();
        unstagedSignal.setSignalId(signalId);
        writeService.upsertSignal(Set.of(eventId), unstagedSignal).get(SIGNAL);

        var randomString = TestUtils.getRandomSmallString().toLowerCase(Locale.US);
        var description = "test_desc_" + randomString + " POSTFIX";

        var unstagedField1 = unstagedField(signalId).toBuilder()
                .id(fieldId1)
                .tag("Test_field_11")
                .description(description)
                .eventTypes(String.join(COMMA, PAGE_VIEW_ENTRY, PAGE_VIEW_EXIT))
                .attributes(Set.of())
                .build();
        var unstagedField2 = unstagedField(signalId).toBuilder()
                .id(fieldId2)
                .tag("test_field_12")
                .description(description)
                .eventTypes(String.join(COMMA, PAGE_VIEW_ENTRY, PAGE_VIEW_EXIT))
                .attributes(Set.of())
                .build();

        writeService.upsertField(signalId.getId(), Set.of(attributeId), unstagedField1);
        writeService.upsertField(signalId.getId(), Set.of(attributeId), unstagedField2);
        TestUtils.sleep(5);
    }

    @Test
    void getById() {
        var persisted = fieldService.getById(fieldId1);

        assertThat(persisted.getId()).isEqualTo(fieldId1);
    }

    @Test
    void getAll() {
        var pageable = EsPageable.of(0, 2); // two out of two
        var page = fieldService.getAll(dataSource, pageable);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
        assertThat(page.getContent()).hasSize(2);

        pageable = EsPageable.of(0, 1); // one out of two
        page = fieldService.getAll(dataSource, pageable);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
        assertThat(page.getContent()).hasSize(1);

        pageable = EsPageable.of((int) page.getTotalElements() + 1, 1); // no such offset
        page = fieldService.getAll(dataSource, pageable);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void search_termByTag() {
        val term = "test_field_12";
        var pageable = EsPageable.of(0, 10);
        var queryBuilder = QueryBuilders.termQuery("tag", term);
        var searchBuilder = udcQueryBuilder.toSearchSourceBuilder(pageable, queryBuilder);

        var page = fieldService.search(searchBuilder.toString());

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(page.getContent().get(0).getTag()).contains(term);
    }
}