package com.ebay.behavior.gds.mdm.signal.common.model.migration;

import com.ebay.behavior.gds.mdm.common.model.VersionedIdWithStatus;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import static com.ebay.behavior.gds.mdm.signal.common.model.migration.MigrationStepStatus.NOT_STARTED;

@Getter
public final class SignalMigrationStatus extends VersionedIdWithStatus {

    private final String signalName;

    private MigrationStepStatus status;

    private SignalMigrationStatus(String signalName, Integer version, MigrationStepStatus status) {
        super(0L, version, HttpStatus.OK.value(), null);
        this.signalName = signalName;
        this.status = status;
    }

    public static SignalMigrationStatus startStep(String signalName, Integer version) {
        return new SignalMigrationStatus(signalName, version, NOT_STARTED);
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOkStatus(MigrationStepStatus status) {
        setStep(status, true, null);
    }

    public void setFailedStatus(MigrationStepStatus status, String errorMessage) {
        setStep(status, false, errorMessage);
    }

    private void setStep(MigrationStepStatus status, boolean isOk, String message) {
        this.httpStatusCode = isOk ? OK_VALUE : INTERNAL_SERVER_ERROR_VALUE;
        this.status = status;
        this.message = message;
    }
}
