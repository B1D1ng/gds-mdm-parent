package com.ebay.behavior.gds.mdm.common.resource.errorMapper;

import com.ebay.behavior.gds.mdm.common.exception.ExternalCallException;
import com.ebay.cos.raptor.error.v3.ErrorCategoryEnumV3;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Getter
@Provider
@Component
public class ExternalCallExceptionMapper extends AbstractExceptionMapper
        implements ExceptionMapper<ExternalCallException> {

    private final long errorId = 9L;
    private final ErrorCategoryEnumV3 errorCategory = ErrorCategoryEnumV3.BUSINESS;

    @Override
    public Response toResponse(ExternalCallException ex) {
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex);
    }
}
