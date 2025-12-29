package com.ebay.behavior.gds.mdm.common.util;

import com.ebay.behavior.gds.mdm.common.testUtil.TestModel;

import lombok.val;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.ebay.behavior.gds.mdm.common.util.TimeUtils.DATE_FORMATTER;
import static com.ebay.behavior.gds.mdm.common.util.TimeUtils.DATE_TIME_FORMATTER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeUtilsTest {

    LocalDateTime date = LocalDateTime.of(2022, 1, 1, 12, 0);

    @Test
    void toString_() {
        var result = TimeUtils.toString(date);

        assertThat(result).isEqualTo("2022-01-01T12:00:00");
    }

    @Test
    void toDateString() {
        var result = TimeUtils.toDateString(date);

        assertThat(result).isEqualTo("1/1/22 12:00 PM");
    }

    @Test
    void toLocalDateTime() {
        var dateTime = TimeUtils.toDateString(date);

        var result = TimeUtils.toLocalDateTime(dateTime, DATE_FORMATTER);

        assertThat(result).isEqualTo(date);
    }

    @Test
    void toString_fromTimestamp() {
        var tsBefore = TimeUtils.toNowSqlTimestamp();

        val result = TimeUtils.toString(tsBefore, DATE_TIME_FORMATTER);

        var tsAfter = TimeUtils.toSqlTimestamp(result, DATE_TIME_FORMATTER);
        assertThat(tsBefore).isCloseTo(tsAfter, 1000);
    }

    @Test
    void toSqlTimestamp_localDateTime() {
        var result = TimeUtils.toSqlTimestamp(date);

        assertThat(result).isEqualTo(Timestamp.valueOf(date));
    }

    @Test
    void toSqlTimestamp_epochMilli() {
        var epochMilli = System.currentTimeMillis();

        var result = TimeUtils.toSqlTimestamp(epochMilli);

        assertThat(result).isEqualTo(new Timestamp(epochMilli));
    }

    @Test
    void toNowSqlTimestamp() {
        var result = TimeUtils.toNowSqlTimestamp();

        var currentTimestamp = new Timestamp(System.currentTimeMillis());
        assertThat(result).isCloseTo(currentTimestamp, 1000);
    }

    @Test
    void getDuration_hours() {
        var result = TimeUtils.getDuration("1h", TimeUnit.HOURS);

        assertThat(result).isEqualTo(1);
    }

    @Test
    void getDuration_minutes() {
        var result = TimeUtils.getDuration("2m", TimeUnit.MINUTES);

        assertThat(result).isEqualTo(2);
    }

    @Test
    void getDuration_seconds() {
        var result = TimeUtils.getDuration("3s", TimeUnit.SECONDS);

        assertThat(result).isEqualTo(3);
    }

    @Test
    void getDuration_unsupportedTimeUnit_error() {
        assertThatThrownBy(() -> TimeUtils.getDuration("1h", TimeUnit.MICROSECONDS))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getDuration_wrongTimeUnit_error() {
        assertThatThrownBy(() -> TimeUtils.getDuration("PT1H", TimeUnit.DAYS))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void sleepSeconds() {
        var start = System.currentTimeMillis();
        TimeUtils.sleepSeconds(1);
        var end = System.currentTimeMillis();

        assertThat(end - start).isGreaterThanOrEqualTo(1000);
    }

    @Test
    void isLessOrEqualTo_updateDateBeforeUpperBound() {
        var upperBound = LocalDateTime.now();
        var auditable = TestModel.builder().updateDate(TimeUtils.toSqlTimestamp(upperBound.minusDays(1))).build();

        var result = TimeUtils.isLessOrEqualTo(TimeUtils.toSqlTimestamp(upperBound), auditable);

        assertThat(result).isTrue();
    }

    @Test
    void isGreaterOrEqualTo_updateDateAfterLowerBound() {
        var lowerBound = LocalDateTime.now();
        var auditable = TestModel.builder().updateDate(TimeUtils.toSqlTimestamp(lowerBound.plusDays(1))).build();

        var result = TimeUtils.isGreaterOrEqualTo(TimeUtils.toSqlTimestamp(lowerBound), auditable);

        assertThat(result).isTrue();
    }
}