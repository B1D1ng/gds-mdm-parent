package com.ebay.behavior.gds.mdm.commonSvc.util;

import com.ebay.behavior.gds.mdm.common.model.Auditable;
import com.ebay.behavior.gds.mdm.common.model.Metadata;
import com.ebay.behavior.gds.mdm.common.model.external.udc.UdcEntityType;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.With;

import java.sql.Timestamp;
import java.util.Map;

@Getter
@Setter
@With
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TestModel implements Auditable, Metadata {

    private Long id;
    private Long parentId;
    private Integer revision;
    private String name;
    private String description;
    private String createBy;
    private Timestamp createDate;
    private String updateBy;
    private Timestamp updateDate;

    @Override
    public Long getParentId() {
        return parentId;
    }

    @Override
    public UdcEntityType getEntityType() {
        return UdcEntityType.TRANSFORMATION;
    }

    @Override
    public Map<String, Object> toMetadataMap(ObjectMapper objectMapper) {
        return Map.of();
    }
}
