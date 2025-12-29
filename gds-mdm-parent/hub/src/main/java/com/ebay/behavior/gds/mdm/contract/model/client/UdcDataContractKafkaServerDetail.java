package com.ebay.behavior.gds.mdm.contract.model.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UdcDataContractKafkaServerDetail {
    private String name;
    private String type;
    private String host;
}
