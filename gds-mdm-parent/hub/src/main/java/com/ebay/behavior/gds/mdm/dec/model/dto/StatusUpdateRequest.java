package com.ebay.behavior.gds.mdm.dec.model.dto;

import java.sql.Timestamp;

public record StatusUpdateRequest(Long id, String status, String updateBy, Timestamp updateDate) {
}
