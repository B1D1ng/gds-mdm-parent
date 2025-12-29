package com.ebay.behavior.gds.mdm.common.model.external;

import org.ebayopensource.ginger.common.types.ErrorMessage;

public interface WithAckAndErrorMessage {

    AckValue getAck();

    ErrorMessage getErrorMessage();
}
