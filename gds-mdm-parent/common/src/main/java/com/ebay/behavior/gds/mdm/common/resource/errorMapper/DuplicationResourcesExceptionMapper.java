package com.ebay.behavior.gds.mdm.common.resource.errorMapper;

import com.ebay.behavior.gds.mdm.common.exception.DuplicateResourceException;
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
public class DuplicationResourcesExceptionMapper extends AbstractExceptionMapper
        implements ExceptionMapper<DuplicateResourceException> {

    private final long errorId = 40_901;
    private final ErrorCategoryEnumV3 errorCategory = ErrorCategoryEnumV3.REQUEST;

    @Override
    public Response toResponse(DuplicateResourceException ex) {
        return errorResponse(HttpStatus.CONFLICT, ex);
    }
}
