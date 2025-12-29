package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.search.RelationalSearchRequest;
import com.ebay.behavior.gds.mdm.common.model.search.RelationalSearchRequest.Filter;
import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.common.model.FieldGroup;
import com.ebay.behavior.gds.mdm.signal.common.model.PlatformLookup;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedField;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.datatype.SignalDefinition;
import com.ebay.behavior.gds.mdm.signal.service.PlanService;
import com.ebay.behavior.gds.mdm.signal.service.PlatformLookupService;
import com.ebay.behavior.gds.mdm.signal.service.StagedSignalService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedSignalService;
import com.ebay.behavior.gds.mdm.commonTestUtil.PageHelper;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;
import com.ebay.behavior.gds.mdm.signal.util.AuditUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.Environment.PRODUCTION;
import static com.ebay.behavior.gds.mdm.common.model.Environment.STAGING;
import static com.ebay.behavior.gds.mdm.common.model.audit.AuditLogParams.MODE;
import static com.ebay.behavior.gds.mdm.common.model.audit.AuditMode.FULL;
import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.CREATED;
import static com.ebay.behavior.gds.mdm.common.model.audit.ChangeType.UPDATED;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.ENVIRONMENT;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.PLATFORM;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.EXACT_MATCH;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.EXACT_MATCH_IGNORE_CASE;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.CJS_PLATFORM_ID;
import static com.ebay.behavior.gds.mdm.signal.util.ImportUtils.EJS;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.EJS_PLATFORM_ID;
import static com.ebay.behavior.gds.mdm.signal.util.ImportUtils.ITEM;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.ITEM_PLATFORM_ID;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.stagedSignal;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.METADATA;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.USE_CACHE;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.WITH_ASSOCIATIONS;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.WITH_LATEST_VERSIONS;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.WITH_LEGACY_FORMAT;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.WITH_UNSTAGED_DETAILS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StagedSignalResourceIT extends AbstractResourceTest {

    @Autowired
    private UnstagedSignalService unstagedSignalService;

    @Autowired
    private StagedSignalService stagedSignalService;

    @Autowired
    private PlanService planService;

    @Autowired
    PlatformLookupService platformLookupService;

    private final Filter stagingFilter = new Filter(ENVIRONMENT, EXACT_MATCH_IGNORE_CASE, STAGING.name());
    private final Filter prodFilter = new Filter(ENVIRONMENT, EXACT_MATCH_IGNORE_CASE, PRODUCTION.name());

    private long planId;
    private StagedSignal signalVer1;
    private StagedSignal signalVer2;
    private StagedSignal signalVer3;
    private StagedSignal signalCjsStaging;
    private StagedSignal signalCjsProduction;
    private StagedSignal signalEjsStaging;
    private StagedSignal signalEjsProduction;

    @BeforeAll
    void setUpAll() {
        var plan = TestModelUtils.plan();
        planId = planService.create(plan).getId();

        signalVer1 = stagedSignal(planId).toBuilder()
                .id(getRandomLong())
                .version(1)
                .name("name1")
                .platformId(ITEM_PLATFORM_ID)
                .environment(PRODUCTION)
                .dataSource(UdcDataSourceType.STAGED)
                .build();

        signalVer2 = stagedSignal(planId).toBuilder()
                .id(signalVer1.getId())
                .version(2)
                .name("name2")
                .platformId(ITEM_PLATFORM_ID)
                .environment(PRODUCTION)
                .dataSource(UdcDataSourceType.STAGED)
                .refVersion(1)
                .build();

        signalVer3 = stagedSignal(planId).toBuilder()
                .id(getRandomLong())
                .version(2)
                .name("name3")
                .platformId(ITEM_PLATFORM_ID)
                .environment(STAGING)
                .dataSource(UdcDataSourceType.STAGED)
                .refVersion(2)
                .build();

        signalCjsStaging = stagedSignal(planId).toBuilder()
                .id(getRandomLong())
                .name("signalCjsStaging")
                .version(1)
                .platformId(CJS_PLATFORM_ID)
                .environment(STAGING)
                .build();

        signalCjsProduction = stagedSignal(planId).toBuilder()
                .id(getRandomLong())
                .name("signalCjsProduction")
                .version(1)
                .platformId(CJS_PLATFORM_ID)
                .environment(PRODUCTION)
                .build();

        signalEjsStaging = stagedSignal(planId).toBuilder()
                .id(getRandomLong())
                .name("signalEjsStaging")
                .version(1)
                .platformId(EJS_PLATFORM_ID)
                .environment(STAGING)
                .build();

        PlatformLookup platformLookup = new PlatformLookup();
        platformLookup.setName(EJS);
        signalEjsProduction = stagedSignal(planId).toBuilder()
                .id(getRandomLong())
                .name("signalEjsProduction")
                .version(1)
                .platformId(EJS_PLATFORM_ID)
                .environment(PRODUCTION)
                .build();

        signalVer1 = stagedSignalService.create(signalVer1);
        signalVer2 = stagedSignalService.create(signalVer2);
        signalVer3 = stagedSignalService.create(signalVer3);
        signalCjsStaging = stagedSignalService.create(signalCjsStaging);
        signalCjsProduction = stagedSignalService.create(signalCjsProduction);
        signalEjsStaging = stagedSignalService.create(signalEjsStaging);
        signalEjsProduction = stagedSignalService.create(signalEjsProduction);
    }

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + METADATA + "/signal";
    }

    @Test
    void getById_notFound() {
        requestSpec()
                .when().get(url + "/" + getRandomLong() + "/version/1")
                .then().statusCode(HttpStatus.EXPECTATION_FAILED.value());
    }

    @Test
    void getById() {
        var persisted = requestSpec()
                .when().get(url + "/" + signalVer1.getId() + "/version/" + signalVer1.getVersion())
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", StagedSignal.class);

        assertThat(persisted.getSignalId()).isEqualTo(signalVer1.getSignalId());
    }

    @Test
    void getLatestVersionById() {
        var persisted = requestSpec()
                .when().get(url + "/" + signalVer1.getId() + "/environment/" + PRODUCTION)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", StagedSignal.class);

        assertThat(persisted.getSignalId()).isEqualTo(signalVer2.getSignalId());
    }

    @Test
    void getById_withAssociations() {
        var persisted = requestSpec()
                .queryParam(WITH_ASSOCIATIONS, true)
                .when().get(url + "/" + signalVer1.getId() + "/version/" + signalVer1.getVersion())
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", StagedSignal.class);

        assertThat(persisted.getSignalId()).isEqualTo(signalVer1.getSignalId());
    }

    @Test
    void getById_withUnstagedDetails() {
        var unstagedSignal = TestModelUtils.unstagedSignal(planId).toBuilder()
                .id(signalVer1.getId())
                .version(signalVer1.getVersion())
                .name(getRandomSmallString())
                .platformId(EJS_PLATFORM_ID)
                .environment(PRODUCTION)
                .dataSource(UdcDataSourceType.STAGED)
                .build();

        unstagedSignalService.create(unstagedSignal);

        var persisted = requestSpec()
                .queryParam(WITH_UNSTAGED_DETAILS, true)
                .when().get(url + "/" + signalVer1.getId() + "/version/" + signalVer1.getVersion())
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", StagedSignal.class);

        assertThat(persisted.getSignalId()).isEqualTo(signalVer1.getSignalId());
    }

    @Test
    void getFields() {
        var fields = requestSpec()
                .when().get(url + "/" + signalVer1.getId() + "/version/" + signalVer1.getVersion() + "/fields")
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", StagedField.class);

        assertThat(fields).isNotNull();
    }

    @Test
    void getFieldGroups() {
        var fields = requestSpec()
                .when().get(url + "/" + signalVer1.getId() + "/version/" + signalVer1.getVersion() + "/field-groups")
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", FieldGroup.class);

        assertThat(fields).isNotNull();
    }

    @Test
    void getEvents() {
        var events = requestSpec()
                .when().get(url + "/" + signalVer1.getId() + "/version/" + signalVer1.getVersion() + "/events")
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", StagedEvent.class);

        assertThat(events).isNotNull();
    }

    @Test
    void getAll_withCacheLatestVersions() {
        var signals = requestSpec()
                .queryParam(PLATFORM, ITEM)
                .queryParam(USE_CACHE, false)
                .queryParam(WITH_LEGACY_FORMAT, true)
                .queryParam(WITH_LATEST_VERSIONS, true)
                .when().get(url)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", StagedSignal.class);

        assertThat(signals).hasSize(2);
        assertThat(signals).extracting(StagedSignal::getName).containsExactlyInAnyOrder(signalVer2.getName(), signalVer3.getName());
        assertThat(signals).extracting(StagedSignal::getRefVersion).containsExactlyInAnyOrder(signalVer2.getRefVersion(), signalVer3.getRefVersion());
    }

    @Test
    void getAll_withCacheAllVersions() {
        var signals = requestSpec()
                .queryParam(PLATFORM, ITEM)
                .queryParam(USE_CACHE, true)
                .queryParam(WITH_LATEST_VERSIONS, false)
                .when().get(url)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", StagedSignal.class);

        assertThat(signals).hasSize(3);
        assertThat(signals).extracting(StagedSignal::getName).containsExactlyInAnyOrder(signalVer1.getName(), signalVer2.getName(), signalVer3.getName());
    }

    @Test
    void getAll_withoutCacheLatestVersions() {
        var signals = requestSpec()
                .queryParam(PLATFORM, platformLookupService.getPlatformName(signalVer3.getPlatformId()))
                .queryParam(USE_CACHE, false)
                .queryParam(WITH_LATEST_VERSIONS, true)
                .when().get(url)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", StagedSignal.class);

        assertThat(signals).hasSize(2);
        assertThat(signals).extracting(StagedSignal::getName).containsExactlyInAnyOrder(signalVer2.getName(), signalVer3.getName());
    }

    @Test
    void getAll_withoutCacheLAllVersions() {
        var signals = requestSpec()
                .queryParam(PLATFORM, platformLookupService.getPlatformName(signalVer3.getPlatformId()))
                .queryParam(USE_CACHE, false)
                .queryParam(WITH_LATEST_VERSIONS, false)
                .when().get(url)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", StagedSignal.class);

        assertThat(signals).hasSize(3);
        assertThat(signals).extracting(StagedSignal::getName).containsExactlyInAnyOrder(signalVer1.getName(), signalVer2.getName(), signalVer3.getName());
    }

    @Test
    void searchAllVersions_platformFilter() throws JsonProcessingException {
        var filter = new Filter("platform", EXACT_MATCH, String.valueOf(signalVer3.getPlatformId()));
        var request = new RelationalSearchRequest(10, 0, null, List.of(filter));

        var json = requestSpecWithBody(request)
                .queryParam(WITH_LATEST_VERSIONS, false)
                .when().put(url)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().asString();

        var page = objectMapper.readValue(json, new TypeReference<PageHelper<StagedSignal>>() {
        });

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(1);
        assertThat(page.getContent().size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void search_cjsStaging() throws JsonProcessingException {
        var signal = signalCjsStaging;
        var filter = new Filter("platform", EXACT_MATCH, String.valueOf(signal.getPlatformId()));
        var request = new RelationalSearchRequest(10, 0, null, List.of(filter));

        var json = requestSpecWithBody(request)
                .queryParam(WITH_LATEST_VERSIONS, true)
                .when().put(url)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().asString();

        var page = objectMapper.readValue(json, new TypeReference<PageHelper<StagedSignal>>() {
        });

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(1);
        assertThat(page.getContent().size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void search_cjsProd() throws JsonProcessingException {
        var signal = signalCjsProduction;
        var filter = new Filter("platform", EXACT_MATCH, String.valueOf(signal.getPlatformId()));
        var request = new RelationalSearchRequest(10, 0, null, List.of(filter, prodFilter));

        var json = requestSpecWithBody(request)
                .queryParam(WITH_LATEST_VERSIONS, false)
                .when().put(url)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().asString();

        var page = objectMapper.readValue(json, new TypeReference<PageHelper<StagedSignal>>() {
        });

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getTotalPages()).isEqualTo(1);
        assertThat(page.getContent().size()).isEqualTo(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(signal.getId());
    }

    @Test
    void legacyFormatSearch_ejsProd() throws JsonProcessingException {
        var signal = signalEjsProduction;
        var filter = new Filter("platform", EXACT_MATCH, String.valueOf(signal.getPlatformId()));
        var request = new RelationalSearchRequest(10, 0, null, List.of(filter, prodFilter));

        var json = requestSpecWithBody(request)
                .queryParam(WITH_LATEST_VERSIONS, true)
                .queryParam(WITH_LEGACY_FORMAT, true)
                .when().put(url)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().asString();

        var page = objectMapper.readValue(json, new TypeReference<PageHelper<SignalDefinition>>() {
        });

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(signal.getId().toString());
    }

    @Test
    void legacyFormatSearch_ejsStaging() throws JsonProcessingException {
        var signal = signalEjsStaging;
        var filter = new Filter("platformId", EXACT_MATCH, String.valueOf(signal.getPlatformId()));
        var request = new RelationalSearchRequest(10, 0, null, List.of(filter, stagingFilter));

        var json = requestSpecWithBody(request)
                .queryParam(WITH_LEGACY_FORMAT, true)
                .when().put(url)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().asString();

        var page = objectMapper.readValue(json, new TypeReference<PageHelper<SignalDefinition>>() {
        });

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(signal.getId().toString());
    }

    @Test
    void getAuditLog_twoProductionSignals() {
        var json = requestSpec()
                .queryParam(MODE, FULL)
                .when().get(url + '/' + signalVer1.getId() + "/auditLog")
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().asString();

        var auditRecords = AuditUtils.deserializeAuditRecords(json, objectMapper, StagedSignal.class);

        assertThat(auditRecords.size()).isEqualTo(2); // since two versions of the signal were created

        var record1 = auditRecords.get(0);
        assertThat(record1.getLeft()).isNull();
        assertThat(record1.getChangeType()).isEqualTo(CREATED);
        assertThat(record1.getRight().getId()).isEqualTo(signalVer1.getId());
        assertThat(record1.getRight().getVersion()).isEqualTo(signalVer1.getVersion());

        var record2 = auditRecords.get(1);
        assertThat(record2.getLeft().getId()).isEqualTo(signalVer1.getId());
        assertThat(record2.getLeft().getVersion()).isEqualTo(signalVer1.getVersion());
        assertThat(record2.getChangeType()).isEqualTo(UPDATED);
        assertThat(record2.getRight().getId()).isEqualTo(signalVer2.getId());
        assertThat(record2.getRight().getVersion()).isEqualTo(signalVer2.getVersion());
    }

    @Test
    void deleteMigrated() {
        var id = getRandomLong();
        var unstagedSignal = TestModelUtils.unstagedSignal(planId).toBuilder()
                .id(id)
                .version(1)
                .name(getRandomSmallString())
                .platformId(EJS_PLATFORM_ID)
                .environment(PRODUCTION)
                .dataSource(UdcDataSourceType.STAGED)
                .build();

        unstagedSignalService.create(unstagedSignal);

        var signal = stagedSignal(planId).toBuilder()
                .id(unstagedSignal.getId())
                .name("signal")
                .version(unstagedSignal.getVersion())
                .legacyId(getRandomSmallString())
                .platformId(EJS_PLATFORM_ID)
                .environment(PRODUCTION)
                .build();
        signal = stagedSignalService.create(signal);

        requestSpec()
                .when().delete(url + "/" + signal.getId() + "/version/" + signal.getVersion())
                .then().statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void deleteMigrated_bulk() {
        var id = getRandomLong();
        var unstagedSignal = TestModelUtils.unstagedSignal(planId).toBuilder()
                .id(id)
                .version(1)
                .name(getRandomSmallString())
                .platformId(EJS_PLATFORM_ID)
                .environment(PRODUCTION)
                .dataSource(UdcDataSourceType.STAGED)
                .build();

        unstagedSignalService.create(unstagedSignal);

        var signal = stagedSignal(planId).toBuilder()
                .id(unstagedSignal.getId())
                .name("signal")
                .version(unstagedSignal.getVersion())
                .legacyId(getRandomSmallString())
                .platformId(EJS_PLATFORM_ID)
                .environment(PRODUCTION)
                .build();
        signal = stagedSignalService.create(signal);

        requestSpecWithBody(Set.of(signal.getSignalId()))
                .when().delete(url + "/bulk")
                .then().statusCode(HttpStatus.NO_CONTENT.value());
    }
}
