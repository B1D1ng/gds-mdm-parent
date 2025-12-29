package com.ebay.behavior.gds.mdm.signal.service.migration;

import com.ebay.behavior.gds.mdm.signal.config.SignalMigrationJobConfiguration;
import com.ebay.behavior.gds.mdm.signal.repository.migration.SignalMigrationJobRepository;
import com.ebay.behavior.gds.mdm.signal.service.MetricsService;

import io.micrometer.core.instrument.Counter;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static com.ebay.behavior.gds.mdm.common.model.Environment.PRODUCTION;
import static com.ebay.behavior.gds.mdm.signal.common.model.migration.SignalMigrationJobType.CUSTOM;
import static com.ebay.behavior.gds.mdm.signal.common.model.migration.SignalMigrationJobType.HOURLY;
import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SignalMigrationJobServiceTest {

    @Mock
    private Counter counter;

    @Mock
    private MetricsService metricsService;

    @Mock
    private SignalMigrationJobRepository jobRepository;

    @Spy
    private SignalMigrationJobConfiguration configuration;

    @Spy
    @InjectMocks
    private SignalMigrationJobService service;

    @BeforeEach
    void setUp() {
        val hourly = new SignalMigrationJobConfiguration.SchedulerConfig();
        val custom = new SignalMigrationJobConfiguration.SchedulerConfig();

        configuration.setHourly(hourly);
        configuration.setCustom(custom);

        reset(counter);
        reset(metricsService);
        reset(jobRepository);
    }

    @Test
    void syncHourly_disabled_skipped() {
        configuration.getHourly().setEnabled(false);

        service.syncHourly();

        verify(service, never()).syncAll(any(), any(), any(), any(), eq(PRODUCTION));
    }

    @Test
    void syncCustom_disabled_skipped() {
        configuration.getCustom().setEnabled(false);

        service.syncCustom();

        verify(service, never()).syncAll(any(), any(), any(), any(), eq(PRODUCTION));
    }

    @Test
    void syncAll_jobInProgress_skipped() {
        configuration.getCustom().setEnabled(true);
        doThrow(new DataIntegrityViolationException("")).when(jobRepository).saveAndFlush(any());

        service.syncAll(now().minusDays(1), now(), counter, CUSTOM, PRODUCTION);

        verify(counter, never()).increment();
    }

    @Test
    void getKey() {
        var now = now();

        var key = HOURLY.getKey(now);
        assertThat(key).isGreaterThan(0);

        key = CUSTOM.getKey(now);
        assertThat(key).isGreaterThan(0);
    }
}
