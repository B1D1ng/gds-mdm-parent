package com.ebay.behavior.gds.mdm.commonTestUtil;

import com.ebay.behavior.gds.mdm.common.model.SortMixin;
import com.ebay.platform.raptor.cosadaptor.token.ISecureTokenManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.specification.RequestSpecification;
import javax.annotation.PostConstruct;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.APP_PATH;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.RESOURCE_TEST;

@ActiveProfiles(IT)
@Tag(RESOURCE_TEST)
@Tag(INTEGRATION_TEST)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractResourceTest {

    protected String url;

    @Autowired
    protected ObjectMapper objectMapper;

    @LocalServerPort
    private int port;

    @Autowired
    private ISecureTokenManager tokenGenerator;

    @PostConstruct
    private void configureObjectMapper() {
        objectMapper.addMixIn(Sort.class, SortMixin.class);
    }

    protected String getBaseUrl() {
        return "http://localhost:" + port + APP_PATH;
    }

    @SneakyThrows
    protected String getToken() {
        return tokenGenerator.getToken().getAccessToken();
    }

    @SneakyThrows
    protected RequestSpecification given() {
        return RestAssured.given().contentType(ContentType.JSON);
    }

    @SneakyThrows
    protected RequestSpecification givenAuthorized() {
        return RestAssured.given().contentType(ContentType.JSON)
                .header(new Header("Authorization", getToken()));
    }
}
