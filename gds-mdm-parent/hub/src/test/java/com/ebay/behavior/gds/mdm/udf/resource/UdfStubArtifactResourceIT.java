package com.ebay.behavior.gds.mdm.udf.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfStubArtifact;
import com.ebay.behavior.gds.mdm.udf.testUtil.TestModelUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.udf.util.UdfUtils.UDFMM;
import static org.assertj.core.api.Assertions.assertThat;

class UdfStubArtifactResourceIT extends AbstractResourceTest {

    private UdfStubArtifact udfStubArtifact;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + UDFMM + "/udf/stub/artifact";
        udfStubArtifact = TestModelUtils.udfStubArtifact();
    }

    @Test
    void create() {
        var created = requestSpecWithBody(udfStubArtifact)
                .when().post(url)
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getObject(".", UdfStubArtifact.class);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getVersion()).isEqualTo("1.0.0-SNAPSHOT");
    }
}
