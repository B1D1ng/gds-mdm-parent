package com.ebay.behavior.gds.mdm.signal.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class LegacyMapperConstants {

    // CJSONB-473: We've defined new signal and event types in the new MDM portal
    // legacy signal type
    public static final String SIGNAL_MODULE_CLICK = "MODULE_CLICK";
    // current signal type
    public static final String SIGNAL_OFFSITE_CLICK = "OFFSITE_CLICK";
    public static final String SIGNAL_ONSITE_CLICK = "ONSITE_CLICK";
    // legacy event type
    public static final String EVENT_MODULE_CLICK = "MODULE_CLICK";
    public static final String EVENT_SOJ_CLICK = "SOJ_CLICK";
    // current event type
    public static final String EVENT_OFFSITE_EVENT = "OFFSITE_EVENT";
}
