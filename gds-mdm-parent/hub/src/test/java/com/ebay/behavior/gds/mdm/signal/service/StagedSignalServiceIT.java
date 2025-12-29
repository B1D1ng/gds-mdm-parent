package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.audit.AuditLogParams;
import com.ebay.behavior.gds.mdm.common.model.audit.AuditMode;
import com.ebay.behavior.gds.mdm.common.model.audit.ChangeType;
import com.ebay.behavior.gds.mdm.common.model.search.RelationalSearchRequest.Filter;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedSignal;
import com.ebay.behavior.gds.mdm.signal.repository.StagedSignalRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.StagedFieldAttributeMappingRepository;
import com.ebay.behavior.gds.mdm.signal.repository.manyToMany.StagedSignalEventMappingRepository;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;
import com.ebay.behavior.gds.mdm.commonTestUtil.TestRequestContextUtils;

import jakarta.ws.rs.ForbiddenException;
import lombok.val;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.Auditable.UPDATE_DATE;
import static com.ebay.behavior.gds.mdm.common.model.Environment.PRODUCTION;
import static com.ebay.behavior.gds.mdm.common.model.Environment.STAGING;
import static com.ebay.behavior.gds.mdm.common.model.Environment.UNSTAGED;
import static com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType.TEST;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.DATA_SOURCE;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.DESCRIPTION;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.DOMAIN;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.ENVIRONMENT;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.TYPE;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.CONTAINS;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.CONTAINS_IGNORE_CASE;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.EXACT_MATCH;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.EXACT_MATCH_IGNORE_CASE;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.STARTS_WITH;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.service.SignalTemplateActionService.CLIENT_PAGE_VIEW;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestAuthFilter.IT_TEST_USER;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.CJS_PLATFORM_ID;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.searchRequest;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.stagedAttribute;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.stagedEvent;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.stagedField;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.stagedSignal;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedSignal;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static java.util.Locale.US;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class StagedSignalServiceIT {

    @Autowired
    private StagedSignalService service;

    @Autowired
    private StagedSignalRepository repository;

    @Autowired
    private StagedEventService eventService;

    @Autowired
    private StagedAttributeService attributeService;

    @Autowired
    private StagedFieldService fieldService;

    @Autowired
    private PlanService planService;

    @Autowired
    private UnstagedSignalService unstagedSignalService;

    @Autowired
    private StagedFieldAttributeMappingRepository fieldAttributeMappingRepository;

    @Autowired
    private StagedSignalEventMappingRepository signalEventMappingRepository;

    private long planId;
    private long eventId;
    private long attributeId;
    private long fieldId;
    private VersionedId signalId;
    private StagedSignal signal;

    private final Filter dataSourceFilter = new Filter(DATA_SOURCE, EXACT_MATCH_IGNORE_CASE, TEST.getValue());
    private final Filter stagingFilter = new Filter(ENVIRONMENT, EXACT_MATCH_IGNORE_CASE, STAGING.name());

    @BeforeAll
    void setUpAll() {
        var plan = TestModelUtils.plan();
        planId = planService.create(plan).getId();
    }

    @BeforeEach
    void setUp() {
        var unstagedSignal = unstagedSignal(planId).toBuilder().dataSource(TEST).build();
        unstagedSignal = unstagedSignalService.create(unstagedSignal);

        var event = stagedEvent();
        eventId = eventService.create(event).getId();

        var attribute = stagedAttribute(eventId);
        attributeId = attributeService.create(attribute).getId();

        signal = stagedSignal(planId).toBuilder()
                .id(unstagedSignal.getId())
                .version(unstagedSignal.getVersion())
                .environment(STAGING)
                .dataSource(TEST)
                .build();
        signal = service.create(signal);
        signal = service.getById(signal.getSignalId());
        signalId = signal.getSignalId();

        var field = stagedField(signalId);
        fieldId = fieldService.create(field, Set.of(attributeId)).getId();
    }

    @Test
    void create() {
        assertThat(signal.getId()).isEqualTo(signalId.getId());
        assertThat(signal.getVersion()).isEqualTo(signalId.getVersion());
    }

    @Test
    void create_withoutId_error() {
        signal = stagedSignal(1L);

        assertThatThrownBy(() -> service.create(signal))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteMigrated_admin() {
        TestRequestContextUtils.setUser(IT_TEST_USER);
        assertThat(unstagedSignalService.findById(signalId)).isPresent();
        assertThat(service.findById(signalId)).isPresent();

        signal.setLegacyId(getRandomSmallString());
        signal = repository.save(signal);

        service.deleteMigrated(Set.of(signalId));

        assertThat(unstagedSignalService.findById(signalId)).isEmpty();
        assertThat(service.findById(signalId)).isEmpty();
        assertThat(eventService.findById(eventId)).isEmpty();
        assertThat(attributeService.findById(attributeId)).isEmpty();
        assertThat(fieldService.findById(fieldId)).isEmpty();
        assertThat(fieldAttributeMappingRepository.findByFieldId(fieldId)).isEmpty();
        assertThat(signalEventMappingRepository.findBySignalIdAndSignalVersion(signalId.getId(), signalId.getVersion())).isEmpty();
    }

    @Test
    void deleteMigrated_notAdmin_error() {
        TestRequestContextUtils.setUser("anyUser");

        assertThatThrownBy(() -> service.deleteMigrated(signalId))
                .isInstanceOf(ForbiddenException.class);
    }

    // TODO temporarily disabled to permit deleting test signals without legacyId
    @Test
    @Disabled
    void deleteMigrated_noLegacyId_error() {
        TestRequestContextUtils.setUser(IT_TEST_USER);

        assertThatThrownBy(() -> service.deleteMigrated(signalId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getById_nonExistentId_error() {
        assertThatThrownBy(() -> service.getById(VersionedId.of(999L, 1)))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void getByIdWithAssociations_initializesAssociations() {
        var persisted = service.getByIdWithAssociations(signalId);

        assertThat(persisted.getFields()).hasSize(1);
        assertThat(persisted.getEvents()).hasSize(1);
    }

    @Test
    void getByIdWithAssociationsRecursive_initializesAssociations() {
        var persisted = service.getByIdWithAssociationsRecursive(signalId);

        var attribute = persisted.getFields().iterator().next().getAttributes().iterator().next();
        assertThat(attribute.getEvent().getId()).isEqualTo(eventId);
        assertThat(persisted.getFields()).hasSize(1);
        assertThat(persisted.getEvents()).hasSize(1);
    }

    @Test
    void getEnrichedById() {
        var unstagedSignal = unstagedSignal(planId).toBuilder().dataSource(TEST).build();
        unstagedSignal = unstagedSignalService.create(unstagedSignal);

        signal = signal.toBuilder()
                .id(unstagedSignal.getId())
                .version(unstagedSignal.getVersion() + 1)
                .environment(STAGING)
                .dataSource(TEST)
                .build();
        signal = service.create(signal);
        signalId = signal.getSignalId();

        var enriched = service.getEnrichedById(signalId);

        assertThat(enriched.getId()).isEqualTo(unstagedSignal.getId());
        assertThat(enriched.getFieldGroups()).isNotNull();

        var proxy = enriched.getUnstagedSignal();
        assertThat(proxy).isNotNull();
        assertThat(proxy.getIsUnstaged()).isTrue();
        assertThat(proxy.getPlanId()).isEqualTo(planId);
        assertThat(proxy.getPlanName()).isEqualTo(planService.getById(planId).getName());
        assertThat(proxy.getVersion()).isEqualTo(unstagedSignal.getVersion());
        assertThat(proxy.getEnvironment()).isEqualTo(UNSTAGED);
    }

    @Test
    void getLatestVersionById() {
        var stagingSignal = service.getLatestVersionById(signalId.getId(), STAGING);
        assertThat(stagingSignal.getSignalId()).isEqualTo(signalId);

        assertThatThrownBy(() -> service.getLatestVersionById(signalId.getId(), PRODUCTION))
                .isInstanceOf(DataNotFoundException.class);
    }

    @Test
    void getFields() {
        var fields = service.getFields(signalId);
        assertThat(fields).hasSize(1);
    }

    @Test
    void getEvents() {
        var events = service.getEvents(signalId);
        assertThat(events).hasSize(1);
    }

    @Test
    void getEventIds() {
        var result = service.getEventIds(Set.of(signalId));

        assertThat(result.size()).isGreaterThanOrEqualTo(1);
        assertThat(result).extracting("signalId").containsOnly(signalId.getId());
        assertThat(result).extracting("signalVersion").containsOnly(signalId.getVersion());
    }

    @Test
    void getFieldIds() {
        var result = service.getFieldIds(Set.of(signalId));

        assertThat(result.size()).isGreaterThanOrEqualTo(1);
        assertThat(result).extracting("signalId").containsOnly(signalId.getId());
        assertThat(result).extracting("signalVersion").containsOnly(signalId.getVersion());
    }

    @Test
    void searchAllVersions_wrongDataSourceFilter_notFound() {
        var filter = new Filter(ENVIRONMENT, EXACT_MATCH_IGNORE_CASE, "no_such_data_source");
        var request = searchRequest(UPDATE_DATE, ASC, filter);

        var page = service.searchAllVersions(false, request);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(0);
    }

    @Test
    void searchAllVersions_byNameExactMatch() {
        var term = signal.getName();
        var upperCaseFilter = new Filter(NAME, EXACT_MATCH_IGNORE_CASE, term.toUpperCase(US));
        var lowerCaseFilter = new Filter(NAME, EXACT_MATCH_IGNORE_CASE, term.toLowerCase(US));
        var upperCaseRequest = searchRequest(UPDATE_DATE, ASC, stagingFilter, dataSourceFilter, upperCaseFilter);
        var lowerCaseRequest = searchRequest(UPDATE_DATE, ASC, stagingFilter, dataSourceFilter, lowerCaseFilter);

        var page1 = service.searchAllVersions(true, upperCaseRequest);
        var page2 = service.searchAllVersions(true, lowerCaseRequest);

        var persisted1 = page1.getContent().get(0);
        assertThat(page1.getContent()).hasSize(1);
        assertThat(persisted1.getName()).isEqualTo(term);

        var persisted2 = page2.getContent().get(0);
        assertThat(page2.getContent()).hasSize(1);
        assertThat(persisted2.getName()).isEqualTo(term);
    }

    @Test
    void searchAllVersions_byTypeStartsWith() {
        var term = signal.getType().substring(0, 5);
        var filter = new Filter(TYPE, STARTS_WITH, term);
        var request = searchRequest(UPDATE_DATE, ASC, stagingFilter, dataSourceFilter, filter);

        var page = service.searchAllVersions(false, request);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getType()).contains(term);
    }

    @Test
    void searchAllVersions_byDescriptionContains() {
        var term = signal.getDescription().substring(3);
        var filter = new Filter(DESCRIPTION, CONTAINS, term);
        var request = searchRequest(UPDATE_DATE, DESC, stagingFilter, dataSourceFilter, filter);

        var page = service.searchAllVersions(false, request);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getDescription()).contains(term);
    }

    @Test
    @Transactional
    void searchAllVersions_byDomainContains() {
        var term = signal.getDomain();
        var filter = new Filter(DOMAIN, EXACT_MATCH, term);
        var request = searchRequest(UPDATE_DATE, DESC, stagingFilter, dataSourceFilter, filter);

        var page = service.searchAllVersions(false, request);

        assertThat(page.getContent().size()).isGreaterThanOrEqualTo(1);
        assertThat(page.getContent().get(0).getDomain()).isEqualTo(term);
    }

    @Test
    void searchAllVersions_byNameAndPlatform() {
        var nameFilter = new Filter(NAME, CONTAINS_IGNORE_CASE, signal.getName().toLowerCase(US));
        var domainFilter = new Filter(DOMAIN, EXACT_MATCH_IGNORE_CASE, signal.getDomain().toLowerCase(US));
        var request = searchRequest(UPDATE_DATE, DESC, stagingFilter, dataSourceFilter, nameFilter, domainFilter);

        var page = service.searchAllVersions(false, request);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getName()).isEqualTo(signal.getName());
        assertThat(page.getContent().get(0).getDomain()).isEqualTo(signal.getDomain());
    }

    @Test
    void searchAllVersions_byDataSource() {
        var filter = new Filter(DATA_SOURCE, EXACT_MATCH_IGNORE_CASE, signal.getDataSource().getValue());
        var request = searchRequest(UPDATE_DATE, DESC, stagingFilter, filter);

        var page = service.searchAllVersions(false, request);
        assertThat(page.getContent()).isNotEmpty();

        filter = new Filter(DATA_SOURCE, EXACT_MATCH_IGNORE_CASE, "no_such_data_source");
        request = searchRequest(UPDATE_DATE, DESC, stagingFilter, filter);

        page = service.searchAllVersions(false, request);
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void searchAllVersions_withLegacyFormat_byNameAndPlatform() {
        var nameFilter = new Filter(NAME, CONTAINS_IGNORE_CASE, signal.getName().toLowerCase(US));
        var domainFilter = new Filter(DOMAIN, EXACT_MATCH_IGNORE_CASE, signal.getDomain().toLowerCase(US));
        var request = searchRequest(UPDATE_DATE, DESC, stagingFilter, dataSourceFilter, nameFilter, domainFilter);

        var page = service.searchAllVersions(false, request);
        var legacyPage = service.toSignalDefinitionsPage(page);

        var content = legacyPage.getContent();
        assertThat(content).hasSize(1);
        assertThat(legacyPage.getTotalElements()).isEqualTo(page.getTotalElements());
        assertThat(legacyPage.getSize()).isEqualTo(page.getSize());
        assertThat(legacyPage.getPageable().getPageSize()).isEqualTo(page.getPageable().getPageSize());
        assertThat(legacyPage.getPageable().getPageNumber()).isEqualTo(page.getPageable().getPageNumber());
    }

    @Test
    void getAllVersions_staging() {
        var id = getRandomLong();
        var signal1 = stagedSignal(planId).toBuilder()
                .id(id)
                .version(1)
                .environment(STAGING)
                .dataSource(TEST)
                .build();
        var signal2 = stagedSignal(planId).toBuilder()
                .id(id)
                .version(2)
                .environment(PRODUCTION) // PRODUCTION signal actually also a STAGING signal
                .dataSource(TEST)
                .build();
        service.create(signal1);
        service.create(signal2);

        var signals = service.getAllVersions(STAGING, TEST, CJS_PLATFORM_ID);

        assertThat(signals.size()).isGreaterThanOrEqualTo(2);
        val signal = signals.iterator().next();
        assertThat(signal.getDataSource()).isEqualTo(TEST);
    }

    @Test
    void getAllVersions_production() {
        var id = getRandomLong();
        var signal1 = stagedSignal(planId).toBuilder()
                .id(id)
                .version(1)
                .environment(PRODUCTION)
                .dataSource(TEST)
                .build();
        var signal2 = stagedSignal(planId).toBuilder()
                .id(id)
                .version(2)
                .environment(PRODUCTION)
                .dataSource(TEST)
                .build();
        service.create(signal1);
        service.create(signal2);

        var signals = service.getAllVersions(PRODUCTION, TEST, CJS_PLATFORM_ID);

        assertThat(signals.size()).isGreaterThanOrEqualTo(2);
        val signal = signals.iterator().next();
        assertThat(signal.getEnvironment()).isEqualTo(PRODUCTION);
        assertThat(signal.getDataSource()).isEqualTo(TEST);
    }

    @Test
    void getAllVersionsCached() {
        service.getAllVersionsCached(STAGING, TEST, CJS_PLATFORM_ID);
        var signals = service.getAllVersionsCached(STAGING, TEST, CJS_PLATFORM_ID);

        assertThat(signals.size()).isGreaterThanOrEqualTo(1);
        val signal = signals.iterator().next();
        assertThat(signal.getDataSource()).isEqualTo(TEST);
    }

    @Test
    void toSignalDefinitions() {
        var signals = service.getAllVersions(STAGING, TEST, CJS_PLATFORM_ID);

        val signalDefinitions = service.toSignalDefinitions(signals);

        assertThat(signalDefinitions.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void toSignalDefinitions_fieldsNotInitialized_error() {
        var term = signal.getDomain();
        var filter = new Filter(DOMAIN, EXACT_MATCH, term);
        var request = searchRequest(UPDATE_DATE, DESC, stagingFilter, dataSourceFilter, filter);
        var page = service.searchAllVersions(false, request);
        var signals = page.getContent(); // fields not initialized with searchAllVersions()

        assertThatThrownBy(() -> service.toSignalDefinitions(signals))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getAuditLog_fullMode() {
        // Given
        var id = getRandomLong();
        var params = AuditLogParams.ofNonVersioned(id, AuditMode.FULL);

        var signal1 = stagedSignal(planId).toBuilder()
                .id(id)
                .version(1)
                .environment(PRODUCTION)
                .build();
        signal1 = service.create(signal1);

        var event1 = stagedEvent();
        event1.setName("event1");
        var eventId1 = eventService.create(event1).getId();

        var attribute1 = stagedAttribute(eventId1);
        attribute1.setTag("tag1");
        var attributeId1 = attributeService.create(attribute1).getId();
        var attribute3 = stagedAttribute(eventId1);
        var attributeId3 = attributeService.create(attribute3).getId();

        var field1 = stagedField(signal1.getSignalId()).toBuilder().tag("field1").build();
        field1.populateAuditKey();
        fieldService.create(field1, Set.of(attributeId1, attributeId3));

        var signal2 = stagedSignal(planId).toBuilder()
                .id(id)
                .version(2)
                .environment(PRODUCTION)
                .build();
        signal2 = service.create(signal2);

        var event2 = event1.toBuilder()
                .id(null)
                .revision(null)
                .name("event2")
                .type("type2")
                .build();
        var eventId2 = eventService.create(event2).getId();

        var attribute2 = attribute1.toBuilder()
                .id(null)
                .revision(null)
                .eventId(eventId2)
                .tag("tag2")
                .description("desc2")
                .build();
        var attributeId2 = attributeService.create(attribute2).getId();

        var attribute4 = attribute3.toBuilder()
                .id(null)
                .revision(null)
                .eventId(eventId2)
                .build();
        var attributeId4 = attributeService.create(attribute4).getId();

        var field2 = field1.toBuilder()
                .signalId(signal2.getId())
                .signalVersion(signal2.getVersion())
                .id(null)
                .revision(null)
                .tag("tag2")
                .eventTypes(CLIENT_PAGE_VIEW)
                .build();
        field2.populateAuditKey();
        fieldService.create(field2, Set.of(attributeId2, attributeId4));

        // When
        val records = service.getAuditLog(TEST, params);

        // Then
        assertThat(records).hasSize(2);
        val record1 = records.get(0);
        assertThat(record1.getUuid()).isNotBlank();
        assertThat(record1.getChangeType()).isEqualTo(ChangeType.CREATED);
        assertThat(record1.getDiff()).isNotNull();
        assertThat(record1.getLeft()).isNull();
        assertThat(record1.getRight().getId()).isEqualTo(signal1.getId());

        val record2 = records.get(1);
        assertThat(record2.getUuid()).isNotBlank();
        assertThat(record2.getChangeType()).isEqualTo(ChangeType.UPDATED);
        assertThat(record2.getDiff()).isNotNull();
        assertThat(record2.getLeft().getId()).isEqualTo(signal1.getId());
        assertThat(record2.getRight().getId()).isEqualTo(signal2.getId());
    }
}
