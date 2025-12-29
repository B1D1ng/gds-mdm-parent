package com.ebay.behavior.gds.mdm.signal.testUtil;

import com.ebay.behavior.gds.mdm.common.config.UdcConfiguration;
import com.ebay.behavior.gds.mdm.common.model.EsPageable;
import com.ebay.behavior.gds.mdm.common.model.JavaType;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedAttribute;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.common.service.MetadataWriteService;
import com.ebay.behavior.gds.mdm.signal.common.service.StagedUdcAttributeService;
import com.ebay.behavior.gds.mdm.signal.common.service.StagedUdcEventService;
import com.ebay.behavior.gds.mdm.signal.common.service.StagedUdcFieldService;
import com.ebay.behavior.gds.mdm.signal.common.service.StagedUdcSignalService;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Objects;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.ExpressionType.JEXL;
import static com.ebay.behavior.gds.mdm.common.model.JavaType.LONG;
import static com.ebay.behavior.gds.mdm.common.model.VersionedId.MIN_VERSION;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.common.model.EventSource.SOJ;
import static com.ebay.behavior.gds.mdm.signal.common.model.EventSource.ST;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.CLIENT_PAGE_VIEW;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.PAGE_IMPRESSION_SIGNAL;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.PAGE_SERVE;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.PAGE_VIEW_ENTRY;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.PAGE_VIEW_EXIT;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.TIMESTAMP;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedAttribute;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedEvent;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedSignal;

