package com.ebay.behavior.gds.mdm.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.Validate;
import org.springframework.data.domain.Sort;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public final class EsPageable {

    public static final Integer PAGE_MIN_OFFSET = 0;
    public static final Integer PAGE_MAX_SIZE = 10_000;

    @PositiveOrZero
    private int from;

    @Positive
    private int size;

    @Valid
    private Sort sort;

    private boolean isUnpaged;

    private EsPageable(int from, int size, Sort sort, boolean isUnpaged) {
        this.from = from;
        this.size = size;
        this.sort = sort;
        this.isUnpaged = isUnpaged;
    }

    private static void validate(int from, int size) {
        Validate.isTrue(from >= 0, "\"from\" must be greater than or equal to zero");
        Validate.isTrue(size > 0, "\"size\" must be greater than zero");
    }

    public boolean isPaged() {
        return !isUnpaged;
    }

    public static EsPageable of(int from, int size) {
        validate(from, size);
        return new EsPageable(from, size, Sort.unsorted(), false);
    }

    public static EsPageable of(int from, int size, Sort sort) {
        validate(from, size);
        return new EsPageable(from, size, sort, false);
    }

    public static EsPageable unpaged() {
        return new EsPageable(0, 0, Sort.unsorted(), true);
    }

    @SuppressWarnings("PMD.NullAssignment")
    public void nullifySort() {
        this.sort = null;
    }
}
