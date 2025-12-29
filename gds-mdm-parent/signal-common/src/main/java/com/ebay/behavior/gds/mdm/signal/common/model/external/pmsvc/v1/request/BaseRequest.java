package com.ebay.behavior.gds.mdm.signal.common.model.external.pmsvc.v1.request;

import javax.xml.datatype.XMLGregorianCalendar;
import lombok.Getter;
import lombok.Setter;
import org.ebayopensource.ginger.common.types.BaseRestRequest;

@Getter
@Setter
@SuppressWarnings({"PMD.AbstractClassWithoutAnyMethod"})
public abstract class BaseRequest extends BaseRestRequest {

    protected XMLGregorianCalendar lastModifiedSince;
    protected long startOffset;
    protected int maxRecord;
}
