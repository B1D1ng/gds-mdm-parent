package com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm;

import com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.datatype.SignalDefinition;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LegacySignalRecord {
    private String signalId;
    private List<SignalDefinition> versions;
}
