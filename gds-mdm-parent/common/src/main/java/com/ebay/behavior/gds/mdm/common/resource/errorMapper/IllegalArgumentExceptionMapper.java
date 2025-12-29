package com.ebay.behavior.gds.mdm.common.resource.errorMapper;

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
public class IllegalArgumentExceptionMapper extends AbstractExceptionMapper
        implements ExceptionMapper<IllegalArgumentException> {

    private final long errorId = 2L;
    private final ErrorCategoryEnumV3 errorCategory = ErrorCategoryEnumV3.REQUEST;

    @Override
    public Response toResponse(IllegalArgumentException ex) {
        return errorResponse(HttpStatus.BAD_REQUEST, ex);
    }
}
