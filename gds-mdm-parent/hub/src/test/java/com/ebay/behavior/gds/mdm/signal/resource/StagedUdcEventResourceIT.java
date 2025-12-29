package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.model.EsPage;
import com.ebay.behavior.gds.mdm.common.model.EsPageable;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.common.service.udc.UdcQueryBuilder;
import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedEvent;
import com.ebay.behavior.gds.mdm.signal.common.service.MetadataWriteService;
import com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.search.Search.NAME;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedEvent;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.ELASTICSEARCH;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.METADATA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

class StagedUdcEventResourceIT extends AbstractResourceTest {

    @Autowired
    private UdcQueryBuilder udcQueryBuilder;

    @Autowired
    private MetadataWriteService writeService;

    private final long eventId = 1L;
    private final String name = "test_event_1";

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + METADATA + ELASTICSEARCH + "/event";
    }

    @Test
    void getById() {
        injectEvent();

        var persisted = requestSpec()
                .when().get(url + '/' + eventId)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", UnstagedEvent.class);

        assertThat(persisted.getId()).isEqualTo(eventId);
    }

    @Test
    void getById_notFound() {
        requestSpec()
                .when().get(url + '/' + getRandomLong())
                .then().statusCode(HttpStatus.EXPECTATION_FAILED.value());
    }

    @Test
    void getAll() throws JsonProcessingException {
        injectEvent();

        var json = requestSpec()
                .queryParam(Search.FROM, 0)
                .queryParam(Search.SIZE, 10)
                .when().get(url)
                .then().statusCode(OK.value())
                .extract().body().asString();

        var page = objectMapper.readValue(json, new TypeReference<EsPage<UnstagedEvent>>() {
        });

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(page.getContent().size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void search() throws JsonProcessingException {
        injectEvent();

        var pageable = EsPageable.of(0, 10);
        var queryBuilder = QueryBuilders.termQuery(NAME, name);
        var searchBuilder = udcQueryBuilder.toSearchSourceBuilder(pageable, queryBuilder);

        var json = requestSpec().body(searchBuilder.toString())
                .when().put(url + "/search")
                .then().statusCode(OK.value())
                .extract().body().asString();

        var page = objectMapper.readValue(json, new TypeReference<EsPage<UnstagedEvent>>() {
        });
        assertThat(page).isNotNull();
    }

    private void injectEvent() {
        var event = unstagedEvent().toBuilder()
                .id(eventId)
                .name(name)
                .description(name)
                .attributes(Set.of())
                .pageIds(Set.of())
                .moduleIds(Set.of())
                .clickIds(Set.of())
                .build();

        writeService.upsertEvent(event);
        TestUtils.sleep(8);
    }
}