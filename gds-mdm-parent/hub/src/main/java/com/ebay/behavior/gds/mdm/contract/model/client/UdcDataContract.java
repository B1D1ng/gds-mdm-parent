package com.ebay.behavior.gds.mdm.contract.model.client;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UdcDataContract {
    @Builder.Default
    private String apiVersion = "v0.1.0";

    @Builder.Default
    private String kind = "SourceDataContract";

    @Builder.Default
    private String dataProduct = "GDS Unified Collection";

    @Builder.Default
    private String status = "active";

    @Builder.Default
    private String resourceGroup = "CJS";

    @Builder.Default
    private String managementGroup = "Customer Journey Signals";

    @Builder.Default
    private String domain = "Data and Metrics";

    @Builder.Default
    private String executiveOrganization = "Cloud Data Technologies";

    @Builder.Default
    private String organization = "Core Technology";

    @Builder.Default
    private int tier = 3;

    @Builder.Default
    private List<UdcDataContractSupportDetail> support = List.of(
            new UdcDataContractSupportDetail(
                    "#cjs-oncall",
                    "slack",
                    "https://join.slack.com/share/enQtODg5MTIzNTY1Mjk2NC0xOGUzYjI5NDZiMmU4ZTA4ZThhNTNiMjg5YTQ5MGI5Y2QzNTAyNDY4Y2IyYTQ5MTIzYzFkMzdjMmFjNDY2NTIy"
            )
    );

    @Builder.Default
    private List<String> tags = List.of();

    private String id;
    private String version;
    private String name;
    private String description;
    private String primaryOwner;
    private String contact;
    private List<UdcDataContractKafkaServerDetail> servers;
    private List<UdcDataContractSchemaInfo> schema;
    private String contractCreatedTs;
    private String lastReleasedDate;
}