/**
 * This is not a test, but a utility class used to delete test metadata from UDC Portal.
 * It runs only manually.
 */
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
@Slf4j
@Disabled
@ActiveProfiles(IT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class TestUdcUtils {

    @Autowired
    private MetadataWriteService writeService;

    @Autowired
    private StagedUdcSignalService signalService;

    @Autowired
    private StagedUdcFieldService fieldService;

    @Autowired
    private StagedUdcEventService eventService;

    @Autowired
    private StagedUdcAttributeService attributeService;

    @Autowired
    private UdcConfiguration config;

    private final EsPageable pageable = EsPageable.of(0, 1_000, Sort.unsorted());

    /**
     * Deletes all test metadata from UDC Portal.
     * Do not run, until you know what you are doing!
     */
    @Test
    @Disabled
    void deleteTestMetadata() {
        val dataSource = config.getDataSource();
        log.info("Deleting test metadata from {} data source", dataSource.getValue());

        attributeService.getAll(dataSource, pageable).forEach(entity -> {
            if (Objects.nonNull(entity.getId())) {
                writeService.deleteAttribute(entity.getId());
            }
        });

        fieldService.getAll(dataSource, pageable).forEach(entity -> {
            if (Objects.nonNull(entity.getId())) {
                writeService.deleteField(entity.getId());
            }
        });

        eventService.getAll(dataSource, pageable).forEach(entity -> {
            if (Objects.nonNull(entity.getId())) {
                writeService.deleteEvent(entity.getId());
            }
        });

        signalService.getAll(dataSource, pageable).forEach(entity -> {
            if (Objects.nonNull(entity.getId())) {
                writeService.deleteSignal(entity.getId());
            }
        });
    }

    @Test
    @Disabled
    void pageImpressionSignal() {
        var id = 30L;
        var signalId = VersionedId.of(id, MIN_VERSION);
        var pageViewEntryId = 31L;
        var pageViewExitId = 32L;
        var pageServeId = 33L;
        var clientPageViewId = 34L;
        var fieldId1 = 35L;
        var fieldId2 = 36L;
        var fieldId3 = 37L;
        var fieldId4 = 38L;
        var fieldId5 = 39L;

        var attr1 = timestamp(pageViewEntryId);
        var attr2 = timestamp(pageViewExitId);
        var attr3 = timestamp(pageServeId);
        var attr4 = timestamp(clientPageViewId);

        var pageViewEntry = unstagedEvent().toBuilder()
                .id(pageViewEntryId)
                .name("Page view entry (test-" + pageViewEntryId + ')')
                .description("Page view entry (test-" + pageViewEntryId + ')')
                .type(PAGE_VIEW_ENTRY)
                .source(ST)
                .expression("[${PAGE_IDS}].contains(event.context.pageInteractionContext.pageId)")
                .expressionType(JEXL)
                .attributes(Set.of(attr1))
                .pageIds(Set.of(123123L, 345123L))
                .moduleIds(Set.of(321L, 465L))
                .clickIds(Set.of(503001L, 608002L))
                .build();

        var pageViewExit = unstagedEvent().toBuilder()
                .id(pageViewExitId)
                .name("Page view exit (test-" + pageViewExitId + ')')
                .description("Page view exit (test-" + pageViewExitId + ')')
                .type(PAGE_VIEW_EXIT)
                .source(ST)
                .expression("[${PAGE_IDS}].contains(event.context.pageInteractionContext.pageId)")
                .expressionType(JEXL)
                .attributes(Set.of(attr2))
                .pageIds(Set.of(123125L, 345120L))
                .moduleIds(Set.of(3210L, 4653L))
                .clickIds(Set.of(5030L, 60802L))
                .build();

        var pageServe = unstagedEvent().toBuilder()
                .id(pageServeId)
                .name("Page experience event (test-" + pageServeId + ')')
                .description("Page experience event (test-" + pageServeId + ')')
                .type(PAGE_SERVE)
                .source(SOJ)
                .expression("[${PAGE_IDS}].contains(event.context.pageInteractionContext.pageId)")
                .expressionType(JEXL)
                .attributes(Set.of(attr3))
                .pageIds(Set.of(123125L, 345120L))
                .moduleIds(Set.of(3210L, 4653L))
                .clickIds(Set.of(5030L, 60802L))
                .build();

        var clientPageView = unstagedEvent().toBuilder()
                .id(clientPageViewId)
                .name("Client page view event (test-" + clientPageViewId + ')')
                .description("Client page view event (test-" + clientPageViewId + ')')
                .type(CLIENT_PAGE_VIEW)
                .source(SOJ)
                .expression("[${PAGE_IDS}].contains(event.context.pageInteractionContext.pageId)")
                .expressionType(JEXL)
                .attributes(Set.of(attr4))
                .pageIds(Set.of(123126L, 343120L))
                .moduleIds(Set.of())
                .clickIds(Set.of(50305L, 608023L))
                .build();

        var entryViewedTs = field(signalId, fieldId1, "viewedTs", LONG, "event.eventPayload.timestamp", PAGE_VIEW_ENTRY, Set.of(attr1));
        var exitExitedTs = field(signalId, fieldId2, "exitedTs", LONG, "event.eventPayload.timestamp", PAGE_VIEW_EXIT, Set.of(attr2));
        var exitDwell = field(signalId, fieldId3, "dwell", LONG, "event.eventPayload.timestamp - viewedTs", PAGE_VIEW_EXIT, Set.of(attr2));
        var serveServedTs = field(signalId, fieldId4, "servedTs", LONG, "event.eventPayload.timestamp", PAGE_SERVE, Set.of(attr3));
        var clientViewedTs = field(signalId, fieldId5, "viewedTs", LONG, "event.eventPayload.timestamp", CLIENT_PAGE_VIEW, Set.of(attr4));

        writeService.upsertEvent(pageViewEntry);
        writeService.upsertEvent(pageViewExit);
        writeService.upsertEvent(pageServe);
        writeService.upsertEvent(clientPageView);

        writeService.upsertAttribute(pageViewEntryId, attr1);
        writeService.upsertAttribute(pageViewExitId, attr2);
        writeService.upsertAttribute(pageServeId, attr3);
        writeService.upsertAttribute(clientPageViewId, attr4);

        var signal = unstagedSignal(id).toBuilder()
                .name("PageImpression (test-" + id + ')')
                .description("PageImpression")
                .type(PAGE_IMPRESSION_SIGNAL)
                .events(Set.of(pageViewEntry))
                .fields(Set.of())
                .build();
        signal.setSignalId(signalId);

        writeService.upsertSignal(Set.of(pageViewEntryId, pageViewExitId, pageServeId, clientPageViewId), signal);
        writeService.upsertField(signalId.getId(), Set.of(pageViewEntryId), entryViewedTs);
        writeService.upsertField(signalId.getId(), Set.of(pageViewExitId), exitExitedTs);
        writeService.upsertField(signalId.getId(), Set.of(pageViewExitId), exitDwell);
        writeService.upsertField(signalId.getId(), Set.of(pageServeId), serveServedTs);
        writeService.upsertField(signalId.getId(), Set.of(clientPageViewId), clientViewedTs);
    }

    private static UnstagedAttribute timestamp(long eventId) {
        return unstagedAttribute(eventId).toBuilder()
                .id(eventId)
                .tag(TIMESTAMP)
                .description(TIMESTAMP)
                .schemaPath("event.eventPayload.timestamp")
                .build();
    }

    private UnstagedField field(VersionedId signalId, long fieldId, String name, JavaType javaType,
                                String expression, String eventTypes, Set<UnstagedAttribute> attributes) {
        return UnstagedField.builder()
                .id(fieldId)
                .signalId(signalId.getId())
                .signalVersion(signalId.getVersion())
                .name(name)
                .tag(name)
                .description(name)
                .javaType(javaType)
                .isMandatory(true)
                .expression(expression)
                .expressionType(JEXL)
                .eventTypes(eventTypes)
                .attributes(attributes)
                .build();
    }
}
