package com.ebay.behavior.gds.mdm.signal.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HadoopSojEvent {

    private String eactn;
    private Long pageId;
    private Long moduleId;
    private Long clickId;
    private String tags;

    public String toKey() {
        return String.format("%s;%s;%s;%s", eactn, pageId, moduleId, clickId);
    }
}
