package com.ebay.behavior.gds.mdm.common.util;

import lombok.experimental.UtilityClass;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@UtilityClass
public class RandomUtils {

    public Long getRandomLong(final long bound) {
        return ThreadLocalRandom.current().nextLong(0, bound);
    }

    public void sleepRandomMills(final int bound) {
        sleepRandom(bound, TimeUnit.MILLISECONDS);
    }

    public void sleepRandomSeconds(final int bound) {
        sleepRandom(bound, TimeUnit.SECONDS);
    }

    public void sleepRandom(final int bound, TimeUnit unit) {
        final long duration = getRandomLong(bound);
        try {
            unit.sleep(duration);
        } catch (InterruptedException ignored) {
        }
    }
}
