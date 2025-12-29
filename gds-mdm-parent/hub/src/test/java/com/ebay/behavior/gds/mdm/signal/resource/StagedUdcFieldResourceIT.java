package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.model.EsPage;
import com.ebay.behavior.gds.mdm.common.model.EsPageable;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.model.search.Search;
import com.ebay.behavior.gds.mdm.common.service.udc.UdcQueryBuilder;
import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
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

import static com.ebay.behavior.gds.mdm.common.model.VersionedId.MIN_VERSION;
import static com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType.SIGNAL;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedAttribute;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedEvent;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedField;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedSignal;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.ELASTICSEARCH;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.METADATA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

class StagedUdcFieldResourceIT extends AbstractResourceTest {

    @Autowired
    private UdcQueryBuilder udcQueryBuilder;

    @Autowired
    private MetadataWriteService writeService;

    private final long fieldId = 1L;
    private final String tag = "test_field_1";

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + METADATA + ELASTICSEARCH + "/field";
        long planId = 1L;
        VersionedId signalId = VersionedId.of(1L, MIN_VERSION);
        long eventId = 1L;
        long attributeId = 1L;

        var event = unstagedEvent().toBuilder()
                .id(eventId)
                .name("test_event_1")
                .description("test_event_1")
                .attributes(Set.of())
                .pageIds(Set.of())
                .moduleIds(Set.of())
                .clickIds(Set.of())
                .build();
        writeService.upsertEvent(event);

        var attribute = unstagedAttribute(eventId).toBuilder()
                .id(attributeId)
                .tag("test_attribute_1")
                .description("test_attribute_1")
                .build();
        writeService.upsertAttribute(eventId, attribute);

        var signal = unstagedSignal(planId).toBuilder()
                .name("test_signal_1")
                .description("test_signal_1")
                .events(Set.of(event))
                .fields(Set.of())
                .build();
        signal.setSignalId(signalId);
        writeService.upsertSignal(Set.of(eventId), signal).get(SIGNAL);

        var field = unstagedField(signalId).toBuilder()
                .id(fieldId)
                .tag(tag)
                .description(tag)
                .attributes(Set.of())
                .build();

        writeService.upsertField(signalId.getId(), Set.of(attributeId), field);
        TestUtils.sleep(3);
    }

    @Test
    void getById() {
        var persisted = requestSpec()
                .when().get(url + '/' + fieldId)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", UnstagedField.class);

        assertThat(persisted.getId()).isEqualTo(fieldId);
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
                .queryParam(Search.FROM, 0)
                .queryParam(Search.SIZE, 10)
                .when().get(url)
                .then().statusCode(OK.value())
                .extract().body().asString();

        var page = objectMapper.readValue(json, new TypeReference<EsPage<UnstagedField>>() {
        });

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(page.getContent().size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void search() throws JsonProcessingException {
        var pageable = EsPageable.of(0, 10);
        var queryBuilder = QueryBuilders.termQuery("tag", tag);
        var searchBuilder = udcQueryBuilder.toSearchSourceBuilder(pageable, queryBuilder);

        var json = requestSpec().body(searchBuilder.toString())
                .when().put(url + "/search")
                .then().statusCode(OK.value())
                .extract().body().asString();

        var page = objectMapper.readValue(json, new TypeReference<EsPage<UnstagedField>>() {
        });
        assertThat(page).isNotNull();
    }
}
