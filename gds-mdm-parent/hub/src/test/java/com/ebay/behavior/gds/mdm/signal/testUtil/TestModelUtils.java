package com.ebay.behavior.gds.mdm.signal.testUtil;

import com.ebay.behavior.gds.mdm.common.model.CompletionStatus;
import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.model.ExpressionType;
import com.ebay.behavior.gds.mdm.common.model.JavaType;
import com.ebay.behavior.gds.mdm.common.model.SurfaceType;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.audit.ChangeType;
import com.ebay.behavior.gds.mdm.common.model.search.RelationalSearchRequest;
import com.ebay.behavior.gds.mdm.common.util.RandomUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.AttributeTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.BusinessOutcomeLookup;
import com.ebay.behavior.gds.mdm.signal.common.model.BusinessTagNotification;
import com.ebay.behavior.gds.mdm.signal.common.model.ChannelIdLookup;
import com.ebay.behavior.gds.mdm.signal.common.model.EventSource;
import com.ebay.behavior.gds.mdm.signal.common.model.EventTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.EventTypeLookup;
import com.ebay.behavior.gds.mdm.signal.common.model.FieldTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.HadoopSojEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.Plan;
import com.ebay.behavior.gds.mdm.signal.common.model.PlanStatus;
import com.ebay.behavior.gds.mdm.signal.common.model.PlatformLookup;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalDimTypeLookup;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalDimValueLookup;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalPhysicalStorage;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalTypeLookup;
import com.ebay.behavior.gds.mdm.signal.common.model.SojBusinessTag;
import com.ebay.behavior.gds.mdm.signal.common.model.SojEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.SojPlatformTag;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedAttribute;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.model.SurfaceTypeLookup;
import com.ebay.behavior.gds.mdm.signal.common.model.TemplateQuestion;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedAttribute;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.model.UpdateUnstagedEventRequest;
import com.ebay.behavior.gds.mdm.signal.common.model.audit.EventTemplateHistory;
import com.ebay.behavior.gds.mdm.signal.common.model.audit.PlanHistory;
import com.ebay.behavior.gds.mdm.signal.common.model.audit.SignalTemplateHistory;
import com.ebay.behavior.gds.mdm.signal.common.model.audit.UnstagedEventHistory;
import com.ebay.behavior.gds.mdm.signal.common.model.audit.UnstagedSignalHistory;
import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.datatype.PropertyV1;
import com.ebay.behavior.gds.mdm.signal.common.model.migration.SignalMigrationJobStatus;
import com.ebay.behavior.gds.mdm.signal.model.migration.SignalMigrationJob;
import com.ebay.cos.raptor.error.v3.ErrorCategoryEnumV3;
import com.ebay.cos.raptor.error.v3.ErrorDetailV3;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import lombok.experimental.UtilityClass;
import lombok.val;
import org.apache.avro.Schema;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Locale;

import static com.ebay.behavior.gds.mdm.common.util.TimeUtils.toNowSqlTimestamp;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.PAGE_IMPRESSION_SIGNAL;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.PAGE_SERVE;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestAuthFilter.IT_TEST_USER;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomEmail;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomString;

@UtilityClass
@SuppressWarnings("PMD.CouplingBetweenObjects")
public class TestModelUtils {

    public static final String IT_ = "IT_";
    public static final String VI_DOMAIN = "VI";
    public static final long CJS_PLATFORM_ID = 1L;
    public static final long EJS_PLATFORM_ID = 2L;
    public static final long ITEM_PLATFORM_ID = 3L;

    public static ErrorMessageV3 errorMessage(String message) {
        val detail = new ErrorDetailV3();
        detail.setErrorId(RandomUtils.getRandomLong(1000));
        detail.setDomain(getRandomSmallString());
        detail.setCategory(ErrorCategoryEnumV3.APPLICATION.name());
        detail.setSubdomain(getRandomSmallString());
        detail.setLongMessage(message);
        detail.setMessage(message);

        return new ErrorMessageV3(detail);
    }

    public static Plan plan() {
        return Plan.builder()
                .name(getRandomString())
                .description(getRandomString())
                .teamDls(getRandomEmail())
                .owners(IT_TEST_USER)
                .jiraProject(IT_ + getRandomString(5))
                .domain(VI_DOMAIN)
                .platformId(CJS_PLATFORM_ID)
                .status(PlanStatus.CREATED)
                .build();
    }

