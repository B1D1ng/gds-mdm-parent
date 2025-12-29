package com.ebay.behavior.gds.mdm.signal.repository;

import com.ebay.behavior.gds.mdm.signal.common.model.StagedSignal;
import com.ebay.behavior.gds.mdm.signal.service.PlanService;
import com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static com.ebay.behavior.gds.mdm.common.model.Environment.PRODUCTION;
import static com.ebay.behavior.gds.mdm.common.model.Environment.STAGING;
import static com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType.QA;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.CJS_PLATFORM_ID;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.EJS_PLATFORM_ID;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.stagedSignal;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class StagedSignalProductionViewRepositoryIT {

    @Autowired
    private StagedSignalRepository signalRepository;

    @Autowired
    private StagedSignalProductionViewRepository viewRepository;

    @Autowired
    private PlanService planService;

    private final long id = getRandomLong();
    private StagedSignal signal2;
    private StagedSignal signal4;

    @BeforeAll
    void setUpClass() {
        var plan = TestModelUtils.plan();
        var planId = planService.create(plan).getId();

        var signal1 = stagedSignal(planId).toBuilder()
                .id(id).name("name1").dataSource(QA).platformId(CJS_PLATFORM_ID).environment(PRODUCTION).version(1).build(); // previous version
        signal2 = stagedSignal(planId).toBuilder()
                .id(id).name("name2").dataSource(QA).platformId(CJS_PLATFORM_ID).environment(PRODUCTION).version(2).build(); // latest version
        var signal3 = stagedSignal(planId).toBuilder()
                .id(id).name("name3").dataSource(QA).platformId(CJS_PLATFORM_ID).environment(STAGING).version(3).build(); // staging filtered out
        signal4 = stagedSignal(planId).toBuilder()
                .id(getRandomLong()).name("name4").dataSource(QA).platformId(EJS_PLATFORM_ID).environment(PRODUCTION).version(1).build(); // different id
        signalRepository.saveAll(List.of(signal1, signal2, signal3, signal4));
    }

    @Test
    void findById() {
        var signal = viewRepository.findById(id).get();

        assertThat(signal.getId()).isEqualTo(id);
    }

    @Test
    void findById_notFound() {
        var maybeSignal = viewRepository.findById(getRandomLong());

        assertThat(maybeSignal).isEmpty();
    }

    @Test
    void findAllByDataSourceAndPlatform() {
        var signals = viewRepository.findAllByDataSourceAndPlatformId(QA, CJS_PLATFORM_ID);

        assertThat(signals).hasSize(1);
        assertThat(signals.get(0).getId()).isEqualTo(signal2.getId());
        assertThat(signals.get(0).getVersion()).isEqualTo(signal2.getVersion());
        assertThat(signals.get(0).getEnvironment()).isEqualTo(PRODUCTION);

        signals = viewRepository.findAllByDataSourceAndPlatformId(QA, EJS_PLATFORM_ID);

        assertThat(signals).hasSize(1);
        assertThat(signals.get(0).getId()).isEqualTo(signal4.getId());
        assertThat(signals.get(0).getVersion()).isEqualTo(signal4.getVersion());
        assertThat(signals.get(0).getEnvironment()).isEqualTo(PRODUCTION);
    }

    @Test
    void findAll() {
        var signals = viewRepository.findAll();

        assertThat(signals.size()).isGreaterThanOrEqualTo(2);
        assertThat(signals).extracting("id").contains(signal2.getId(), signal4.getId());
        assertThat(signals).extracting("version").contains(signal2.getVersion(), signal4.getVersion());
    }
}
