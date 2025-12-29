package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.signal.common.model.AbstractStagedSignal;
import com.ebay.behavior.gds.mdm.signal.common.model.PlatformLookup;
import com.ebay.behavior.gds.mdm.signal.model.view.StagedSignalProductionView;
import com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.datatype.SignalDefinition;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.Environment.PRODUCTION;
import static com.ebay.behavior.gds.mdm.common.model.Environment.STAGING;
import static com.ebay.behavior.gds.mdm.signal.util.ImportUtils.CJS;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.CJS_PLATFORM_ID;
import static com.ebay.behavior.gds.mdm.signal.util.ImportUtils.EJS;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.EJS_PLATFORM_ID;
import static com.ebay.behavior.gds.mdm.signal.util.ImportUtils.ITEM;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.ITEM_PLATFORM_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType.STAGED;

@ExtendWith(MockitoExtension.class)
class StagedSignalExportServiceTest {

    @InjectMocks
    private StagedSignalExportService service;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private UserGroupInformation userGroupInformation;

    @Mock
    private FileSystem hdfsFileSystem;

    @Mock
    private MetricsService metricsService;

    @Mock
    private StagedSignalService signalService;

    @Mock
    private Counter counter;

    @Mock
    private PlatformLookupService platformService;

    @BeforeEach
    void setup() {
        service.hdfsDumpPath = "/dummy/hdfs/path";
        Mockito.reset(counter);
    }

    @Test
    void export_prodSuccess() throws Exception {
        when(userGroupInformation.doAs(any(PrivilegedExceptionAction.class))).thenReturn(true);

        service.export("localhost", PRODUCTION);

        verify(userGroupInformation).reloginFromTicketCache();
        verify(userGroupInformation).doAs(any(PrivilegedExceptionAction.class));
    }

    @Test
    void export_stagingSuccess() throws Exception {
        when(userGroupInformation.doAs(any(PrivilegedExceptionAction.class))).thenReturn(true);

        service.export("localhost", STAGING);

        verify(userGroupInformation).reloginFromTicketCache();
        verify(userGroupInformation).doAs(any(PrivilegedExceptionAction.class));
    }

