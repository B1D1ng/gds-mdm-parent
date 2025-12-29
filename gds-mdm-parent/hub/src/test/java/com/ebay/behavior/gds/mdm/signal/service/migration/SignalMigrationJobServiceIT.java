package com.ebay.behavior.gds.mdm.signal.service.migration;

import com.ebay.behavior.gds.mdm.signal.repository.migration.SignalMigrationJobRepository;
import com.ebay.behavior.gds.mdm.signal.service.MetricsService;

import io.micrometer.core.instrument.Counter;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.common.util.TimeUtils.nowPst;
import static com.ebay.behavior.gds.mdm.signal.common.model.migration.SignalMigrationJobType.CUSTOM;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SignalMigrationJobServiceIT {

    @Mock
    private Counter counter;

    @Autowired
    private SignalMigrationJobRepository jobRepository;

    @MockitoSpyBean
    private SignalMigrationJobService service;

    @MockitoBean
    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        reset(counter);
        doReturn(counter).when(metricsService).getSignalMigrationCustomCounter();
        lenient().doAnswer(a -> a).when(counter).increment();

        val jobKey = CUSTOM.getKey(nowPst());
        val maybeJob = jobRepository.findByJobId(jobKey);
        maybeJob.ifPresent(job -> jobRepository.delete(job));
    }

    @Test
    void syncOnDemand() throws ExecutionException, InterruptedException {
        val from = nowPst().minusDays(1);
        val to = nowPst();

        var done = service.syncOnDemand(from, to);

        assertThat(done.get()).isTrue();
        verify(counter, times(1)).increment();

        done = service.syncOnDemand(from, to);
        assertThat(done.get()).isFalse();
    }

    @Test
    void syncOnDemand_locked_skipped() throws ExecutionException, InterruptedException {
        val from = nowPst().minusDays(1);
        val to = nowPst();

        val firstDone = service.syncOnDemand(from, to);
        sleep(1);

        val secondDone = service.syncOnDemand(from, to);

        assertThat(secondDone.get()).isFalse();
        assertThat(firstDone.get()).isTrue();
        verify(counter, times(1)).increment();
    }

    @Test
    void syncOnDemand_alreadyInProgress_skipped() throws ExecutionException, InterruptedException {
        doReturn(Optional.empty()).when(service).acquireJob(any(), any());
        val from = nowPst().minusDays(1);
        val to = nowPst();

        val done = service.syncOnDemand(from, to);

        assertThat(done.get()).isFalse();
        verify(counter, never()).increment();
    }
}
