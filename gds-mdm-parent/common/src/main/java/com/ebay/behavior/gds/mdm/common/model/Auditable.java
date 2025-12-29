package com.ebay.behavior.gds.mdm.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.sql.Timestamp;

public interface Auditable extends Model {

    String CREATE_DATE = "createDate";
    String CREATE_BY = "createBy";
    String UPDATE_DATE = "updateDate";
    String UPDATE_BY = "updateBy";

    @JsonIgnore
    default Long getParentId() {
        return null;
    }

    String getCreateBy();

    String getUpdateBy();

    Timestamp getCreateDate();

    Timestamp getUpdateDate();
}
