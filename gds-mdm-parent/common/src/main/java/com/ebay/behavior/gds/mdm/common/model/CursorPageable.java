package com.ebay.behavior.gds.mdm.common.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Sort;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class CursorPageable {

    private String cursor;

    @Positive
    private final int pageSize;

    @NotNull
    private Sort sort;

    private boolean unpaged;

    public static CursorPageable unpaged() {
        return new CursorPageable(null, 0, Sort.unsorted(), true);
    }

    public static CursorPageable of(int pageSize) {
        return new CursorPageable(null, pageSize, Sort.unsorted());
    }

    public static CursorPageable of(int pageSize, Sort sort) {
        return new CursorPageable(null, pageSize, sort);
    }

    public static CursorPageable of(String cursor, int pageSize) {
        return new CursorPageable(cursor, pageSize, Sort.unsorted());
    }

    public static CursorPageable of(String cursor, int pageSize, Sort sort) {
        return new CursorPageable(cursor, pageSize, sort);
    }

    private CursorPageable(String cursor, int pageSize, Sort sort) {
        this.cursor = cursor;
        this.pageSize = pageSize;
        this.sort = sort;
    }

    public boolean isPaged() {
        return !unpaged;
    }

    public boolean isUnpaged() {
        return unpaged;
    }
}