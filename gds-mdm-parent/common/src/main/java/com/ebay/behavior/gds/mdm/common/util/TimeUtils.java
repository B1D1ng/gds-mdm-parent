package com.ebay.behavior.gds.mdm.common.util;

import com.ebay.behavior.gds.mdm.common.model.Auditable;

import lombok.experimental.UtilityClass;
import lombok.val;
import org.apache.commons.lang3.Validate;
import org.springframework.boot.convert.DurationStyle;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@UtilityClass
public class TimeUtils {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(Locale.US);
    public static final DateTimeFormatter LEGACY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final ZoneId PST = ZoneId.of(ZoneId.SHORT_IDS.get("PST"));

    public static String toString(LocalDateTime date) {
        return DATE_TIME_FORMATTER.format(date);
    }

    public static String toString(Timestamp timestamp, DateTimeFormatter formatter) {
        return formatter.format(timestamp.toLocalDateTime());
    }

    public static String toDateString(LocalDateTime date) {
        return DATE_FORMATTER.format(date);
    }

    public static Timestamp toNowSqlTimestamp() {
        val now = LocalDateTime.now().withNano(0);
        return Timestamp.valueOf(now);
    }

    public static Timestamp toSqlTimestamp(LocalDateTime ldt) {
        return Timestamp.valueOf(ldt);
    }

    public static Timestamp toSqlTimestamp(long epochMilli) {
        return new Timestamp(epochMilli);
    }

    public static Timestamp toSqlTimestamp(String dateTime, DateTimeFormatter formatter) {
        val ldm = LocalDateTime.parse(dateTime, formatter);
        return Timestamp.valueOf(ldm);
    }

    public static LocalDateTime toLocalDateTime(String dateTime, DateTimeFormatter formatter) {
        return LocalDateTime.parse(dateTime, formatter);
    }

    public static boolean isLessOrEqualTo(Timestamp upperBound, Auditable auditable) {
        return isLessGreaterOrEqualTo(upperBound, auditable, ts -> ts.before(upperBound));
    }

    public static boolean isGreaterOrEqualTo(Timestamp lowerBound, Auditable auditable) {
        return isLessGreaterOrEqualTo(lowerBound, auditable, ts -> ts.after(lowerBound));
    }

    public static long getDuration(String strDuration, TimeUnit unit) {
        val duration = DurationStyle.detectAndParse(strDuration);

        long range = switch (unit) {
            case SECONDS -> duration.getSeconds();
            case MINUTES -> duration.toMinutes();
            case HOURS -> duration.toHours();
            case DAYS -> duration.toDays();
            default -> throw new UnsupportedOperationException(String.format("Unsupported TimeUnit: %s", unit.name()));
        };

        if (range == 0) {
            throw new IllegalStateException(String.format("Wrong TimeUnit passed: %s - too large", unit.name()));
        }
        return range;
    }

    public static void sleepSeconds(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException ignored) {
        }
    }

    private static boolean isLessGreaterOrEqualTo(Timestamp bound, Auditable auditable, Predicate<Timestamp> predicate) {
        Validate.isTrue(Objects.nonNull(bound), "bound is null");
        Validate.isTrue(Objects.nonNull(auditable), "auditable is null");

        var updateDate = auditable.getUpdateDate();

        if (Objects.isNull(updateDate)) {
            updateDate = auditable.getCreateDate();
        }

        Validate.isTrue(Objects.nonNull(updateDate), "no create/update date found");
        return predicate.test(updateDate) || updateDate.equals(bound);
    }

    public static LocalDateTime nowPst() {
        return LocalDateTime.now(PST);
    }
}
