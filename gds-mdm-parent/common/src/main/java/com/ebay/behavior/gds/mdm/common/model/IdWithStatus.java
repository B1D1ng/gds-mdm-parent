package com.ebay.behavior.gds.mdm.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IdWithStatus {

    public static final int OK_VALUE = 200;
    public static final int INTERNAL_SERVER_ERROR_VALUE = 500;

    @NotNull
    @PositiveOrZero
    protected Long id;

    @NotNull
    @Positive
    protected Integer httpStatusCode;

    protected String message;

    public static IdWithStatus okStatus(long id) {
        return new IdWithStatus(id, OK_VALUE, null);
    }

    public static IdWithStatus okStatus(long id, String message) {
        return new IdWithStatus(id, OK_VALUE, message);
    }

    public static IdWithStatus failedStatus(long id, String message) {
        return new IdWithStatus(id, INTERNAL_SERVER_ERROR_VALUE, message);
    }

    @JsonIgnore
    public boolean isOk() {
        return httpStatusCode == OK_VALUE;
    }

    @JsonIgnore
    public boolean isFailed() {
        return !isOk();
    }
}