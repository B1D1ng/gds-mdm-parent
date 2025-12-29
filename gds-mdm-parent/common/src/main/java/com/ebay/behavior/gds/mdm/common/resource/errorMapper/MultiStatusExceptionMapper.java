package com.ebay.behavior.gds.mdm.common.resource.errorMapper;

import com.ebay.behavior.gds.mdm.common.exception.PartialSuccessException;
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
public class MultiStatusExceptionMapper extends AbstractExceptionMapper
        implements ExceptionMapper<PartialSuccessException> {

    private final long errorId = 13L;
    private final ErrorCategoryEnumV3 errorCategory = ErrorCategoryEnumV3.APPLICATION;

    @Override
    public Response toResponse(PartialSuccessException ex) {
        return Response.status(HttpStatus.MULTI_STATUS.value()).entity(ex.getStatuses()).build();
    }
}