    public static PlanHistory planHistory() {
        return PlanHistory.builder()
                .name(getRandomString())
                .description(getRandomString())
                .teamDls(getRandomEmail())
                .owners(getRandomSmallString())
                .jiraProject(IT_ + getRandomString(5))
                .domain(VI_DOMAIN)
                .status(PlanStatus.CREATED)
                .originalId(RandomUtils.getRandomLong(1000))
                .originalRevision(0)
                .originalCreateDate(toNowSqlTimestamp())
                .originalUpdateDate(toNowSqlTimestamp())
                .changeType(ChangeType.CREATED)
                .build();
    }

    public static FieldTemplate fieldTemplate(long signalId) {
        return FieldTemplate.builder()
                .signalTemplateId(signalId)
                .name(getRandomString())
                .description(getRandomString())
                .tag(getRandomString())
                .javaType(JavaType.STRING)
                .avroSchema(Schema.create(Schema.Type.STRING))
                .isMandatory(true)
                .expression(getRandomString())
                .expressionType(ExpressionType.JEXL)
                .build();
    }

    public static UpdateUnstagedEventRequest updateEventRequest() {
        return UpdateUnstagedEventRequest.builder()
                .description(getRandomString())
                .expression(getRandomString())
                .expressionType(ExpressionType.JEXL)
                .fsmOrder(1)
                .cardinality(1)
                .build();
    }

    public static SignalTemplate signalTemplate() {
        return SignalTemplate.builder()
                .name(getRandomSmallString())
                .description(getRandomString())
                .domain(VI_DOMAIN)
                .type(getRandomSmallString())
                .platformId(CJS_PLATFORM_ID)
                .build();
    }

    public static SignalTemplateHistory signalTemplateHistory() {
        return SignalTemplateHistory.builder()
                .name(getRandomSmallString())
                .description(getRandomString())
                .domain(VI_DOMAIN)
                .type(getRandomSmallString())
                .completionStatus(CompletionStatus.COMPLETED)
                .platformId(CJS_PLATFORM_ID)
                .originalId(RandomUtils.getRandomLong(1000))
                .originalRevision(0)
                .originalCreateDate(toNowSqlTimestamp())
                .originalUpdateDate(toNowSqlTimestamp())
                .changeType(ChangeType.CREATED)
                .build();
    }

    public static EventTemplate eventTemplate() {
        return EventTemplate.builder()
                .name(getRandomSmallString())
                .description(getRandomString())
                .type(PAGE_SERVE)
                .source(EventSource.SOJ)
                .surfaceType(SurfaceType.RAPTOR_IO)
                .expression(getRandomString())
                .expressionType(ExpressionType.LITERAL)
                .isMandatory(true)
                .build();
    }

    public static EventTemplateHistory eventTemplateHistory() {
        return EventTemplateHistory.builder()
                .name(getRandomSmallString())
                .description(getRandomString())
                .type(getRandomSmallString())
                .source(EventSource.SOJ)
                .surfaceType(SurfaceType.RAPTOR_IO)
                .fsmOrder(99_999)
                .cardinality(1)
                .expression(getRandomString())
                .expressionType(ExpressionType.LITERAL)
                .originalId(RandomUtils.getRandomLong(1000))
                .originalRevision(0)
                .originalCreateDate(toNowSqlTimestamp())
                .originalUpdateDate(toNowSqlTimestamp())
                .changeType(ChangeType.CREATED)
                .build();
    }

    public static AttributeTemplate attributeTemplate(long eventId) {
        return AttributeTemplate.builder()
                .eventTemplateId(eventId)
                .tag(getRandomSmallString())
                .description(getRandomString())
                .javaType(JavaType.STRING)
                .schemaPath(getRandomString())
                .build();
    }

    public static TemplateQuestion templateQuestion() {
        return TemplateQuestion.builder()
                .question(getRandomString())
                .description(getRandomString())
                .answerJavaType(JavaType.STRING)
                .answerPropertyName(getRandomString())
                .isList(false)
                .isMandatory(true)
                .build();
    }

    public static UnstagedEvent unstagedEvent() {
        return UnstagedEvent.builder()
                .name(getRandomString())
                .description(getRandomString())
                .type(getRandomSmallString())
                .source(EventSource.SOJ)
                .surfaceType(SurfaceType.RAPTOR_IO)
                .githubRepositoryUrl("https://github.corp.ebay.com/customer-journey-signal/gds-mdm-parent")
                .expression(getRandomString())
                .expressionType(ExpressionType.LITERAL)
                .build();
    }

