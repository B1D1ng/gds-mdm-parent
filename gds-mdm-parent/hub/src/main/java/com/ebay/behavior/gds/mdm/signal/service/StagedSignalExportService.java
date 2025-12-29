package com.ebay.behavior.gds.mdm.signal.service;

import com.ebay.behavior.gds.mdm.common.exception.PartialSuccessException;
import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.util.TimeUtils;
import com.ebay.behavior.gds.mdm.signal.common.model.AbstractStagedSignal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static com.ebay.behavior.gds.mdm.common.model.UdcDataSourceType.STAGED;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static com.ebay.behavior.gds.mdm.common.model.Environment.PRODUCTION;
import static com.ebay.behavior.gds.mdm.common.model.Environment.STAGING;

@Slf4j
@Service
@Validated
@Profile({"!IT", "!Dev"})
public class StagedSignalExportService {

    @Autowired
    public ObjectMapper objectMapper;

    @Lazy
    @Autowired
    public UserGroupInformation userGroupInformation;

    @Lazy
    @Autowired
    public FileSystem fileSystem;

    @Autowired
    private StagedSignalService signalService;

    @Autowired
    private MetricsService metricsService;

    @Autowired
    public PlatformLookupService platformLookupService;

    @Value("${hadoop.hdfs.path:default/path}") // Default path is a dummy path for dev env
    public String hdfsDumpPath;

    private final String signalType = "staged_signal";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
    private static final Pattern TS_PATTERN = Pattern.compile("(\\d{8}_\\d{4})(?=\\.json$)", Pattern.CASE_INSENSITIVE);

    public void export(@NotBlank String hostname, Environment env) {
        String timestamp = TimeUtils.toString(new Timestamp(System.currentTimeMillis()), TIMESTAMP_FORMATTER);
        val signalMap = getSignalsMap(env);
        try {
            userGroupInformation.reloginFromTicketCache();
            val success = userGroupInformation.doAs((PrivilegedExceptionAction<Boolean>) () -> dumpSignals(signalMap, timestamp));
            if (!success) {
                throw new IllegalStateException("Error while dumping signals to HDFS for host: " + hostname);
            }
            log.info("Successfully dumped signals to HDFS.");
        } catch (PartialSuccessException e) {
            log.warn("Partial result while dumping signals to HDFS ", e);
            throw new PartialSuccessException("Partial result while dumping signals to HDFS: " + e.getMessage(), e, List.of());
        } catch (Exception e) {
            throw new IllegalStateException("Export failed due to an unexpected error for host: " + hostname, e);
        }
    }

    @VisibleForTesting
    protected Map<String, Set<? extends AbstractStagedSignal>> getSignalsMap(Environment env) {
        Map<String, Set<? extends AbstractStagedSignal>> signalsMap = new HashMap<>();
        val platformLookups = platformLookupService.getAll();
        if (env == PRODUCTION) {
            platformLookups.forEach((platformLookup) -> {
                signalsMap.put(platformLookup.getName(), signalService.getAllProductionLatestVersions(STAGED, platformLookup.getId()));
            });
        } else if (env == STAGING) {
            platformLookups.forEach((platformLookup) -> {
                signalsMap.put(platformLookup.getName(), signalService.getAllStagingLatestVersions(STAGED, platformLookup.getId()));
            });
        } else {
            throw new IllegalArgumentException("Unsupported environment: " + env);
        }
        return signalsMap;
    }

    @VisibleForTesting
    protected boolean dumpSignals(Map<String, Set<? extends AbstractStagedSignal>> signalMap, String timestamp) {
        try {
            for (val entry : signalMap.entrySet()) {
                val platform = entry.getKey().toLowerCase(Locale.US);
                val filePath = new Path(hdfsDumpPath + "/" + String.join("_", signalType, platform, timestamp + ".json"));

                if (fileSystem.exists(filePath)) {
                    log.error("Detected duplicate signals for {} and timestamp {}", platform, timestamp);
                    metricsService.getExportErrorCounter().increment();
                    return false;
                }

                val signals = entry.getValue();
                val legacySignals = signalService.toSignalDefinitions(signals); // temporarily convert to legacy format

                try (val fsDataOutputStream = fileSystem.create(filePath, true)) {
                    fsDataOutputStream.writeBytes(isEmpty(legacySignals) ? "" : objectMapper.writeValueAsString(legacySignals));
                } catch (Exception ex) {
                    log.error("Could write to the give HDFS path : {}", filePath, ex);
                    metricsService.getExportErrorCounter().increment();
                    return false;
                }
                metricsService.getExportSuccessCounter().increment();
            }
            log.info("Finish dumping file to HDFS");
            return true;
        } catch (Exception ex) {
            log.error("Error while dumping file to HDFS", ex);
            metricsService.getExportErrorCounter().increment();
            return false;
        }
    }

    public int deleteOldSignalFiles() {
        try {
            return userGroupInformation.doAs((PrivilegedExceptionAction<Integer>) this::doCleanup);
        } catch (IOException | InterruptedException ex) {
            val errMsg = "Error during HDFS cleanup for directory: " + hdfsDumpPath;
            log.error(errMsg, ex);
            return 0;
        }
    }

    private int doCleanup() throws IOException {
        val dir = new Path(hdfsDumpPath);

        if (!fileSystem.exists(dir)) {
            throw new IllegalStateException("HDFS cleanup skipped: directory does not exist: " + hdfsDumpPath);
        }

        val statuses = fileSystem.listStatus(dir);
        if (statuses == null || statuses.length == 0) {
            log.info("HDFS cleanup: directory {} is already empty.", hdfsDumpPath);
            return 0;
        }

        val cutoff = LocalDateTime.now(ZoneId.systemDefault()).minusWeeks(1);
        int deletedCount = 0;

        for (val status : statuses) {
            if (!shouldDelete(status, cutoff)) {
                continue;
            }
            if (deletePath(status.getPath())) {
                deletedCount++;
            }
        }

        log.info("HDFS cleanup completed for {}. Files deleted (older than 1 week): {}", hdfsDumpPath, deletedCount);
        return deletedCount;
    }

    private boolean shouldDelete(FileStatus status, LocalDateTime cutoff) {
        val fileName = status.getPath().getName();
        val matcher = TS_PATTERN.matcher(fileName);
        if (!matcher.find()) {
            log.info("HDFS cleanup: skipping {} (no timestamp in filename).", fileName);
            return false;
        }

        val tsText = matcher.group(1);
        try {
            val fileTs = LocalDateTime.parse(tsText, TS_FORMATTER);
            return fileTs.isBefore(cutoff);
        } catch (DateTimeParseException ex) {
            log.warn("HDFS cleanup: bad timestamp '{}' in {}. Skipping.", tsText, fileName);
            return false;
        }
    }

    private boolean deletePath(Path target) {
        try {
            val deleted = fileSystem.delete(target, true);
            if (!deleted) {
                metricsService.getDeleteSignalErrorCounter().increment();
                log.error("HDFS cleanup: failed to delete {}", target);
            }
            return deleted;
        } catch (IOException ioe) {
            metricsService.getDeleteSignalErrorCounter().increment();
            log.error("HDFS cleanup: IO error deleting {}", target, ioe);
            return false;
        }
    }
}
