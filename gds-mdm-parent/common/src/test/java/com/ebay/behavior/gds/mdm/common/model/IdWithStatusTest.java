package com.ebay.behavior.gds.mdm.common.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdWithStatusTest {

    @Test
    void okStatus() {
        long id = 123L;
        var message = "Success";

        var status = IdWithStatus.okStatus(id, message);

        assertThat(status.getId()).isEqualTo(id);
        assertThat(status.getHttpStatusCode()).isEqualTo(IdWithStatus.OK_VALUE);
        assertThat(status.getMessage()).isEqualTo(message);
        assertThat(status.isOk()).isTrue();
        assertThat(status.isFailed()).isFalse();
    }

    @Test
    void failedStatus() {
        long id = 123L;
        var message = "Error occurred";

        var status = IdWithStatus.failedStatus(id, message);

        assertThat(status.getId()).isEqualTo(id);
        assertThat(status.getHttpStatusCode()).isEqualTo(IdWithStatus.INTERNAL_SERVER_ERROR_VALUE);
        assertThat(status.getMessage()).isEqualTo(message);
        assertThat(status.isOk()).isFalse();
        assertThat(status.isFailed()).isTrue();
    }
}