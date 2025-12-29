package com.ebay.behavior.gds.mdm.common.service;

import com.ebay.behavior.gds.mdm.common.model.QueryParam;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface RestGetClient {

    enum HttpMethod {
        GET, HEAD, POST, PUT, DELETE, OPTIONS, TRACE, PATCH
    }

    <T> T get(String methodPath, List<QueryParam> queryParams, @NotNull Class<T> type);

    <T> T get(String methodPath, @NotNull Class<T> type);
}

