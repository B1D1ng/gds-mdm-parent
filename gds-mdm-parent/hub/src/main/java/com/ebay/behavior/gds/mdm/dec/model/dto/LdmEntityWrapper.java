package com.ebay.behavior.gds.mdm.dec.model.dto;

import com.ebay.behavior.gds.mdm.dec.model.LdmEntity;
import com.ebay.behavior.gds.mdm.dec.model.PhysicalStorage;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class LdmEntityWrapper {
    LdmEntity entity;
    List<PhysicalStorage> physicalStorages;
    List<SignalStorage> signalStorages;
}
