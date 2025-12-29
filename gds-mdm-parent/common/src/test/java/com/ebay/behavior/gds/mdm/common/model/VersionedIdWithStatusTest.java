package com.ebay.behavior.gds.mdm.common.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VersionedIdWithStatusTest {

    @Test
    void okStatus() {
        long id = 123L;
        int version = 1;
        var message = "Success";

        var status = VersionedIdWithStatus.okStatus(id, version, message);

        assertThat(status.getId()).isEqualTo(id);
        assertThat(status.getVersion()).isEqualTo(version);
        assertThat(status.getHttpStatusCode()).isEqualTo(VersionedIdWithStatus.OK_VALUE);
        assertThat(status.getMessage()).isEqualTo(message);
        assertThat(status.isOk()).isTrue();
        assertThat(status.isFailed()).isFalse();
    }

    @Test
    void failedStatus() {
        long id = 123L;
        int version = 1;
        var message = "Error occurred";

        var status = VersionedIdWithStatus.failedStatus(id, version, message);

        assertThat(status.getId()).isEqualTo(id);
        assertThat(status.getVersion()).isEqualTo(version);
        assertThat(status.getHttpStatusCode()).isEqualTo(VersionedIdWithStatus.INTERNAL_SERVER_ERROR_VALUE);
        assertThat(status.getMessage()).isEqualTo(message);
        assertThat(status.isOk()).isFalse();
        assertThat(status.isFailed()).isTrue();
    }
}