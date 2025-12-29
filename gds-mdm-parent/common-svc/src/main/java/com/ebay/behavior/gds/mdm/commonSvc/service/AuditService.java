package com.ebay.behavior.gds.mdm.commonSvc.service;

import com.ebay.behavior.gds.mdm.common.model.Auditable;
import com.ebay.behavior.gds.mdm.common.model.audit.AuditLogParams;
import com.ebay.behavior.gds.mdm.common.model.audit.AuditRecord;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface AuditService<A extends Auditable> {

    List<AuditRecord<A>> getAuditLog(@Valid @NotNull AuditLogParams params);
}
