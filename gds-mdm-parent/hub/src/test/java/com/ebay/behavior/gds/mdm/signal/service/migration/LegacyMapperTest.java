package com.ebay.behavior.gds.mdm.signal.service.migration;

import com.ebay.behavior.gds.mdm.common.config.UdcConfiguration;
import com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType;
import com.ebay.behavior.gds.mdm.common.model.VersionedId;
import com.ebay.behavior.gds.mdm.common.util.RandomUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.StagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.LegacySignalRecord;
import com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.datatype.SignalDefinition;
import com.ebay.behavior.gds.mdm.signal.repository.SojPlatformTagRepository;
import com.ebay.behavior.gds.mdm.commonTestUtil.JsonUtils;

import com.ebay.behavior.gds.mdm.signal.service.PlatformLookupService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.Environment.PRODUCTION;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestMigrationUtils.assertSignal;
import static com.ebay.behavior.gds.mdm.signal.util.ImportUtils.CJS;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.CJS_PLATFORM_ID;
import static com.ebay.behavior.gds.mdm.signal.util.ImportUtils.EJS;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.EJS_PLATFORM_ID;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.stagedEvent;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.stagedField;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedEvent;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedField;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedSignal;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomString;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class LegacyMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private LegacyMapper legacyMapper;

    @Mock
    private SojPlatformTagRepository platformTagRepository;

    @Mock
    private PlatformLookupService platformService;

    @BeforeEach
    void setUp() {
        var config = new UdcConfiguration();
        config.setDataSource(UdcDataSourceType.TEST);

        legacyMapper = new LegacyMapper(platformService, platformTagRepository, config);
        legacyMapper.init();
    }

    private LegacySignalRecord signalRecord() throws IOException {
        val content = JsonUtils.loadJsonFile("legacyData/testSignal_SRP_PageImpression.json");
        val signals = objectMapper.readValue(content, new TypeReference<List<SignalDefinition>>() {
        });
        signals.sort(Comparator.comparingInt(SignalDefinition::getVersion));
        return new LegacySignalRecord(signals.get(0).getId(), signals);
    }

    @Test
    void map_signalRecord_cjs() throws IOException {
        Mockito.doReturn(CJS_PLATFORM_ID).when(platformService).getPlatformId(CJS);
        val signalRecord = signalRecord();
        val planId = RandomUtils.getRandomLong(10_000L);

        val signalVersions = legacyMapper.mapLegacySignalRecord(signalRecord, planId);

        assertThat(signalVersions).hasSize(3);

        for (int i = 0; i < signalVersions.size(); i++) {
            val signal = signalVersions.get(i);
            assertSignal(signalRecord.getVersions().get(i), signal);
            assertThat(signal.getEnvironment()).isEqualTo(PRODUCTION);
            assertThat(signal.getPlatformId()).isEqualTo(CJS_PLATFORM_ID);
        }
    }

    @Test
    void map_signalRecord_ejs() throws IOException {
        Mockito.doReturn(EJS_PLATFORM_ID).when(platformService).getPlatformId(EJS);
        val signalRecord = signalRecord();
        val planId = RandomUtils.getRandomLong(10_000L);
        signalRecord.getVersions().stream()
                .flatMap(sd -> sd.getLogicalDefinition().stream())
                .forEach(ld -> ld.setPlatform("ejs"));

        val signalVersions = legacyMapper.mapLegacySignalRecord(signalRecord, planId);

        assertThat(signalVersions).hasSize(3);

        for (int i = 0; i < signalVersions.size(); i++) {
            val signal = signalVersions.get(i);
            assertSignal(signalRecord.getVersions().get(i), signal);
            assertThat(signal.getEnvironment()).isEqualTo(PRODUCTION);
            assertThat(signal.getPlatformId()).isEqualTo(EJS_PLATFORM_ID);
        }
    }

    @Test
    void map_unstagedSignal() {
        Mockito.doReturn(CJS).when(platformService).getPlatformName(CJS_PLATFORM_ID);
        val event = unstagedEvent().toBuilder()
                .revision(0)
                .id(123L)
                .build();

        val field = unstagedField(VersionedId.of(123L, 0)).toBuilder()
                .revision(0)
                .id(123L)
                .build();

        val signal = unstagedSignal(123L).toBuilder()
                .revision(0)
                .id(123L)
                .events(Set.of(event))
                .fields(Set.of(field))
                .build();

        val legacy = legacyMapper.map(signal, SignalDefinition.class);
        assertThat(legacy.getId()).isEqualTo(signal.getId().toString());
    }

    @Test
    void map_stagedSignal() {
        Mockito.doReturn(CJS).when(platformService).getPlatformName(CJS_PLATFORM_ID);
        val event = stagedEvent().toBuilder()
                .revision(0)
                .id(123L)
                .build();

        val field = stagedField(VersionedId.of(123L, 0)).toBuilder()
                .revision(0)
                .id(123L)
                .build();

        val stagedSignal = StagedSignal.builder()
                .planId(123L)
                .name(getRandomSmallString())
                .description(getRandomString())
                .domain("domain")
                .retentionPeriod(1L)
                .owners(getRandomSmallString())
                .type(getRandomSmallString())
                .platformId(CJS_PLATFORM_ID)
                .dataSource(UdcDataSourceType.TEST)
                .events(Set.of(event))
                .fields(Set.of(field))
                .refVersion(3)
                .build();

        stagedSignal.setSignalId(VersionedId.of(123L, 0));

        val legacy = legacyMapper.map(stagedSignal, SignalDefinition.class);
        assertThat(legacy.getId()).isEqualTo(stagedSignal.getId().toString());
        assertThat(legacy.getRefVersion()).isNotNull();
        assertThat(legacy.getLogicalDefinition().size()).isEqualTo(1);
        val mappedEvent = legacy.getLogicalDefinition().get(0).getEventClassifiers().get(0);
        assertThat(mappedEvent.getName()).isEqualTo(event.getName());
        assertThat(mappedEvent.getType()).isEqualTo(event.getType());
        assertThat(mappedEvent.getSource()).isEqualTo(event.getSource().name());
        assertThat(mappedEvent.getFilter()).isEqualTo(event.getExpression());
        assertThat(mappedEvent.getFsmOrder()).isEqualTo(event.getFsmOrder());

        val mappedFields = legacy.getLogicalDefinition().get(0).getFields();
        assertThat(mappedFields).hasSize(1);

        val mappedField = mappedFields.get(0);
        assertThat(mappedField.getName()).isEqualTo(field.getName());
        assertThat(mappedField.getFormula()).isEqualTo(field.getExpression());
        assertThat(mappedField.getClz()).isEqualToIgnoringCase(field.getExpressionType().name());
        assertThat(mappedField.isCached()).isEqualTo(field.getIsCached());
        assertThat(mappedField.getReadyStates().get(0)).isEqualTo(field.getEventTypes());
    }
}
