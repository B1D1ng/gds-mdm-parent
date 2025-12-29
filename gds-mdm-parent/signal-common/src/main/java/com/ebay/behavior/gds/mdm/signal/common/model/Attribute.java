package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.Auditable;
import com.ebay.behavior.gds.mdm.common.model.JavaType;

public interface Attribute extends Auditable {

    String getTag();

    String getDescription();

    JavaType getJavaType();

    String getSchemaPath();

    Boolean getIsStoreInState();
}
