package com.ebay.behavior.gds.mdm.contract.model.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UdcDataContractRegisterPayload {
    @Builder.Default
    private String query = "mutation registerFromContent($contractContent: String!) { registerFromContent("
            + "contractContent: $contractContent) { graphPK resourceGroup contractSource dataItemGraphPK"
            + " sourceCommitSha contractLocation contractCommitSha contractId contractVersion contractName"
            + " contractStatus deleted modifiedBy content} }";
    private Map<String, Object> variables;
}