    public static UnstagedEventHistory unstagedEventHistory() {
        return UnstagedEventHistory.builder()
                .name(getRandomString())
                .description(getRandomString())
                .type(getRandomSmallString())
                .source(EventSource.SOJ)
                .surfaceType(SurfaceType.RAPTOR_IO)
                .githubRepositoryUrl("https://github.corp.ebay.com/customer-journey-signal/gds-mdm-parent")
                .fsmOrder(99_999)
                .cardinality(1)
                .expression(getRandomString())
                .expressionType(ExpressionType.LITERAL)
                .originalId(RandomUtils.getRandomLong(1000))
                .originalRevision(0)
                .originalCreateDate(toNowSqlTimestamp())
                .originalUpdateDate(toNowSqlTimestamp())
                .changeType(ChangeType.CREATED)
                .build();
    }

    public static UnstagedAttribute unstagedAttribute(long eventId) {
        return UnstagedAttribute.builder()
                .eventId(eventId)
                .tag(getRandomSmallString())
                .description(getRandomString())
                .javaType(JavaType.STRING)
                .schemaPath(getRandomString())
                .isStoreInState(true)
                .build();
    }

    public static UnstagedField unstagedField(VersionedId signalId) {
        return UnstagedField.builder()
                .signalId(signalId.getId())
                .signalVersion(signalId.getVersion())
                .name(getRandomString())
                .description(getRandomString())
                .tag(getRandomString())
                .javaType(JavaType.STRING)
                .avroSchema(Schema.create(Schema.Type.STRING))
                .isMandatory(false)
                .isCached(false)
                .eventTypes(PAGE_SERVE)
                .expression(getRandomString())
                .expressionType(ExpressionType.JEXL)
                .build();
    }

    public static UnstagedSignal unstagedSignal(long planId) {
        return UnstagedSignal.builder()
                .planId(planId)
                .name(getRandomSmallString())
                .description(getRandomString())
                .domain(VI_DOMAIN)
                .retentionPeriod(1L)
                .owners(getRandomSmallString())
                .type(getRandomSmallString())
                .platformId(CJS_PLATFORM_ID)
                .dataSource(UdcDataSourceType.TEST)
                .build();
    }

    public static UnstagedSignalHistory unstagedSignalHistory() {
        return UnstagedSignalHistory.builder()
                .planId(RandomUtils.getRandomLong(1000))
                .name(getRandomSmallString())
                .description(getRandomString())
                .domain(VI_DOMAIN)
                .type(getRandomSmallString())
                .completionStatus(CompletionStatus.COMPLETED)
                .environment(Environment.UNSTAGED)
                .platformId(CJS_PLATFORM_ID)
                .originalId(RandomUtils.getRandomLong(1000))
                .originalVersion(1)
                .originalRevision(0)
                .originalCreateDate(toNowSqlTimestamp())
                .originalUpdateDate(toNowSqlTimestamp())
                .changeType(ChangeType.CREATED)
                .build();
    }

    public static StagedAttribute stagedAttribute(long eventId) {
        return StagedAttribute.builder()
                .eventId(eventId)
                .tag(getRandomSmallString())
                .description(getRandomString())
                .javaType(JavaType.STRING)
                .schemaPath(getRandomString())
                .isStoreInState(true)
                .build();
    }

    public static StagedEvent stagedEvent() {
        return StagedEvent.builder()
                .name(getRandomString())
                .description(getRandomString())
                .type(getRandomSmallString())
                .source(EventSource.SOJ)
                .surfaceType(SurfaceType.RAPTOR_IO)
                .githubRepositoryUrl("https://github.corp.ebay.com/customer-journey-signal/gds-mdm-parent")
                .expression(getRandomString())
                .expressionType(ExpressionType.LITERAL)
                .build();
    }

    public static StagedField stagedField(VersionedId signalId) {
        return StagedField.builder()
                .signalId(signalId.getId())
                .signalVersion(signalId.getVersion())
                .name(getRandomString())
                .description(getRandomString())
                .tag(getRandomString())
                .javaType(JavaType.STRING)
                .avroSchema(Schema.create(Schema.Type.STRING))
                .isMandatory(false)
                .isCached(false)
                .eventTypes(PAGE_SERVE)
                .expression(getRandomString())
                .expressionType(ExpressionType.JEXL)
                .build();
    }

    public static StagedSignal stagedSignal(long planId) {
        return StagedSignal.builder()
                .planId(planId)
                .name(getRandomSmallString())
                .description(getRandomString())
                .domain(VI_DOMAIN)
                .retentionPeriod(1L)
                .owners(getRandomSmallString())
                .type(getRandomSmallString())
                .platformId(CJS_PLATFORM_ID)
                .environment(Environment.STAGING)
                .dataSource(UdcDataSourceType.TEST)
                .build();
    }

