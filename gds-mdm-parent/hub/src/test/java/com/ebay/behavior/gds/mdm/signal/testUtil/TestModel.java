package com.ebay.behavior.gds.mdm.signal.testUtil;

import com.ebay.behavior.gds.mdm.common.model.Auditable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.With;

import java.sql.Timestamp;

@Getter
@Setter
@With
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TestModel implements Auditable {

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
}
