package com.ebay.behavior.gds.mdm.common.service;

import com.ebay.behavior.gds.mdm.common.exception.ExternalCallException;
import com.ebay.behavior.gds.mdm.common.model.QueryParam;
import com.ebay.behavior.gds.mdm.common.model.external.AckValue;
import com.ebay.behavior.gds.mdm.common.model.external.WithAckAndErrorMessage;
import com.ebay.behavior.gds.mdm.common.service.token.TokenGenerator;
import com.ebay.behavior.gds.mdm.common.util.ResourceUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.ebay.behavior.gds.mdm.common.util.ServiceConstants.LARGE_RETRY_BACKOFF;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Validated
public abstract class AbstractRestGetClient implements RestGetClient {

    @Autowired
    private ObjectMapper objectMapper;

    protected abstract String getPath();

    public abstract WebTarget getTarget();

    protected abstract TokenGenerator getTokenGenerator();

    private Response call(String path, Collection<QueryParam> queryParams) {
        var target = getTarget().path(path);
        for (val param : queryParams) {
            target = target.queryParam(param.key(), param.value());
        }

        val builder = target.request(MediaType.APPLICATION_JSON)
                .header(ACCEPT, APPLICATION_JSON_VALUE);
        return addTokenHeader(builder).get();
    }

    protected Invocation.Builder addTokenHeader(Invocation.Builder builder) {
        Optional.ofNullable(getTokenGenerator())
                .map(TokenGenerator::getTokenHeader)
                .ifPresent(header -> builder.header(header.name(), header.value()));
        return builder;
    }

    @Override
    @Retryable(retryFor = ExternalCallException.class, backoff = @Backoff(delay = LARGE_RETRY_BACKOFF), maxAttempts = 2)
    public <T> T get(String methodPath, List<QueryParam> queryParams, @NotNull Class<T> type) {
        log.debug(String.format("Get attempt for %s...", type.getSimpleName()));
        val path = concatPath(methodPath);
        val params = CollectionUtils.emptyIfNull(queryParams);
        val clientResponse = call(path, params);
        return this.extract(clientResponse, path, type);
    }

    @Override
    @Retryable(retryFor = ExternalCallException.class, backoff = @Backoff(delay = LARGE_RETRY_BACKOFF), maxAttempts = 2)
    public <T> T get(String methodPath, @NotNull Class<T> type) {
        log.debug(String.format("Get attempt for %s...", type.getSimpleName()));

        val path = concatPath(methodPath);
        val clientResponse = call(path, List.of());
        return this.extract(clientResponse, path, type);
    }

    /**
     * Fires a request and takes care of response errors.
     *
     * @param clientResponse client response object.
     * @return REST call response.
     */
    public <T> T extract(Response clientResponse, String path, Class<T> type) {
        val host = getTarget().getUri().getHost();
        val response = ResourceUtils.handleResponse(clientResponse, type, path, objectMapper, host);

        if (Objects.isNull(response)) {
            return null; // could happen only for HttpStatus = 204
        }

        if (response instanceof WithAckAndErrorMessage baseResponse) {
            val ack = baseResponse.getAck();
            if (!AckValue.SUCCESS.equals(ack)) {
                val message = String.format("Got %s response from %s for path %s", host, ack.name(), path);
                throw new ExternalCallException(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
            }
        }

        return response;
    }

    protected String concatPath(String methodPath) {
        return StringUtils.defaultString(getPath()) + StringUtils.defaultString(methodPath);
    }

    public List<QueryParam> createQueryParams(String key, String value) {
        return List.of(new QueryParam(key, value));
    }
}