    public static SignalDimValueLookup domain(String name) {
        return SignalDimValueLookup.builder()
                .name(name)
                .readableName(name)
                .dimensionTypeId(0L)  // id from data.sql - platform dimension id
                .build();
    }

    public static SignalDimTypeLookup signalDimTypeLookup(String name) {
        return SignalDimTypeLookup.builder()
                .name(name.toUpperCase(Locale.US))
                .readableName(name)
                .build();
    }

    public static PlatformLookup platform(String name) {
        return PlatformLookup.builder()
                .name(name)
                .readableName(name)
                .build();
    }

    public static ChannelIdLookup channelId() {
        return channelId("ePN", 1);
    }

    public static ChannelIdLookup channelId(String channelName, int channelId) {
        return ChannelIdLookup.builder()
                .name(channelName)
                .readableName(channelName)
                .channelId(channelId)
                .build();
    }

    public static EventTypeLookup eventType() {
        return EventTypeLookup.builder()
                .name(PAGE_SERVE)
                .readableName("Page serve")
                .build();
    }

    public static SignalTypeLookup signalType() {
        return SignalTypeLookup.builder()
                .name(PAGE_IMPRESSION_SIGNAL)
                .readableName("Page Impression")
                .platformId(CJS_PLATFORM_ID)
                .logicalDataEntity("touchpoint")
                .build();
    }

    public static SurfaceTypeLookup surfaceType() {
        return SurfaceTypeLookup.builder()
                .name("RAPTOR_IO")
                .readableName("Raptor.io")
                .build();
    }

    public static BusinessOutcomeLookup businessOutcome() {
        return BusinessOutcomeLookup.builder()
                .name("bin")
                .readableName("Buy it Now")
                .eventType("BBOWAC:BIN")
                .build();
    }

    public static BusinessTagNotification businessTagNotification() {
        return BusinessTagNotification.builder()
                .jobName("CJS MDM Business Tags")
                .runDate("2024-12-01")
                .table("business_tags")
                .status("OK")
                .build();
    }

    public static SojEvent sojEvent(String action, Long pageId, Long moduleId, Long clickId) {
        return SojEvent.builder()
                .action(action)
                .pageId(pageId)
                .moduleId(moduleId)
                .clickId(clickId)
                .build();
    }

    public static SojBusinessTag sojBusinessTag(String tag) {
        return SojBusinessTag.builder()
                .sojName(tag)
                .name(tag)
                .description(tag)
                .dataType(JavaType.STRING.toString())
                .schemaPath(getRandomSmallString())
                .build();
    }

    public static SojPlatformTag sojPlatformTag(String tag) {
        return SojPlatformTag.builder()
                .sojName(tag)
                .name(tag)
                .description(tag)
                .dataType(JavaType.STRING.toString())
                .schemaPath(getRandomSmallString())
                .build();
    }

    public static HadoopSojEvent sojResult(String eactn, Long pageId, Long moduleId, Long clickId, String tags) {
        return HadoopSojEvent.builder()
                .eactn(eactn)
                .pageId(pageId)
                .moduleId(moduleId)
                .clickId(clickId)
                .tags(tags)
                .build();
    }

    public static PropertyV1 pmsvcTag(String sojName, String name) {
        return PropertyV1.builder()
                .sojName(sojName)
                .name(name)
                .description(getRandomString())
                .dataType("java.lang.String")
                .build();
    }

    public static RelationalSearchRequest searchRequest(String sortBy, Sort.Direction direction, RelationalSearchRequest.Filter... filters) {
        val sort = new RelationalSearchRequest.SortRequest(sortBy, direction);
        return new RelationalSearchRequest(100, 0, sort, List.of(filters));
    }

    public static SignalMigrationJob signalMigrationJob() {
        return SignalMigrationJob.builder()
                .jobId(RandomUtils.getRandomLong(1000))
                .status(SignalMigrationJobStatus.STARTED)
                .createBy(IT_TEST_USER)
                .updateBy(IT_TEST_USER)
                .build();
    }

    public static SignalPhysicalStorage physicalStorage() {
        return SignalPhysicalStorage.builder()
                .environment(Environment.PRODUCTION)
                .description("Physical Storage")
                .kafkaTopic(getRandomSmallString())
                .kafkaSchema("schema...")
                .hiveTableName("hive_table_test")
                .doneFilePath("/data/warehouse/path")
                .build();
    }
}
