package com.ebay.behavior.gds.mdm.contract.model.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UdcDataContractSchemaProperty {
    private String name;
    private String logicalType;
    private String physicalType;
    private boolean required;
    private String description;
    private List<UdcDataContractSchemaSource> source;
    private boolean partitioned;
    private int partitionKeyPosition;
    private boolean criticalDataElement;
    private List<String> tags;
    private String classification;
}
