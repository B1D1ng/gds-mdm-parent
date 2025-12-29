package com.ebay.behavior.gds.mdm.common.resource.errorMapper;

import com.ebay.cos.raptor.error.v3.ErrorCategoryEnumV3;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.Getter;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Getter
@Provider
@Component
public class DataAcccessExceptionMapper extends AbstractExceptionMapper
        implements ExceptionMapper<InvalidDataAccessResourceUsageException> {

    private final long errorId = 16L;
    private final ErrorCategoryEnumV3 errorCategory = ErrorCategoryEnumV3.REQUEST;

    @Override
    public Response toResponse(InvalidDataAccessResourceUsageException ex) {
        var message = ex.getMessage();
        var maybeUiMessage = Optional.ofNullable(ex.getRootCause()).map(Throwable::getMessage);

        if (maybeUiMessage.isPresent() && !maybeUiMessage.get().equals(message)) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, maybeUiMessage.get(), ex);
        }

        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex);
    }
}
