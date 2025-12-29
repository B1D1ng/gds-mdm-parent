package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.model.EsPage;
import com.ebay.behavior.gds.mdm.common.model.EsPageable;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.common.service.udc.UdcQueryBuilder;
import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.service.MetadataWriteService;
import com.ebay.behavior.gds.mdm.signal.service.PlanService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedAttributeService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedEventService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedFieldService;
import com.ebay.behavior.gds.mdm.signal.service.UnstagedSignalService;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;
import com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.SIGNAL;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedAttribute;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedField;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedSignal;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.ELASTICSEARCH;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.METADATA;
import static java.util.Locale.US;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

class StagedUdcSignalResourceIT extends AbstractResourceTest {

    @Autowired
    private UdcQueryBuilder udcQueryBuilder;

    @Autowired
    private MetadataWriteService writeService;

    @Autowired
    private UnstagedSignalService unstagedSignalService;

    @Autowired
    private UnstagedAttributeService unstagedAttributeService;

    @Autowired
    private UnstagedEventService unstagedEventService;

    @Autowired
    private UnstagedFieldService unstagedFieldService;

    @Autowired
    private PlanService planService;

    private UnstagedSignal signal;
    private VersionedId signalId;
    private final String name = "test_signal_" + getRandomSmallString().toLowerCase(US);

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + METADATA + ELASTICSEARCH + "/signal";
        var plan = TestModelUtils.plan();
        var planId = planService.create(plan).getId();

        var event = TestModelUtils.unstagedEvent();
        var eventId = unstagedEventService.create(event).getId();

        var attribute = unstagedAttribute(eventId);
        var attributeId = unstagedAttributeService.create(attribute).getId();

        signal = unstagedSignal(planId).toBuilder()
                .name(name)
                .description(name)
                .owners(plan.getOwners())
                .domain(plan.getDomain())
                .signalTemplateSourceId(getRandomLong())
                .build();
        signal = unstagedSignalService.create(signal);
        signalId = signal.getSignalId();

        var field = unstagedField(signalId);
        unstagedFieldService.create(field, Set.of(attributeId));

        signal = unstagedSignalService.getByIdWithAssociationsRecursive(signalId);
        writeService.upsertSignal(Set.of(eventId), signal).get(SIGNAL);
        TestUtils.sleep(6);
    }

    @Test
    void getById() {
        var persisted = requestSpec()
                .when().get(url + '/' + signalId.getId())
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", StagedSignal.class);

        assertThat(persisted.getId()).isEqualTo(signalId.getId());
        assertThat(persisted.getOwners()).isEqualTo(signal.getOwners());
    }

    @Test
    void getById_notFound() {
        requestSpec()
                .when().get(url + '/' + getRandomLong())
                .then().statusCode(HttpStatus.EXPECTATION_FAILED.value());
    }

    @Test
    void getAll() throws JsonProcessingException {
        var json = requestSpec()
                .queryParam(Search.PAGE_NUMBER, 0)
                .queryParam(Search.PAGE_SIZE, 5)
                .when().get(url)
                .then().statusCode(OK.value())
                .extract().body().asString();

        var page = objectMapper.readValue(json, new TypeReference<EsPage<StagedSignal>>() {
        });

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(page.getContent().size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void getAll_filterByName() throws JsonProcessingException {
        var json = requestSpec()
                .queryParam(NAME, name.toUpperCase(US)) // toUpperCase tests case-insensitive search
                .when().get(url)
                .then().statusCode(OK.value())
                .extract().body().asString();

        var page = objectMapper.readValue(json, new TypeReference<EsPage<StagedSignal>>() {
        });

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(page.getContent().size()).isGreaterThanOrEqualTo(1);
        assertThat(page.getContent().get(0).getName()).isEqualTo(name);
    }

    @Test
    void search() throws JsonProcessingException {
        var pageable = EsPageable.of(0, 10);
        var queryBuilder = QueryBuilders.termQuery(NAME, name);
        var searchBuilder = udcQueryBuilder.toSearchSourceBuilder(pageable, queryBuilder);

        var json = requestSpec().body(searchBuilder.toString())
                .when().put(url + "/search")
                .then().statusCode(OK.value())
                .extract().body().asString();

        var page = objectMapper.readValue(json, new TypeReference<EsPage<StagedSignal>>() {
        });
        assertThat(page).isNotNull();
    }
}