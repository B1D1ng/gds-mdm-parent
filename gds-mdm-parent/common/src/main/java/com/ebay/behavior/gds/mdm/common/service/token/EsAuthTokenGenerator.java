package com.ebay.behavior.gds.mdm.common.service.token;

import com.ebay.behavior.gds.mdm.common.config.SamConfiguration;
import com.ebay.behavior.gds.mdm.common.config.SamConfiguration.ConfigListElement;
import com.ebay.behavior.gds.mdm.common.config.SamConfiguration.SamConfig;
import com.ebay.behavior.gds.mdm.common.model.external.infohub.EsAuth;
import com.ebay.behavior.gds.mdm.common.model.external.infohub.EsAuthDetails;
import com.ebay.behavior.gds.mdm.common.service.FideliusService;
import com.ebay.com.google.common.annotations.VisibleForTesting;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Named;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import static com.ebay.behavior.gds.mdm.common.config.CacheConfiguration.ES_AUTH_TOKEN_CACHE;
import static com.ebay.behavior.gds.mdm.common.util.CommonValidationUtils.validateStatus;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.AUTOWIRING_INSPECTION;

@Service
public class EsAuthTokenGenerator implements TokenGenerator {

    private static final String PATH = "/authentication/service/login";
    private static final String ES_AUTH_DOMAIN = "esAuth";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    @Autowired
    private SamConfiguration config;

    @Autowired
    private FideliusService service;

    private SamConfig userConfig;

    private SamConfig passConfig;

    @Autowired
    @Named("infohubService.esauthClient")
    @SuppressWarnings(AUTOWIRING_INSPECTION)
    private WebTarget target;

    @PostConstruct
    protected void init() {
        userConfig = config.getFideliusConfig(new ConfigListElement(ES_AUTH_DOMAIN, USERNAME));
        passConfig = config.getFideliusConfig(new ConfigListElement(ES_AUTH_DOMAIN, PASSWORD));
    }

    @Override
    @Cacheable(ES_AUTH_TOKEN_CACHE)
    public String getToken() {
        val response = call(PATH, service.getAuthRequest(userConfig.getPath(), passConfig.getPath()));
        validateStatus(response, "generating esams token");
        return response.readEntity(EsAuthDetails.class).token();
    }

    @Override
    public String getTokenHeaderName() {
        return "ES-SERVICE-TOKEN";
    }

    @VisibleForTesting
    protected Response call(String path, EsAuth auth) {
        return target.path(path)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(auth, MediaType.APPLICATION_JSON));
    }
}
