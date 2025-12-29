package com.ebay.behavior.gds.mdm.signal.mockResource;

import com.ebay.behavior.gds.mdm.common.util.TimeUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.datatype.SignalDefinition;
import com.ebay.behavior.gds.mdm.signal.common.service.MetadataWriteService;
import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.signal.resource.ProductionSignalMigrationResource;
import com.ebay.behavior.gds.mdm.signal.service.migration.LegacySignalReadService;
import com.ebay.behavior.gds.mdm.commonTestUtil.JsonUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import static com.ebay.behavior.gds.mdm.common.model.search.Search.PLATFORM;
import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.signal.util.ImportUtils.CJS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class ProductionSignalMigrationResourceIT extends AbstractResourceTest {

    @MockitoBean
    private MetadataWriteService metadataWriteService;

    @MockitoBean
    private LegacySignalReadService legacySignalReadService;

    private final String name = "Account_Created";

    @BeforeEach
    void setUp() throws IOException {
        url = getBaseUrl() + V1 + "/migration/signal";

        var content = JsonUtils.loadJsonFile("legacyData/testSignal.json");
        List<SignalDefinition> signals = objectMapper.readValue(content, new TypeReference<>() {
        });
        signals = signals.stream()
                .filter(signal -> signal.getName().equals(name))
                .sorted(Comparator.comparingInt(SignalDefinition::getVersion))
                .toList();

        doReturn("signal:0").when(metadataWriteService).upsert(any(), any());
        doReturn(signals).when(legacySignalReadService).readAll(CJS);
    }

    @Test
    void migrateAll() {
        val message = requestSpec()
                .queryParam(PLATFORM, CJS)
                .when().post(url + "/bulk")
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().asString();

        assertThat(message).startsWith("Migration job is running");
    }

    @Test
    void migrateSignal() {
        TimeUtils.sleepSeconds(5);

        val response = requestSpec()
                .queryParam(PLATFORM, CJS)
                .when().post(url + "/name/" + name)
                .then().statusCode(OK.value())
                .and().contentType(APPLICATION_JSON_VALUE)
                .extract().body().jsonPath().getObject(".", ProductionSignalMigrationResource.SignalMigrationResponse.class);

        val statuses = response.statuses();
        assertThat(response.hostname()).isNotBlank();
        assertThat(statuses).hasSize(1);
        assertThat(statuses.get(0).getSignalName()).isEqualTo(name);
        assertThat(statuses.get(0).isOk()).isTrue();
    }
}
