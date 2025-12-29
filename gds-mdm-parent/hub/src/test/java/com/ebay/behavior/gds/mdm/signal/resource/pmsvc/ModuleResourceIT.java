package com.ebay.behavior.gds.mdm.signal.resource.pmsvc;

import com.ebay.behavior.gds.mdm.common.model.search.SearchCriterion;
import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.datatype.ModuleV1;
import com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.request.SearchIn;
import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_BY;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_CRITERION;
import static com.ebay.behavior.gds.mdm.common.model.search.Search.SEARCH_TERM;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.EXPECTATION_FAILED;
import static org.springframework.http.HttpStatus.OK;

class ModuleResourceIT extends AbstractResourceTest {

    private final long id = 1_682;
    private String url;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + "/metadata/module";
    }

    @Test
    void getById() {
        var persisted = requestSpec().when().get(url + '/' + id)
                .then().statusCode(OK.value())
                .extract().body().jsonPath().getObject(".", ModuleV1.class);

        assertThat(persisted.getId()).isEqualTo(id);
    }

    @Test
    void getById_notFound_417() {
        requestSpec()
                .when().get(url + "/999999999")
                .then().statusCode(EXPECTATION_FAILED.value());
    }

    @Test
    void getByIds() {
        var models = requestSpec()
                .queryParam("ids", String.format("%d,%d", id, id))
                .when().get(url)
                .then().statusCode(OK.value())
                .extract().body().jsonPath().getList(".", ModuleV1.class);

        assertThat(models.size()).isEqualTo(1);
        assertThat(models.get(0).getId()).isEqualTo(id);
    }

    @Test
    void getAll() {
        var models = requestSpec()
                .queryParam(SEARCH_TERM, String.valueOf(id))
                .queryParam(SEARCH_BY, SearchIn.ID.name())
                .queryParam(SEARCH_CRITERION, SearchCriterion.EXACT_MATCH.name())
                .when().get(url + "/search")
                .then().statusCode(OK.value())
                .extract().body().jsonPath().getList(".", ModuleV1.class);

        assertThat(models.size()).isEqualTo(1);
        assertThat(models.get(0).getId()).isEqualTo(id);
    }
}