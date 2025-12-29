package com.ebay.behavior.gds.mdm.contract.model.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UdcDataContractSchemaSource {
    private List<String> table;
    private String type;
    private String transform;
    private String transformType;
    private String contact;
}