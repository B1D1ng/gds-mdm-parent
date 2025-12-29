package com.ebay.behavior.gds.mdm.common.service;

import com.ebay.behavior.gds.mdm.common.model.external.muse.UserDetails;

import jakarta.inject.Named;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.client.WebTarget;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import static com.ebay.behavior.gds.mdm.common.config.CacheConfiguration.AUTH_TOKEN_CACHE;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.AUTOWIRING_INSPECTION;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Service
@Validated
public class AuthService {

    public static final String MUSE_GINGER_CLIENT_NAME = "museauth";
    public static final String MUSE_APP_HEADER = "muse-app";
    public static final String VALIDATE_API_PATH = "/validate";

    private final WebTarget target;

    @Autowired
    @SuppressWarnings(AUTOWIRING_INSPECTION)
    public AuthService(@Named(MUSE_GINGER_CLIENT_NAME) WebTarget target) {
        this.target = target;
    }

    @Cacheable(AUTH_TOKEN_CACHE)
    public String getUser(@NotBlank String token) {
        val response = target.path(VALIDATE_API_PATH).request()
                .header(AUTHORIZATION, token)
                .header(MUSE_APP_HEADER, "cjsportal,gdsgovtool")
                .header(ACCEPT, APPLICATION_JSON_VALUE)
                .get(UserDetails.class);

        return response.getUsername();
    }
}