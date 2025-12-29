package com.ebay.behavior.gds.mdm.common.testUtil;

import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class TestMetadata implements Metadata {

    private Long id;
    private UdcEntityType entityType;

    public TestMetadata(long id) {
        this.id = id;
        this.entityType = UdcEntityType.ATTRIBUTE; // Default value for testing
    }

    @Override
    public Map<String, Object> toMetadataMap(ObjectMapper objectMapper) {
        return Map.of(
                "id", getId(),
                "entityType", getEntityType().getValue()
        );
    }
}
