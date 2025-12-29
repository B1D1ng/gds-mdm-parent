package com.ebay.behavior.gds.mdm.common.resource.errorMapper;

import com.ebay.cos.raptor.error.v3.ErrorCategoryEnumV3;

import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Getter
@Provider
@Component
public class NotAcceptableExceptionMapper extends AbstractExceptionMapper
        implements ExceptionMapper<NotAcceptableException> {

    private final long errorId = 12L;
    private final ErrorCategoryEnumV3 errorCategory = ErrorCategoryEnumV3.REQUEST;

    @Override
    public Response toResponse(NotAcceptableException ex) {
        return errorResponse(HttpStatus.NOT_ACCEPTABLE, ex);
    }
}
