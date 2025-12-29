package com.ebay.behavior.gds.mdm.common.service;

import com.ebay.behavior.gds.mdm.common.exception.ExternalCallException;
import com.ebay.behavior.gds.mdm.common.model.QueryParam;
import com.ebay.com.google.common.annotations.VisibleForTesting;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.ebay.behavior.gds.mdm.common.util.ServiceConstants.LARGE_RETRY_BACKOFF;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Validated
public abstract class AbstractRestPostClient
        extends AbstractRestGetClient
        implements RestPostClient {

    @VisibleForTesting
    public Response call(String path, Collection<QueryParam> queryParams, Object request, HttpMethod httpMethod) {
        var target = getTarget().path(path);

        for (val param : queryParams) {
            target = target.queryParam(param.key(), param.value());
        }

        var builder = target.request(APPLICATION_JSON)
                .header(ACCEPT, APPLICATION_JSON_VALUE);
        builder = addTokenHeader(builder);
        Entity<?> entity = request == null ? null : Entity.entity(request, APPLICATION_JSON);

        return switch (httpMethod) {
            case POST -> builder.post(entity);
            case PUT -> builder.put(entity);
            case PATCH -> {
                if (Objects.isNull(request)) {
                    yield builder.method(HttpMethod.PATCH.name());
                } else {
                    yield builder.method(HttpMethod.PATCH.name(), entity);
                }
            }
            default -> throw new NotImplementedException("Unsupported HTTP method: " + httpMethod);
        };
    }

    @Override
    @Retryable(retryFor = ExternalCallException.class, backoff = @Backoff(delay = LARGE_RETRY_BACKOFF), maxAttempts = 2)
    public <T> T post(String methodPath, List<QueryParam> queryParams, @NotNull Object request, @NotNull Class<T> type) {
        return executeRequest(methodPath, queryParams, request, type, HttpMethod.POST);
    }

    @Override
    @Retryable(retryFor = ExternalCallException.class, backoff = @Backoff(delay = LARGE_RETRY_BACKOFF), maxAttempts = 2)
    public <T> T put(String methodPath, List<QueryParam> queryParams, @NotNull Object request, @NotNull Class<T> type) {
        return executeRequest(methodPath, queryParams, request, type, HttpMethod.PUT);
    }

    @Override
    @Retryable(retryFor = ExternalCallException.class, backoff = @Backoff(delay = LARGE_RETRY_BACKOFF), maxAttempts = 2)
    public <T> T patch(String methodPath, List<QueryParam> queryParams, Object request, @NotNull Class<T> type) {
        return executeRequest(methodPath, queryParams, request, type, HttpMethod.PATCH);
    }

    private <T> T executeRequest(String methodPath, List<QueryParam> queryParams, Object request, Class<T> type, HttpMethod httpMethod) {
        log.debug(String.format("%s extractor attempt for %s...", httpMethod.name(), type.getSimpleName()));
        val path = concatPath(methodPath);
        val params = CollectionUtils.emptyIfNull(queryParams);
        try {
            val clientResponse = call(path, params, request, httpMethod);
            return this.extract(clientResponse, path, type);
        } catch (ProcessingException ex) {
            throw new ExternalCallException(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage(), ex);
        }
    }
}
