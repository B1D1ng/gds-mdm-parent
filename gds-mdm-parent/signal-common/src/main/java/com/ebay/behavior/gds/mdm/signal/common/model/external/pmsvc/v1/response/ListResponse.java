package com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.response;

import com.ebay.behavior.gds.mdm.common.model.external.AckValue;
import com.ebay.behavior.gds.mdm.common.model.external.WithAckAndErrorMessage;

import lombok.Getter;
import lombok.Setter;
import org.ebayopensource.ginger.common.types.ErrorMessage;

import java.util.List;

@Getter
@Setter
public abstract class ListResponse<T> implements WithAckAndErrorMessage {

    private long id;
    private AckValue ackValue;
    private ErrorMessage errorMessage;
    private String extension;

    @Override
    public AckValue getAck() {
        return getAckValue();
    }

    public abstract List<T> getList();
}
