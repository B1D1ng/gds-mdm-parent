package com.ebay.behavior.gds.mdm.dec.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class SignalStorage {
    private String signalDefinitionId;
    private List<StorageDetail> storageDetails;
}
