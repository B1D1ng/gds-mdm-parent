package com.ebay.behavior.gds.mdm.signal.resource;

import com.ebay.behavior.gds.mdm.common.model.search.RelationalSearchRequest;
import com.ebay.behavior.gds.mdm.signal.service.MetricsService;
import com.ebay.behavior.gds.mdm.signal.service.PlatformLookupService;
import com.ebay.behavior.gds.mdm.signal.service.StagedSignalService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import io.micrometer.core.instrument.Counter;

import static com.ebay.behavior.gds.mdm.common.model.Environment.PRODUCTION;
import static com.ebay.behavior.gds.mdm.common.model.Environment.STAGING;
import static com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType.STAGED;
import static com.ebay.behavior.gds.mdm.signal.util.ImportUtils.CJS;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.CJS_PLATFORM_ID;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StagedSignalResourceTest {

    @Mock
    private StagedSignalService service;

    @Spy
    @InjectMocks
    private StagedSignalResource resource;

    @Mock
    private PlatformLookupService platformService;

    @Mock
    private MetricsService metricsService;

    @Mock
    private Counter successCounter;

    @Mock
    private Counter errorCounter;

    @BeforeEach
    void setUp() {
        Mockito.reset(service);
        lenient().when(metricsService.getSignalSuccessCounter()).thenReturn(successCounter);
        lenient().when(metricsService.getSignalErrorCounter()).thenReturn(errorCounter);
    }

    @Test
    void getAll_useCacheAndLatestVersionAndProduction() {
        doReturn(PRODUCTION).when(resource).getStagedEnvironment();
        when(platformService.getPlatformId(CJS)).thenReturn(1L);

        resource.getAll(CJS, true, false, true);

        verify(service, times(1)).getAllProductionLatestVersionsCached(STAGED, CJS_PLATFORM_ID);
    }

    @Test
    void getAll_useCacheAndLatestVersionAndStaging() {
        doReturn(STAGING).when(resource).getStagedEnvironment();
        when(platformService.getPlatformId(CJS)).thenReturn(1L);

        resource.getAll(CJS, true, false, true);

        verify(service, times(1)).getAllStagingLatestVersionsCached(STAGED, CJS_PLATFORM_ID);
        verify(successCounter, times(1)).increment();
        verify(errorCounter, never()).increment();
    }

    @Test
    void getAll_useCacheAndNoLatestVersion() {
        doReturn(STAGING).when(resource).getStagedEnvironment();
        when(platformService.getPlatformId(CJS)).thenReturn(1L);

        resource.getAll(CJS, true, false, false);

        verify(service, times(1)).getAllVersionsCached(STAGING, STAGED, CJS_PLATFORM_ID);
        verify(successCounter, times(1)).increment();
        verify(errorCounter, never()).increment();
    }

    @Test
    void getAll_noCacheAndLatestVersionAndProduction() {
        doReturn(PRODUCTION).when(resource).getStagedEnvironment();
        when(platformService.getPlatformId(CJS)).thenReturn(1L);

        resource.getAll(CJS, false, false, true);

        verify(service, times(1)).getAllProductionLatestVersions(STAGED, CJS_PLATFORM_ID);
        verify(successCounter, times(1)).increment();
        verify(errorCounter, never()).increment();
    }

    @Test
    void getAll_noCacheAndLatestVersionAndStaging() {
        doReturn(STAGING).when(resource).getStagedEnvironment();
        when(platformService.getPlatformId(CJS)).thenReturn(1L);

        resource.getAll(CJS, false, false, true);

        verify(service, times(1)).getAllStagingLatestVersions(STAGED, CJS_PLATFORM_ID);
        verify(successCounter, times(1)).increment();
        verify(errorCounter, never()).increment();
    }

    @Test
    void getAll_noCacheAndNoLatestVersion() {
        doReturn(STAGING).when(resource).getStagedEnvironment();
        when(platformService.getPlatformId(CJS)).thenReturn(1L);

        resource.getAll(CJS, false, false, false);

        verify(service, times(1)).getAllVersions(STAGING, STAGED, CJS_PLATFORM_ID);
        verify(successCounter, times(1)).increment();
        verify(errorCounter, never()).increment();
    }

    @Test
    void search_withUnstagedDetailsAndWithLegacyFormatAndWithLatestVersionsAndProduction() {
        var request = new RelationalSearchRequest();
        doReturn(PRODUCTION).when(resource).getStagedEnvironment();

        resource.search(true, true, true, request);

        verify(service, times(1)).searchProductionLatestVersions(false, request);
    }

    @Test
    void search_withUnstagedDetailsAndWithLegacyFormatAndWithLatestVersionsAndStaging() {
        var request = new RelationalSearchRequest();
        doReturn(STAGING).when(resource).getStagedEnvironment();

        resource.search(true, true, true, request);

        verify(service, times(1)).searchStagingLatestVersions(false, request);
    }

    @Test
    void search_withUnstagedDetailsAndWithLegacyFormatAndWithoutLatestVersions() {
        var request = new RelationalSearchRequest();
        doReturn(STAGING).when(resource).getStagedEnvironment();

        resource.search(true, true, false, request);

        verify(service, times(1)).searchAllVersions(false, request);
    }

    @Test
    void search_withUnstagedDetailsAndWithoutLegacyFormatAndWithLatestVersionsAndProduction() {
        var request = new RelationalSearchRequest();
        doReturn(PRODUCTION).when(resource).getStagedEnvironment();

        resource.search(true, false, true, request);

        verify(service, times(1)).searchProductionLatestVersions(true, request);
    }

    @Test
    void search_withUnstagedDetailsAndWithoutLegacyFormatAndWithLatestVersionsAndStaging() {
        var request = new RelationalSearchRequest();
        doReturn(STAGING).when(resource).getStagedEnvironment();

        resource.search(true, false, true, request);

        verify(service, times(1)).searchStagingLatestVersions(true, request);
    }

    @Test
    void search_withUnstagedDetailsAndWithoutLegacyFormatAndWithOutLatestVersions() {
        var request = new RelationalSearchRequest();
        doReturn(STAGING).when(resource).getStagedEnvironment();

        resource.search(true, false, false, request);

        verify(service, times(1)).searchAllVersions(true, request);
    }

    @Test
    void getAll_serviceThrowsException_incrementsErrorCounter() {
        doReturn(PRODUCTION).when(resource).getStagedEnvironment();
        when(platformService.getPlatformId(CJS)).thenReturn(1L);
        when(service.getAllProductionLatestVersionsCached(STAGED, CJS_PLATFORM_ID))
                .thenThrow(new RuntimeException("Simulated failure"));

        Assertions.assertThrows(RuntimeException.class,
                () -> resource.getAll(CJS, true, false, true));

        verify(errorCounter, times(1)).increment();
        verify(successCounter, never()).increment();
    }
}
