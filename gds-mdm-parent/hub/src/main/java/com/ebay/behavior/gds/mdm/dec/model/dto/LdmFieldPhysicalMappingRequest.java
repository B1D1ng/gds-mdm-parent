package com.ebay.behavior.gds.mdm.dec.model.dto;

import java.sql.Timestamp;
import java.util.Set;

public record LdmFieldPhysicalMappingRequest(Long fieldId, Set<Long> storageIds, String createBy, Timestamp createDate) {
}
