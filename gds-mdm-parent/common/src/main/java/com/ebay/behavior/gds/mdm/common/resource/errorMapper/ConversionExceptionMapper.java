package com.ebay.behavior.gds.mdm.common.resource.errorMapper;

import com.ebay.cos.raptor.error.v3.ErrorCategoryEnumV3;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.Getter;
import org.apache.commons.beanutils.ConversionException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Getter
@Provider
@Component
public class ConversionExceptionMapper extends AbstractExceptionMapper
        implements ExceptionMapper<ConversionException> {

    private final long errorId = 15L;
    private final ErrorCategoryEnumV3 errorCategory = ErrorCategoryEnumV3.REQUEST;

    @Override
    public Response toResponse(ConversionException ex) {
        return errorResponse(HttpStatus.BAD_REQUEST, ex);
    }
}
