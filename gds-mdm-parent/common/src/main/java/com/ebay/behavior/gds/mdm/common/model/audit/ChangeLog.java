package com.ebay.behavior.gds.mdm.common.model.audit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangeLog {
    private String changeType;
    private String entityType;
    private Long entityId;
    private Object left;
    private Object right;
    private String propertyName;
}
