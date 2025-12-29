package com.ebay.behavior.gds.mdm.signal.common.model.migration;

import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public enum SignalMigrationJobType {

    HOURLY(1, "hourly",DateTimeFormatter.ofPattern("yyyyMMddHH")),
    CUSTOM(4, "custom", DateTimeFormatter.ofPattern("yyyyMMddHH"));

    @Getter
    private final int value;

    @Getter
    private final String name;

    private final transient DateTimeFormatter format;

    SignalMigrationJobType(int value, String name, DateTimeFormatter format) {
        this.value = value;
        this.name = name;
        this.format = format;
    }

    /**
     * A unique per type and date key. Will return same key, for same type / date combination,
     * so we can ensure only single signal migration execution on many hosts, that simultaneously try to run same sync.
     *
     * @param ldt A trigger date (current date).
     * @return A sync key.
     */
    public long getKey(LocalDateTime ldt) {
        return Long.parseLong("999999" + value + format.format(ldt));
    }
}
