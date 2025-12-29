package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.model.search.RelationalSearchRequest.Filter;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedSignal;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;

import lombok.val;
import org.junit.jupiter.api.BeforeAll;
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
import static com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType.TEST;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.DATA_SOURCE;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.DESCRIPTION;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.DOMAIN;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.TYPE;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.CONTAINS;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.CONTAINS_IGNORE_CASE;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.EXACT_MATCH;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.EXACT_MATCH_IGNORE_CASE;
import static com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion.STARTS_WITH;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.CJS_PLATFORM_ID;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.searchRequest;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.stagedAttribute;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.stagedEvent;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.stagedField;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.stagedSignal;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static java.util.Locale.US;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class StagedSignalServiceViewIT {

    @Autowired
    private StagedSignalService service;

    @Autowired
    private StagedEventService eventService;

    @Autowired
    private StagedAttributeService attributeService;

    @Autowired
    private StagedFieldService fieldService;

    @Autowired
    private PlanService planService;

    private StagedSignal signal2;
    private StagedSignal signal3;

    private final Filter dataSourceFilter = new Filter(DATA_SOURCE, EXACT_MATCH_IGNORE_CASE, TEST.getValue());

    @BeforeAll
    void setUpAll() {
        var plan = TestModelUtils.plan();
        var planId = planService.create(plan).getId();

        var event = stagedEvent();
        var eventId = eventService.create(event).getId();

        var attribute = stagedAttribute(eventId);
        var attributeId = attributeService.create(attribute).getId();

        var id = getRandomLong();
        var signal1 = stagedSignal(planId).toBuilder()
                .id(id)
                .version(1)
                .environment(PRODUCTION)
                .dataSource(TEST)
                .build();
        signal2 = stagedSignal(planId).toBuilder()
                .id(id)
                .version(2)
                .environment(PRODUCTION)
                .dataSource(TEST)
                .build();
        signal3 = stagedSignal(planId).toBuilder()
                .id(id)
                .version(3)
                .environment(STAGING)
                .dataSource(TEST)
                .build();
        signal1 = service.create(signal1);
        signal2 = service.create(signal2);
        signal3 = service.create(signal3);

        var field = stagedField(signal1.getSignalId());
        fieldService.create(field, Set.of(attributeId));
    }

    @Test
    void searchStagingLatestVersions_byNameExactMatch() {
        var term = signal3.getName();
        var upperCaseFilter = new Filter(NAME, EXACT_MATCH_IGNORE_CASE, term.toUpperCase(US));
        var lowerCaseFilter = new Filter(NAME, EXACT_MATCH_IGNORE_CASE, term.toLowerCase(US));
        var upperCaseRequest = searchRequest(UPDATE_DATE, ASC, dataSourceFilter, upperCaseFilter);
        var lowerCaseRequest = searchRequest(UPDATE_DATE, ASC, dataSourceFilter, lowerCaseFilter);

        var page1 = service.searchStagingLatestVersions(true, upperCaseRequest);
        var page2 = service.searchStagingLatestVersions(true, lowerCaseRequest);

        var persisted1 = page1.getContent().get(0);
        assertThat(page1.getContent()).hasSize(1);
        assertThat(persisted1.getName()).isEqualTo(term);

        var persisted2 = page2.getContent().get(0);
        assertThat(page2.getContent()).hasSize(1);
        assertThat(persisted2.getName()).isEqualTo(term);
    }

    @Test
    void searchStagingLatestVersions_byTypeStartsWith() {
        var term = signal3.getType().substring(0, 5);
        var filter = new Filter(TYPE, STARTS_WITH, term);
        var request = searchRequest(UPDATE_DATE, ASC, dataSourceFilter, filter);

        var page = service.searchStagingLatestVersions(false, request);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getType()).contains(term);
    }

    @Test
    void searchStagingLatestVersions_byDescriptionContains() {
        var term = signal3.getDescription().substring(3);
        var filter = new Filter(DESCRIPTION, CONTAINS, term);
        var request = searchRequest(UPDATE_DATE, DESC, dataSourceFilter, filter);

        var page = service.searchStagingLatestVersions(false, request);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getDescription()).contains(term);
    }

    @Test
    @Transactional
    void searchProductionLatestVersions_byDomainContains() {
        var term = signal2.getDomain();
        var filter = new Filter(DOMAIN, EXACT_MATCH, term);
        var request = searchRequest(UPDATE_DATE, DESC, dataSourceFilter, filter);

        var page = service.searchProductionLatestVersions(false, request);

        assertThat(page.getContent().size()).isEqualTo(1);
        assertThat(page.getContent().get(0).getDomain()).isEqualTo(term);
    }

    @Test
    void searchProductionLatestVersions_byNameAndPlatform() {
        var nameFilter = new Filter(NAME, CONTAINS_IGNORE_CASE, signal2.getName().toLowerCase(US));
        var domainFilter = new Filter(DOMAIN, EXACT_MATCH_IGNORE_CASE, signal2.getDomain().toLowerCase(US));
        var request = searchRequest(UPDATE_DATE, DESC, dataSourceFilter, nameFilter, domainFilter);

        var page = service.searchProductionLatestVersions(false, request);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getName()).isEqualTo(signal2.getName());
        assertThat(page.getContent().get(0).getDomain()).isEqualTo(signal2.getDomain());
    }

    @Test
    void searchProductionLatestVersions_byDataSource() {
        var filter = new Filter(DATA_SOURCE, EXACT_MATCH_IGNORE_CASE, signal2.getDataSource().getValue());
        var request = searchRequest(UPDATE_DATE, DESC, filter);

        var page = service.searchProductionLatestVersions(false, request);
        assertThat(page.getContent()).isNotEmpty();

        filter = new Filter(DATA_SOURCE, EXACT_MATCH_IGNORE_CASE, "no_such_data_source");
        request = searchRequest(UPDATE_DATE, DESC, filter);

        page = service.searchProductionLatestVersions(false, request);
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void getAllStagingLatestVersions() {
        // use service.getAllStagingLatestVersions(STAGED, CJS) for performance testing
        var signals = service.getAllStagingLatestVersions(TEST, CJS_PLATFORM_ID);

        assertThat(signals.size()).isEqualTo(1);
        val signal = signals.iterator().next();
        assertThat(signal.getVersion()).isEqualTo(signal3.getVersion());
    }

    @Test
    void getAllStagingLatestVersionsCached() {
        var signals = service.getAllStagingLatestVersionsCached(TEST, CJS_PLATFORM_ID);

        assertThat(signals.size()).isEqualTo(1);
        val signal = signals.iterator().next();
        assertThat(signal.getVersion()).isEqualTo(signal3.getVersion());
    }

    @Test
    void getAllProductionLatestVersions() {
        var signals = service.getAllProductionLatestVersions(TEST, CJS_PLATFORM_ID);

        assertThat(signals.size()).isEqualTo(1);
        val signal = signals.iterator().next();
        assertThat(signal.getVersion()).isEqualTo(signal2.getVersion());
    }

    @Test
    void getAllProductionLatestVersionsCached() {
        var signals = service.getAllProductionLatestVersionsCached(TEST, CJS_PLATFORM_ID);

        assertThat(signals.size()).isEqualTo(1);
        val signal = signals.iterator().next();
        assertThat(signal.getVersion()).isEqualTo(signal2.getVersion());
    }
}
