package com.ebay.behavior.gds.mdm.common.service;

import com.ebay.behavior.gds.mdm.common.model.QueryParam;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface RestPostClient extends RestGetClient {

    <T> T post(String methodPath, List<QueryParam> queryParams, @NotNull Object request, @NotNull Class<T> type);

    <T> T put(String methodPath, List<QueryParam> queryParams, @NotNull Object request, @NotNull Class<T> type);

    <T> T patch(String methodPath, List<QueryParam> queryParams, Object request, @NotNull Class<T> type);
}

