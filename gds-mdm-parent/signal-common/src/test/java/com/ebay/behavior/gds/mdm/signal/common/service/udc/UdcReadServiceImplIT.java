package com.ebay.behavior.gds.mdm.signal.common.service.udc;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.EsPageable;
import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.service.udc.UdcReadServiceImpl;
import com.ebay.behavior.gds.mdm.common.util.TimeUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.service.MetadataWriteService;
import com.ebay.behavior.gds.mdm.signal.common.testUtil.TestUtils;

import lombok.val;
import org.elasticsearch.index.query.Operator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.VersionedId.MIN_VERSION;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.EVENT;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.SIGNAL;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.TRANSFORMATION;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.DESCRIPTION;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestModelUtils.unstagedEvent;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestModelUtils.unstagedField;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestModelUtils.unstagedSignal;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestUtils.INTEGRATION_TEST;
import static com.ebay.behavior.gds.mdm.signal.common.testUtil.TestUtils.getRandomSmallString;
import static java.util.Locale.US;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UdcReadServiceImplIT {

    private final long planId = 1L;
    private final long eventId = 12L;
    private final VersionedId signalId1 = VersionedId.of(25L, MIN_VERSION);
    private final VersionedId signalId2 = VersionedId.of(26L, MIN_VERSION);

    private final int sleepSeconds = 7;
    private EsPageable pageable;

    private String descWord1;
    private String descWord2;
    private String eventEntityId;
    private UnstagedEvent event;

    @Autowired
    private MetadataWriteService writeService;

    @Autowired
    private UdcReadServiceImpl readService;

    @BeforeAll
    void setUpAll() {
        var prefix = getRandomSmallString().toUpperCase(US);
        var name = prefix + '_' + getRandomSmallString().toLowerCase(US); // like ABC_abc
        var type = getRandomSmallString().toLowerCase(US);
        descWord1 = getRandomSmallString().toLowerCase(US);
        descWord2 = getRandomSmallString().toUpperCase(US);
        var desc = descWord1 + ' ' + descWord2;

        event = unstagedEvent().toBuilder()
                .id(eventId)
                .name(name)
                .type(type)
                .description(desc)
                .expression(name)
                .attributes(Set.of())
                .pageIds(Set.of())
                .moduleIds(Set.of())
                .clickIds(Set.of())
                .build();
        event.setEventSourceId(1L);
        event.setCreateDate(TimeUtils.toNowSqlTimestamp());
        event.setUpdateDate(TimeUtils.toNowSqlTimestamp());
        eventEntityId = writeService.upsertEvent(event);

        TestUtils.sleep(sleepSeconds);
    }

    @BeforeEach
    void setUp() {
        pageable = TestUtils.getAuditableEsPageable(0, 5);
    }

    @Disabled
    @AfterAll
    void tearDown() {
        writeService.deleteEvent(eventId);
        writeService.deleteSignal(signalId1.getId());
        writeService.deleteSignal(signalId2.getId());
    }

    @Test
    void getHistoryById_event() {
        var versions = readService.getHistoryById(eventEntityId).getVersions();

        assertThat(versions).extracting("version").doesNotContainNull();
        assertThat(versions).extracting("entityVersionData.entityType").containsOnly(EVENT);
        assertThat(versions).extracting("entityVersionData.graphPk").containsOnly(eventEntityId);
    }

    @Test
    void getHistoryById_signal() {
        var signal1 = unstagedSignal(planId).toBuilder()
                .events(Set.of())
                .fields(Set.of())
                .createDate(TimeUtils.toNowSqlTimestamp())
                .updateDate(TimeUtils.toNowSqlTimestamp())
                .build();
        signal1.setSignalId(signalId1);

        var entityIdMap = writeService.upsertSignal(Set.of(), signal1);
        var entityId = entityIdMap.get(SIGNAL);

        var versions = readService.getHistoryById(entityId).getVersions();

        assertThat(versions).extracting("version").doesNotContainNull();
        assertThat(versions).extracting("entityVersionData.entityType").containsOnly(SIGNAL);
        assertThat(versions).extracting("entityVersionData.graphPk").containsOnly(entityId);
    }

    @Test
    void getHistoryById_notFound() {
        assertThatThrownBy(() -> readService.getHistoryById("not_found"))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void getEntityById() {
        var entityById = readService.getEntityById(EVENT, eventId);
        var entityByEntityId = readService.getEntityById(eventEntityId);

        assertThat(entityById.getEntityType()).isEqualTo(entityByEntityId.getEntityType());
        assertThat(entityById.getGraphPk()).isEqualTo(entityByEntityId.getGraphPk());
    }

    @Test
    void getEntityById_notFound() {
        assertThatThrownBy(() -> readService.getEntityById("not_found"))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    @Disabled("This test have issues with a sync on UDC side")
    void getLineageById() {
        // given
        var field = unstagedField(signalId1).toBuilder()
                .id(11L)
                .attributes(Set.of())
                .build();
        var signal = unstagedSignal(planId).toBuilder()
                .events(Set.of(event))
                .fields(Set.of(field))
                .build();
        signal.setSignalId(signalId1);
        var entityIdMap = writeService.upsertSignal(Set.of(eventId), signal);
        writeService.upsertField(signalId1.getId(), Set.of(), field);

        var lineageId = entityIdMap.get(TRANSFORMATION);

        assertThat(lineageId).isNotNull();
        TestUtils.sleep(sleepSeconds);

        // when
        var lineageData = readService.getLineageById(Set.of(lineageId));
        val vertices = lineageData.getVertices();

        // then
        assertThat(vertices).hasSize(3);
        assertThat(vertices).extracting("entityType")
                .containsExactlyInAnyOrderElementsOf(List.of(TRANSFORMATION.getValue(), SIGNAL.getValue(), EVENT.getValue()));
        assertThat(vertices).extracting("entityId").contains(lineageId);
    }

    @Test
    void getLineageById_badId_error() {
        assertThatThrownBy(() -> readService.getLineageById("999999"))
                .isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("doesn't found");
    }

    @Test
    void termQuery_eventById() {
        var idName = event.getEntityType().getIdName();

        var page = readService.termQuery(UnstagedEvent.class, pageable, String.valueOf(eventId), idName);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getNumberOfElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);

        var entity = page.getContent().get(0);
        assertThat(entity.getEntityType()).isEqualTo(event.getEntityType());
        assertThat(entity.getId()).isEqualTo(eventId);
    }

    @Test
    void termQuery_eventByName() {
        var page = readService.termQuery(UnstagedEvent.class, pageable, event.getName().toLowerCase(US), NAME);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getNumberOfElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);

        var entity = page.getContent().get(0);
        assertThat(entity.getEntityType()).isEqualTo(event.getEntityType());
        assertThat(entity.getName()).isEqualTo(event.getName());
    }

    @Test
    void termsQuery_signalById() {
        var signal1 = unstagedSignal(planId).toBuilder()
                .events(Set.of())
                .fields(Set.of())
                .build();
        signal1.setSignalId(signalId1);
        signal1.setCreateDate(TimeUtils.toNowSqlTimestamp());
        signal1.setUpdateDate(TimeUtils.toNowSqlTimestamp());

        var signal2 = unstagedSignal(planId).toBuilder()
                .events(Set.of())
                .fields(Set.of())
                .build();
        signal2.setSignalId(signalId2);
        signal2.setCreateDate(TimeUtils.toNowSqlTimestamp());
        signal2.setUpdateDate(TimeUtils.toNowSqlTimestamp());

        writeService.upsertSignal(Set.of(), signal1);
        writeService.upsertSignal(Set.of(), signal2);
        var idName = signal1.getEntityType().getIdName();
        TestUtils.sleep(sleepSeconds);

        var page = readService.termsQuery(UnstagedSignal.class, pageable, Set.of(String.valueOf(signalId1.getId()), String.valueOf(signalId2.getId())), idName);

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getNumberOfElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(2);

        var entities = page.getContent();
        assertThat(entities).extracting(Metadata::getEntityType).containsOnly(signal1.getEntityType());
        assertThat(entities).extracting(Metadata::getId).containsExactlyInAnyOrder(signalId1.getId(), signalId2.getId());
    }

    @Test
    void wildcardQueryOnTextField() {
        var pattern = "*" + descWord2.toLowerCase(US);

        var page = readService.wildcardQuery(UnstagedEvent.class, pageable, pattern, DESCRIPTION);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getNumberOfElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);

        var entity = page.getContent().get(0);
        assertThat(entity.getEntityType()).isEqualTo(event.getEntityType());
        assertThat(entity.getDescription()).endsWith(descWord2);
    }

    @Test
    void wildcardQueryOnKeywordField() {
        var pattern = event.getName() + "*";

        var page = readService.wildcardQuery(UnstagedEvent.class, pageable, pattern, NAME);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getNumberOfElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);

        var entity = page.getContent().get(0);
        assertThat(entity.getEntityType()).isEqualTo(event.getEntityType());
        assertThat(entity.getDescription()).endsWith(descWord2);
    }

    @Test
    void matchQuery() {
        var desc = event.getDescription().toLowerCase(US); // since ES uses processed data

        var page = readService.matchQuery(UnstagedEvent.class, pageable, desc, DESCRIPTION);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getNumberOfElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);

        var entity = page.getContent().get(0);
        assertThat(entity.getEntityType()).isEqualTo(event.getEntityType());
        assertThat(entity.getDescription()).isEqualTo(event.getDescription());
    }

    @Test
    void matchAllQuery() {
        val oneElementPageable = EsPageable.of(0, 1);

        var page = readService.matchAllQuery(UnstagedEvent.class, oneElementPageable); // also tests pagination works

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(page.getNumberOfElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);

        var entity = page.getContent().get(0);
        assertThat(entity.getEntityType()).isEqualTo(event.getEntityType());
    }

    @Test
    void multiMatchQuery_or() {
        var text = event.getDescription().toLowerCase(US);

        var page = readService.multiMatchQuery(UnstagedEvent.class, pageable, text, Operator.OR, DESCRIPTION, NAME);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getNumberOfElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);

        var entity = page.getContent().get(0);
        assertThat(entity.getEntityType()).isEqualTo(event.getEntityType());
        assertThat(entity.getId()).isEqualTo(eventId);
    }

    @Test
    void multiMatchQuery_and() {
        var text = event.getName().toLowerCase(US);

        var page = readService.multiMatchQuery(UnstagedEvent.class, pageable, text, Operator.AND, "expression", NAME);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getNumberOfElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);

        var entity = page.getContent().get(0);
        assertThat(entity.getEntityType()).isEqualTo(event.getEntityType());
        assertThat(entity.getId()).isEqualTo(eventId);
    }

    @Test
    void prefixQuery() {
        var text = descWord1; // ES uses processed data

        var page = readService.prefixQuery(UnstagedEvent.class, pageable, text, DESCRIPTION);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getNumberOfElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);

        var entity = page.getContent().get(0);
        assertThat(entity.getEntityType()).isEqualTo(event.getEntityType());
        assertThat(entity.getId()).isEqualTo(eventId);
    }

    @Test
    void existsQuery() {
        var page = readService.existsQuery(UnstagedEvent.class, pageable, NAME);

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(page.getNumberOfElements()).isGreaterThanOrEqualTo(1);
        assertThat(page.getContent().size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void rangeQuery_event() {
        val now = TimeUtils.toNowSqlTimestamp();
        var from = new Timestamp(now.getTime() - 60000).getTime(); // minus 1 minute
        var to = new Timestamp(now.getTime() + 60000).getTime(); // plus 1 minute

        var page = readService.rangeQuery(UnstagedEvent.class, pageable, "createDate", from, to);

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(page.getNumberOfElements()).isGreaterThanOrEqualTo(1);
        assertThat(page.getContent().size()).isGreaterThanOrEqualTo(1);

        var entity = page.getContent().get(0);
        assertThat(entity.getEntityType()).isEqualTo(event.getEntityType());
        assertThat(entity.getId()).isEqualTo(eventId);
    }
}