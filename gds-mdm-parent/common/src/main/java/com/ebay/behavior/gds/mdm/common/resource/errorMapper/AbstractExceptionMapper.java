package com.ebay.behavior.gds.mdm.common.resource.errorMapper;

import com.ebay.behavior.gds.mdm.common.exception.MdmException;
import com.ebay.cos.raptor.error.v3.ErrorCategoryEnumV3;
import com.ebay.cos.raptor.error.v3.ErrorDetailV3;
import com.ebay.cos.raptor.error.v3.ErrorMessageV3;

import jakarta.ws.rs.core.Response;
import lombok.val;
import org.springframework.http.HttpStatus;

import java.util.Objects;

public abstract class AbstractExceptionMapper {

    public static final String ERROR_DOMAIN = "GDS-MDM";
    public static final String ERROR_SUBDOMAIN = "Onboarding";

    protected abstract long getErrorId();

    protected abstract ErrorCategoryEnumV3 getErrorCategory();

    protected Response errorResponse(HttpStatus status, Throwable th) {
        return errorResponse(status, th.getMessage(), th);
    }

    protected Response errorResponse(HttpStatus status, MdmException th) {
        val message = Objects.isNull(th.getUiMessage()) ? th.getMessage() : th.getUiMessage();
        return errorResponse(status, message, th);
    }

    protected Response errorResponse(HttpStatus status, String uiMessage, Throwable th) {
        val detail = new ErrorDetailV3();
        detail.setErrorId(getErrorId());
        detail.setDomain(ERROR_DOMAIN);
        detail.setCategory(getErrorCategory().name());
        detail.setSubdomain(ERROR_SUBDOMAIN);
        detail.setLongMessage(th.getMessage());
        detail.setMessage(uiMessage);

        val message = new ErrorMessageV3(detail);
        return Response.status(status.value()).entity(message).build();
    }
}

