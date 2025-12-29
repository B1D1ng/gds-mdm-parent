package com.ebay.behavior.gds.mdm.dec.model.dto;

import com.ebay.behavior.gds.mdm.dec.model.enums.SignalStorageType;

public record StorageDetail(SignalStorageType storageType, String assetName, String assetSchema, String doneFile, String frequency) {
}