    @Test
    void export_prodUnexpectedException_failure() throws Exception {
        when(userGroupInformation.doAs(any(PrivilegedExceptionAction.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Verify that the exception is thrown
        assertThatThrownBy(() -> service.export("localhost", PRODUCTION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Export failed due to an unexpected error");

        // Verify interactions
        verify(userGroupInformation).reloginFromTicketCache();
        verify(userGroupInformation).doAs(any(PrivilegedExceptionAction.class));
    }

    @Test
    void export_stagingUnexpectedException_failure() throws Exception {
        when(userGroupInformation.doAs(any(PrivilegedExceptionAction.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        assertThatThrownBy(() -> service.export("localhost", STAGING))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Export failed due to an unexpected error");

        // Verify interactions
        verify(userGroupInformation).reloginFromTicketCache();
        verify(userGroupInformation).doAs(any(PrivilegedExceptionAction.class));
    }

    @Test
    void getSignalsMap_prodSuccess() {
        when(signalService.getAllProductionLatestVersions(eq(STAGED), anyLong()))
                .thenReturn(mock(Set.class));
        Set<PlatformLookup> platformLookups = new HashSet<>();
        platformLookups.add(getPlatformLookup(CJS, CJS_PLATFORM_ID));
        platformLookups.add(getPlatformLookup(EJS, EJS_PLATFORM_ID));
        platformLookups.add(getPlatformLookup(ITEM, ITEM_PLATFORM_ID));

        when(platformService.getAll()).thenReturn(platformLookups);

        var result = service.getSignalsMap(PRODUCTION);

        verify(signalService).getAllProductionLatestVersions(STAGED, CJS_PLATFORM_ID);
        verify(signalService).getAllProductionLatestVersions(STAGED, EJS_PLATFORM_ID);
        verify(signalService).getAllProductionLatestVersions(STAGED, ITEM_PLATFORM_ID);
        assertThat(result.size()).isGreaterThan(1);
    }

    @Test
    void getSignalsMap_stagingSuccess() {
        when(signalService.getAllStagingLatestVersions(eq(STAGED), anyLong()))
                .thenReturn(mock(Set.class));
        Set<PlatformLookup> platformLookups = new HashSet<>();
        platformLookups.add(getPlatformLookup(CJS, CJS_PLATFORM_ID));
        platformLookups.add(getPlatformLookup(EJS, EJS_PLATFORM_ID));
        platformLookups.add(getPlatformLookup(ITEM, ITEM_PLATFORM_ID));

        when(platformService.getAll()).thenReturn(platformLookups);


        var result = service.getSignalsMap(STAGING);
        verify(signalService).getAllStagingLatestVersions(STAGED, CJS_PLATFORM_ID);
        verify(signalService).getAllStagingLatestVersions(STAGED, EJS_PLATFORM_ID);
        verify(signalService).getAllStagingLatestVersions(STAGED, ITEM_PLATFORM_ID);
        assertThat(result.size()).isGreaterThan(1);
    }

    private PlatformLookup getPlatformLookup(String name, Long id) {
        PlatformLookup platformLookup = new PlatformLookup();
        platformLookup.setName(name);
        platformLookup.setId(id);
        return platformLookup;
    }

    @Test
    void dumpSignals_success() throws Exception {
        var signals = Set.of(mock(StagedSignalProductionView.class));
        var legacySignals = List.of(mock(SignalDefinition.class));
        var outputStream = mock(FSDataOutputStream.class);
        var signalMap = Map.<String, Set<? extends AbstractStagedSignal>>of(
                "CJS", signals
        );

        when(hdfsFileSystem.exists(any(Path.class))).thenReturn(false);
        when(hdfsFileSystem.create(any(Path.class), eq(true))).thenReturn(outputStream);
        when(objectMapper.writeValueAsString(legacySignals)).thenReturn("{\"mocked\":\"json\"}");
        when(signalService.toSignalDefinitions(signals)).thenReturn(legacySignals);
        when(metricsService.getExportSuccessCounter()).thenReturn(counter);

        var result = service.dumpSignals(signalMap, "20231010_1234");

        verify(hdfsFileSystem).exists(any(Path.class));
        verify(hdfsFileSystem).create(any(Path.class), eq(true));
        verify(outputStream).writeBytes("{\"mocked\":\"json\"}");
        verify(metricsService.getExportSuccessCounter()).increment();
        assertThat(result).isTrue();
    }

    @Test
    void dumpSignals_withDuplicateFile_failure() throws Exception {
        var signalMap = Map.<String, Set<? extends AbstractStagedSignal>>of(
                "CJS", Set.of(mock(StagedSignalProductionView.class)),
                "EJS", Set.of(mock(StagedSignalProductionView.class)),
                "ESP", Set.of(mock(StagedSignalProductionView.class))
        );

        when(hdfsFileSystem.exists(any(Path.class))).thenReturn(true);
        when(metricsService.getExportErrorCounter()).thenReturn(counter);

        var result = service.dumpSignals(signalMap, "20231010_1234");

        verify(hdfsFileSystem).exists(any(Path.class));
        verify(metricsService.getExportErrorCounter()).increment();
        assertThat(result).isFalse();
    }

    @Test
    void dumpSignals_exceptionDuringFileCreation_failure() throws Exception {
        var signalMap = Map.<String, Set<? extends AbstractStagedSignal>>of(
                "CJS", Set.of(mock(StagedSignalProductionView.class))
        );

        when(hdfsFileSystem.exists(any(Path.class))).thenReturn(false);
        when(hdfsFileSystem.create(any(Path.class), eq(true))).thenThrow(new RuntimeException("File creation error"));
        when(metricsService.getExportErrorCounter()).thenReturn(counter);

        var result = service.dumpSignals(signalMap, "20231010_1234");

        verify(hdfsFileSystem).exists(any(Path.class));
        verify(hdfsFileSystem).create(any(Path.class), eq(true));
        verify(metricsService.getExportErrorCounter()).increment();
        assertThat(result).isFalse();
    }

    @Test
    void dumpSignals_generalException_failure() throws Exception {
        var signalMap = Map.<String, Set<? extends AbstractStagedSignal>>of(
                "CJS", Set.of(mock(StagedSignalProductionView.class))
        );

        when(hdfsFileSystem.exists(any(Path.class))).thenThrow(new RuntimeException("General error"));
        when(metricsService.getExportErrorCounter()).thenReturn(counter);

        var result = service.dumpSignals(signalMap, "20231010_1234");

        verify(hdfsFileSystem).exists(any(Path.class));
        verify(metricsService.getExportErrorCounter()).increment();
        assertThat(result).isFalse();
    }

    @Test
    void deleteOldSignalFilesFromHDFS_allFiles_onlyOldDeleted() throws Exception {
        when(userGroupInformation.doAs(any(PrivilegedExceptionAction.class)))
                .thenAnswer(invocation -> ((PrivilegedExceptionAction<?>) invocation.getArgument(0)).run());

        var fmt = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
        var oldTs = LocalDateTime.now().minusWeeks(2).format(fmt);
        var recentTs = LocalDateTime.now().minusDays(1).format(fmt);

        var oldPath = new Path("/dummy/hdfs/path/anything_" + oldTs + ".json");
        var recentPath = new Path("/dummy/hdfs/path/anything_" + recentTs + ".json");

        var oldStatus = mock(FileStatus.class);
        var recentStatus = mock(FileStatus.class);
        when(oldStatus.getPath()).thenReturn(oldPath);
        when(recentStatus.getPath()).thenReturn(recentPath);

        when(hdfsFileSystem.exists(any(Path.class))).thenReturn(true);
        when(hdfsFileSystem.listStatus(any(Path.class))).thenReturn(new FileStatus[]{oldStatus, recentStatus});
        when(hdfsFileSystem.delete(oldPath, true)).thenReturn(true);

        int deleted = service.deleteOldSignalFiles();

        verify(hdfsFileSystem).delete(oldPath, true);
        verify(hdfsFileSystem, never()).delete(eq(recentPath), anyBoolean());
        assertThat(deleted).isEqualTo(1);
    }

    @Test
    void deleteOldSignalFilesFromHDFS_directoryDoesNotExist() throws Exception {
        when(userGroupInformation.doAs(any(PrivilegedExceptionAction.class)))
                .thenAnswer(invocation -> ((PrivilegedExceptionAction<?>) invocation.getArgument(0)).run());
        when(hdfsFileSystem.exists(any(Path.class))).thenReturn(false);

        assertThatThrownBy(() -> service.deleteOldSignalFiles())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("HDFS cleanup skipped: directory does not exist: /dummy/hdfs/path");
        verify(hdfsFileSystem, never()).listStatus(any(Path.class));
    }

    @Test
    void deleteOldSignalFilesFromHDFS_emptyDirectory() throws Exception {
        when(userGroupInformation.doAs(any(PrivilegedExceptionAction.class)))
                .thenAnswer(invocation -> ((PrivilegedExceptionAction<?>) invocation.getArgument(0)).run());

        when(hdfsFileSystem.exists(any(Path.class))).thenReturn(true);
        when(hdfsFileSystem.listStatus(any(Path.class))).thenReturn(new FileStatus[0]);

        var deleted = service.deleteOldSignalFiles();

        assertThat(deleted).isEqualTo(0);
        verify(hdfsFileSystem).listStatus(any(Path.class));
        verify(hdfsFileSystem, never()).delete(any(Path.class), anyBoolean());
    }

    @Test
    void deleteOldSignalFilesFromHDFS_invalidTimestampInFileName() throws Exception {
        when(userGroupInformation.doAs(any(PrivilegedExceptionAction.class)))
                .thenAnswer(invocation -> ((PrivilegedExceptionAction<?>) invocation.getArgument(0)).run());

        var invalidPath = new Path("/dummy/hdfs/path/anything_invalid.json");
        var invalidStatus = mock(FileStatus.class);
        when(invalidStatus.getPath()).thenReturn(invalidPath);

        when(hdfsFileSystem.exists(any(Path.class))).thenReturn(true);
        when(hdfsFileSystem.listStatus(any(Path.class))).thenReturn(new FileStatus[]{invalidStatus});

        var deleted = service.deleteOldSignalFiles();

        verify(hdfsFileSystem, never()).delete(any(Path.class), anyBoolean());
        assertThat(deleted).isEqualTo(0);
    }

    @Test
    void deleteOldSignalFilesFromHDFS_recentFilesNotDeleted() throws Exception {
        when(userGroupInformation.doAs(any(PrivilegedExceptionAction.class)))
                .thenAnswer(invocation -> ((PrivilegedExceptionAction<?>) invocation.getArgument(0)).run());

        var df = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
        var recentTs = LocalDateTime.now().minusDays(2).format(df);
        var recentPath = new Path("/dummy/hdfs/path/anything_" + recentTs + ".json");

        var recentStatus = mock(FileStatus.class);
        when(recentStatus.getPath()).thenReturn(recentPath);

        when(hdfsFileSystem.exists(any(Path.class))).thenReturn(true);
        when(hdfsFileSystem.listStatus(any(Path.class))).thenReturn(new FileStatus[]{recentStatus});

        var deleted = service.deleteOldSignalFiles();

        verify(hdfsFileSystem, never()).delete(any(Path.class), anyBoolean());
        assertThat(deleted).isEqualTo(0);
    }

    @Test
    void deleteOldSignalFilesFromHDFS_nullFileStatusArray() throws Exception {
        when(userGroupInformation.doAs(any(PrivilegedExceptionAction.class)))
                .thenAnswer(invocation -> ((PrivilegedExceptionAction<?>) invocation.getArgument(0)).run());

        when(hdfsFileSystem.exists(any(Path.class))).thenReturn(true);
        when(hdfsFileSystem.listStatus(any(Path.class))).thenReturn(null);

        var deleted = service.deleteOldSignalFiles();

        assertThat(deleted).isEqualTo(0);
        verify(hdfsFileSystem).listStatus(any(Path.class));
        verify(hdfsFileSystem, never()).delete(any(Path.class), anyBoolean());
    }

    @Test
    void deleteOldSignalFilesFromHDFS_filesWithoutMatchingPattern() throws Exception {
        when(userGroupInformation.doAs(any(PrivilegedExceptionAction.class)))
                .thenAnswer(invocation -> ((PrivilegedExceptionAction<?>) invocation.getArgument(0)).run());

        var unmatchedPath = new Path("/dummy/hdfs/path/anything_no_match.json");
        var unmatchedStatus = mock(FileStatus.class);
        when(unmatchedStatus.getPath()).thenReturn(unmatchedPath);

        when(hdfsFileSystem.exists(any(Path.class))).thenReturn(true);
        when(hdfsFileSystem.listStatus(any(Path.class))).thenReturn(new FileStatus[]{unmatchedStatus});

        var deleted = service.deleteOldSignalFiles();

        verify(hdfsFileSystem, never()).delete(any(Path.class), anyBoolean());
        assertThat(deleted).isEqualTo(0);
    }

    @Test
    void deleteOldSignalFiles_ioExceptionCaught() throws Exception {
        when(userGroupInformation.doAs(any(PrivilegedExceptionAction.class)))
                .thenThrow(new IOException("Simulated IO error"));

        var deleted = service.deleteOldSignalFiles();

        assertThat(deleted).isEqualTo(0);
        verify(userGroupInformation).doAs(any(PrivilegedExceptionAction.class));
    }

    @Test
    void deleteOldSignalFiles_interruptedExceptionCaught() throws Exception {
        when(userGroupInformation.doAs(any(PrivilegedExceptionAction.class)))
                .thenThrow(new InterruptedException("Simulated interruption"));

        var deleted = service.deleteOldSignalFiles();

        assertThat(deleted).isEqualTo(0);
        verify(userGroupInformation).doAs(any(PrivilegedExceptionAction.class));
    }
}
