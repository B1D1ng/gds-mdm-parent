package com.ebay.behavior.gds.mdm.contract.resource;

import com.ebay.behavior.gds.mdm.common.model.search.RelationalSearchRequest;
import com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion;
import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.commonTestUtil.PageHelper;
import com.ebay.behavior.gds.mdm.contract.model.BesSource;
import com.ebay.behavior.gds.mdm.contract.service.BesSourceService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomString;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.besSource;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.CMM;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.DEFINITION;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.WITH_ASSOCIATIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class BesSourceResourceIT extends AbstractResourceTest {

    private BesSource besSource;

    @Autowired
    private BesSourceService besSourceService;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + CMM + DEFINITION + "/bes-source";
        besSource = besSource(getRandomString());
    }

    @Test
    void create() {
        var created = requestSpecWithBody(besSource)
                .when().post(url)
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getObject(".", BesSource.class);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo(besSource.getName());
        assertThat(created.getConnectorType()).isEqualTo(besSource.getConnectorType());
        assertThat(created.getMetadataId()).isEqualTo(besSource.getMetadataId());
    }

    @Test
    void getById() {
        var created = besSourceService.create(besSource);
        var besSourceId = created.getId();

        var retrieved = requestSpec()
                .when().get(url + '/' + besSourceId)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", BesSource.class);

        assertThat(retrieved.getId()).isEqualTo(besSourceId);
        assertThat(retrieved.getName()).isEqualTo(besSource.getName());
        assertThat(retrieved.getConnectorType()).isEqualTo(besSource.getConnectorType());
    }

    @Test
    void getById_withAssociations() {
        var created = besSourceService.create(besSource);
        var besSourceId = created.getId();

        var retrieved = requestSpec()
                .queryParam(WITH_ASSOCIATIONS, true)
                .when().get(url + '/' + besSourceId)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", BesSource.class);

        assertThat(retrieved.getId()).isEqualTo(besSourceId);
        assertThat(retrieved.getName()).isEqualTo(besSource.getName());
    }

    @Test
    void getById_notFound() {
        requestSpec()
                .when().get(url + '/' + getRandomLong())
                .then().statusCode(HttpStatus.EXPECTATION_FAILED.value());
    }

    @Test
    void update() {
        var created = besSourceService.create(besSource);
        var besSourceId = created.getId();
        var newName = getRandomString();
        var updateRequest = created.toBuilder().name(newName).build();

        var updated = requestSpecWithBody(updateRequest)
                .when().patch(url + '/' + besSourceId)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", BesSource.class);

        assertThat(updated.getId()).isEqualTo(besSourceId);
        assertThat(updated.getName()).isEqualTo(newName);
        assertThat(updated.getConnectorType()).isEqualTo(besSource.getConnectorType());
    }

    @Test
    void update_notFound() {
        var updateRequest = besSource.toBuilder().id(getRandomLong()).revision(1).build();

        requestSpecWithBody(updateRequest)
                .when().patch(url + '/' + updateRequest.getId())
                .then().statusCode(HttpStatus.EXPECTATION_FAILED.value());
    }

    @Test
    void delete() {
        var created = besSourceService.create(besSource);
        var besSourceId = created.getId();

        requestSpec()
                .when().delete(url + '/' + besSourceId)
                .then().statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void delete_notFound() {
        requestSpec()
                .when().delete(url + '/' + getRandomLong())
                .then().statusCode(HttpStatus.EXPECTATION_FAILED.value());
    }

    @Test
    void search() throws JsonProcessingException {
        var namePrefix = "testBesSource";
        var besSource1 = besSource(namePrefix + "1");
        var besSource2 = besSource(namePrefix + "2");
        besSourceService.create(besSource1);
        besSourceService.create(besSource2);

        var searchRequest = RelationalSearchRequest.builder()
                .filters(java.util.List.of(
                        RelationalSearchRequest.Filter.builder()
                                .field("name")
                                .operator(SearchCriterion.CONTAINS)
                                .value(namePrefix)
                                .build()
                ))
                .pageNumber(0)
                .pageSize(10)
                .build();

        var json = requestSpecWithBody(searchRequest)
                .when().put(url)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().asString();

        var page = objectMapper.readValue(json, new TypeReference<PageHelper<BesSource>>() {
        });

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
        assertThat(page.getContent().size()).isGreaterThanOrEqualTo(2);
        assertThat(page.getContent()).allMatch(source -> source.getName().contains(namePrefix));
    }

    @Test
    void search_withAssociations() throws JsonProcessingException {
        var namePrefix = "testBesSourceWithAssoc";
        var besSource1 = besSource(namePrefix + "1");
        besSourceService.create(besSource1);

        var searchRequest = RelationalSearchRequest.builder()
                .filters(java.util.List.of(
                        RelationalSearchRequest.Filter.builder()
                                .field("name")
                                .operator(SearchCriterion.CONTAINS)
                                .value(namePrefix)
                                .build()
                ))
                .pageNumber(0)
                .pageSize(10)
                .build();

        var json = requestSpecWithBody(searchRequest)
                .queryParam(WITH_ASSOCIATIONS, true)
                .when().put(url)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().asString();

        var page = objectMapper.readValue(json, new TypeReference<PageHelper<BesSource>>() {
        });

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(page.getContent().size()).isGreaterThanOrEqualTo(1);
        assertThat(page.getContent()).allMatch(source -> source.getName().contains(namePrefix));
    }

    @Test
    void search_badRequest() {
        var invalidSearchRequest = RelationalSearchRequest.builder().build();

        requestSpecWithBody(invalidSearchRequest)
                .when().put(url)
                .then().statusCode(HttpStatus.BAD_REQUEST.value());
    }
}