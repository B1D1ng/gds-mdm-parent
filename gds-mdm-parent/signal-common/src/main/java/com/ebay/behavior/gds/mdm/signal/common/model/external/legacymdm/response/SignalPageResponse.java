package com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.response;

import com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.datatype.SignalDefinition;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SignalPageResponse {
    private Integer count;

    private Integer offset;

    private Integer limit;

    private List<SignalDefinition> records;

    public static SignalPageResponse of(int total, int offset, int limit, List<SignalDefinition> records) {
        return new SignalPageResponse(total, offset, limit, records);
    }
}