package com.ebay.behavior.gds.mdm.common.resource.errorMapper;

import com.ebay.cos.raptor.error.v3.ErrorCategoryEnumV3;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Getter
@Provider
@Component
public class BadRequestExceptionMapper extends AbstractExceptionMapper
        implements ExceptionMapper<BadRequestException> {

    private final long errorId = 3L;
    private final ErrorCategoryEnumV3 errorCategory = ErrorCategoryEnumV3.REQUEST;

    @Override
    public Response toResponse(BadRequestException ex) {
        return errorResponse(HttpStatus.BAD_REQUEST, ex);
    }
}
