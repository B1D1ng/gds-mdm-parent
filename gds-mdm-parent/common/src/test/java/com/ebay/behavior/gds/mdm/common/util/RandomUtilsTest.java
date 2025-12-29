package com.ebay.behavior.gds.mdm.common.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

class RandomUtilsTest {

    @Test
    void getRandomLong_returnsNumberWithinBounds() {
        long num = RandomUtils.getRandomLong(1000);
        assertThat(num).isBetween(0L, 1000L);
    }

    @Test
    void getRandomLong_returnsNumberWithinBounds_whenBoundIsOne() {
        long num = RandomUtils.getRandomLong(1);
        assertThat(num).isEqualTo(0);
    }

    @Test
    void sleepRandomMills_doesNotExceedBound() {
        long start = currentTimeMillis();
        RandomUtils.sleepRandomMills(1000);
        long end = currentTimeMillis();
        assertThat(end - start).isLessThanOrEqualTo(1200);
    }

    @Test
    void sleepRandomSeconds_doesNotExceedBound() {
        long start = currentTimeMillis();
        RandomUtils.sleepRandomSeconds(1);
        long end = currentTimeMillis();
        assertThat(MILLISECONDS.toSeconds(end - start)).isLessThanOrEqualTo(1);
    }

    @Test
    void sleepRandom_doesNotExceedBound() {
        long start = currentTimeMillis();
        RandomUtils.sleepRandom(1, TimeUnit.SECONDS);
        long end = currentTimeMillis();
        assertThat(MILLISECONDS.toSeconds(end - start)).isLessThanOrEqualTo(1);
    }
}