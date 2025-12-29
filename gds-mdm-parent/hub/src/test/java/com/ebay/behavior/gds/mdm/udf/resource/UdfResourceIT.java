package com.ebay.behavior.gds.mdm.udf.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.udf.common.model.Udf;
import com.ebay.behavior.gds.mdm.udf.testUtil.TestModelUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.udf.util.UdfUtils.UDFMM;
import static org.assertj.core.api.Assertions.assertThat;

class UdfResourceIT extends AbstractResourceTest {

    private Udf udf;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + UDFMM + "/udf";
        udf = TestModelUtils.udf();
    }

    @Test
    void create() {
        var created = requestSpecWithBody(udf)
                .when().post(url)
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getObject(".", Udf.class);

        assertThat(created.getId()).isNotNull();
    }

    @Test
    void getByIds() {
        var created = requestSpec()
                .when().get(url + "/ids?ids=1&withAssociations=true")
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", Udf.class);

        assertThat(created).hasSize(1);
    }

    @Test
    void getByNames() {
        var created = requestSpec()
                .when().get(url + "/names?names=initial_test_udf&withAssociations=true")
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", Udf.class);

        assertThat(created).hasSize(1);
    }
}
