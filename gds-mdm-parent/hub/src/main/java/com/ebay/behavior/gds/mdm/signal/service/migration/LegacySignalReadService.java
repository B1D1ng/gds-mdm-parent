package com.ebay.behavior.gds.mdm.signal.service.migration;

import com.ebay.behavior.gds.mdm.common.model.QueryParam;
import com.ebay.behavior.gds.mdm.common.service.AbstractRestGetClient;
import com.ebay.behavior.gds.mdm.common.service.token.TokenGenerator;
import com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.datatype.SignalDefinition;
import com.ebay.behavior.gds.mdm.signal.common.model.external.legacymdm.response.SignalApiResponse;

import jakarta.inject.Named;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.client.WebTarget;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static com.ebay.behavior.gds.mdm.common.model.search.Search.PLATFORM;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.AUTOWIRING_INSPECTION;
import static com.ebay.behavior.gds.mdm.signal.util.ApiConstants.LEGACY_MDM_CLIENT_NAME;
import static com.ebay.behavior.gds.mdm.signal.util.CacheConstants.ALL_LEGACY_SIGNALS_CACHE;

@Slf4j
@Service
@Validated
public class LegacySignalReadService extends AbstractRestGetClient {

    private static final String SIGNAL_DEFINITION_PATH = "/signal_meta/v1";

    @Getter
    private final WebTarget target;

    @SuppressWarnings(AUTOWIRING_INSPECTION)
    public LegacySignalReadService(@Named(LEGACY_MDM_CLIENT_NAME) WebTarget target) {
        this.target = target;
    }

    @Cacheable(value = ALL_LEGACY_SIGNALS_CACHE, sync = true, key = "#platformName")
    public List<SignalDefinition> readAll(@NotBlank String platformName) {
        val response = get(SIGNAL_DEFINITION_PATH, List.of(new QueryParam(PLATFORM, platformName)), SignalApiResponse.class);
        return response.getData().getRecords();
    }

    @Override
    protected String getPath() {
        return "";
    }

    @Override
    protected TokenGenerator getTokenGenerator() {
        return null;
    }
}
