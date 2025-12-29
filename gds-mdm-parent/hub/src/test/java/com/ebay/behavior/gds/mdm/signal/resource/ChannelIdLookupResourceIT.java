package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.common.model.ChannelIdLookup;
import com.ebay.behavior.gds.mdm.signal.service.ChannelIdLookupService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.channelId;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.LOOKUP;
import static org.assertj.core.api.Assertions.assertThat;

class ChannelIdLookupResourceIT extends AbstractResourceTest {

    @Autowired
    private ChannelIdLookupService service;

    private ChannelIdLookup lookup;

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + LOOKUP + "/channel_id/";
        lookup = service.getByName(channelId().getName());
    }

    @Test
    void create() {
        var testChannelId = ChannelIdLookup.builder().name("TEST").readableName("TEST name").build();

        var created = requestSpecWithBody(testChannelId)
                .when().post(url)
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getObject(".", ChannelIdLookup.class);

        assertThat(created.getId()).isNotNull();
    }

    @Test
    void update() {
        var updated = requestSpecWithBody(lookup)
                .when().put(url + lookup.getId())
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", ChannelIdLookup.class);

        assertThat(updated.getId()).isEqualTo(lookup.getId());
    }

    @Test
    void create_nameInUse_error() {
        requestSpecWithBody(lookup)
                .when().post(url)
                .then().statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void getAll() {
        var channelId = requestSpec()
                .when().get(url)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", ChannelIdLookup.class);

        assertThat(channelId.size()).isGreaterThanOrEqualTo(1);
        assertThat(channelId.get(0).getReadableName()).isNotBlank();
    }

    @Test
    void getByName() {
        var channelIds = requestSpec()
                .when().get(url + "?name=" + lookup.getName())
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", ChannelIdLookup.class);

        assertThat(channelIds.size()).isEqualTo(1);
        assertThat(channelIds.get(0).getReadableName()).isNotBlank();
    }

    @Test
    void getById() {
        var result = requestSpec()
                .when().get(url + lookup.getId())
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", ChannelIdLookup.class);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(lookup.getId());
    }
}
