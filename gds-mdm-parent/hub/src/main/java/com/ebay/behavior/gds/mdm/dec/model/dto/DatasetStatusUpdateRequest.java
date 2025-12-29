package com.ebay.behavior.gds.mdm.dec.model.dto;

import com.ebay.behavior.gds.mdm.dec.model.enums.PlatformEnvironment;

import java.sql.Timestamp;

public record DatasetStatusUpdateRequest(Long id, PlatformEnvironment env, String status, String updateBy, Timestamp updateDate) {
}
