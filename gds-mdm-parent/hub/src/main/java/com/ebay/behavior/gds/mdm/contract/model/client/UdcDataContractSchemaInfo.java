package com.ebay.behavior.gds.mdm.contract.model.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UdcDataContractSchemaInfo {
    private String name;
    private String logicalType = "object";
    private String physicalName;
    private String physicalType = "topic";
    private String description;
    private List<String> tags;
    private List<UdcDataContractSchemaProperty> properties;
}
