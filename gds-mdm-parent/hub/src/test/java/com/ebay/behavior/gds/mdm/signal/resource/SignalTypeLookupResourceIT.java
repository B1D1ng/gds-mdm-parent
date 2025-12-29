package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalTypeLookup;
import com.ebay.behavior.gds.mdm.signal.model.manyToMany.SignalTypeDimensionMapping;
import com.ebay.behavior.gds.mdm.signal.model.manyToMany.SignalTypePhysicalStorageMapping;
import com.ebay.behavior.gds.mdm.signal.service.SignalTypeLookupService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import static com.ebay.behavior.gds.mdm.common.model.search.Search.IS_MANDATORY;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.CJS_PLATFORM_ID;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.signalType;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.LOOKUP;
import static org.assertj.core.api.Assertions.assertThat;

class SignalTypeLookupResourceIT extends AbstractResourceTest {

    @Autowired
    private SignalTypeLookupService service;

    private SignalTypeLookup lookup;
    private final long dimId = 0L; // dimension id 0 is defined in data.sql, signal_dim_type_lookup table, domain dimension

    @BeforeEach
    void setUp() {
        url = getBaseUrl() + V1 + LOOKUP + "/signal-type";
        lookup = service.getByName(signalType().getName());
    }

    @Test
    void create() {
        var signalType = SignalTypeLookup.builder()
                .name("TEST")
                .readableName("TEST name")
                .platformId(CJS_PLATFORM_ID)
                .logicalDataEntity("touchpoint")
                .build();

        var created = requestSpecWithBody(signalType)
                .when().post(url)
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getObject(".", SignalTypeLookup.class);

        assertThat(created.getId()).isNotNull();
    }

    @Test
    void update() {
        var updated = requestSpecWithBody(lookup)
                .when().put(url + '/' + lookup.getId())
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", SignalTypeLookup.class);

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
        var signalTypes = requestSpec()
                .when().get(url)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", SignalTypeLookup.class);

        assertThat(signalTypes.size()).isGreaterThanOrEqualTo(1);
        assertThat(signalTypes.get(0).getReadableName()).isNotBlank();
    }

    @Test
    void getByName() {
        var signalTypes = requestSpec()
                .when().get(url + "?name=" + lookup.getName())
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getList(".", SignalTypeLookup.class);

        assertThat(signalTypes.size()).isEqualTo(1);
        assertThat(signalTypes.get(0).getReadableName()).isNotBlank();
    }

    @Test
    void getById() {
        var result = requestSpec()
                .when().get(url + '/' + lookup.getId())
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", SignalTypeLookup.class);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(lookup.getId());
    }

    @Test
    void createPhysicalStorageMapping() {
        var signalType = SignalTypeLookup.builder()
                .name(getRandomSmallString())
                .readableName(getRandomSmallString())
                .platformId(CJS_PLATFORM_ID)
                .logicalDataEntity("touchpoint")
                .build();
        signalType = service.create(signalType);

        var created = requestSpec()
                .when().post(url + '/' + signalType.getId() + "/physical-storage/" + 123) // physicalStorage id 123 is defined under data.sql
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getObject(".", SignalTypePhysicalStorageMapping.class);

        assertThat(created.getId()).isNotNull();
    }

    @Test
    void deletePhysicalStorageMapping() {
        requestSpec()
                .when().delete(url + '/' + lookup.getId() + "/physical-storage/" + 999) // now such physicalStorage id
                .then().statusCode(HttpStatus.EXPECTATION_FAILED.value());
    }

    @Test
    void createAndUpdateDimensionMapping() {
        // create a signal type -> dimension mapping with isMandatory = true
        var created = requestSpec()
                .queryParam(IS_MANDATORY, true)
                .when().post(url + '/' + lookup.getId() + "/dimension/" + dimId)
                .then().statusCode(HttpStatus.CREATED.value())
                .extract().body().jsonPath().getObject(".", SignalTypeDimensionMapping.class);
        assertThat(created.getDimension().getId()).isEqualTo(dimId);
        assertThat(created.getSignalType().getId()).isEqualTo(lookup.getId());
        assertThat(created.getIsMandatory()).isTrue(); // isMandatory = true by default

        // update this signal type -> dimension mapping to isMandatory = false
        var updated = requestSpec()
                .when().put(url + '/' + lookup.getId() + "/dimension/" + dimId + "?isMandatory=false")
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", SignalTypeDimensionMapping.class);

        assertThat(updated.getSignalType().getId()).isEqualTo(lookup.getId());
        assertThat(updated.getDimension().getId()).isEqualTo(dimId);
        assertThat(updated.getIsMandatory()).isFalse();
    }

    @Test
    void deleteDimensionMapping() {
        var signalType = signalType().toBuilder().name(getRandomSmallString()).readableName(getRandomSmallString()).build();
        signalType = service.create(signalType);
        service.createDimensionMapping(signalType.getId(), dimId, false);

        requestSpec()
                .when().delete(url + '/' + signalType.getId() + "/dimension/" + dimId)
                .then().statusCode(HttpStatus.NO_CONTENT.value());
    }
}
