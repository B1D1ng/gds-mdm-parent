package com.ebay.behavior.gds.mdm.common.service;

import com.ebay.behavior.gds.mdm.common.model.external.infohub.EsAuth;
import com.ebay.security.exceptions.EsamsException;
import com.ebay.security.nameservice.NameService;
import com.ebay.security.nameservice.NameServiceFactory;

import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.InternalServerErrorException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import static com.ebay.behavior.gds.mdm.common.util.ServiceConstants.DEFAULT_RETRY_BACKOFF;
import static com.ebay.behavior.gds.mdm.common.util.ServiceConstants.MEDIUM_RETRY_MAX_ATTEMPTS;
import static com.ebay.security.nameservice.NameService.VERSION_LAST_ENABLED;

/*
 * Note: these service account details have only been submitted to staging esams. Prod credentials were not submitted by design.
 * The reason being that at this time the features using this account credential (infohub) are only
 * used in staging. if/when that design changes note that this service account would need to be added to prod.
 */
@Service
@Validated
public class FideliusService {

    public static final String APP_NAME = "cjsmdm";

    private final NameService nameService;

    public FideliusService() {
        this(NameServiceFactory.getInstance());
    }

    public FideliusService(NameService nameService) {
        this.nameService = nameService;
    }

    @Retryable(retryFor = InternalServerErrorException.class, maxAttempts = MEDIUM_RETRY_MAX_ATTEMPTS, backoff = @Backoff(delay = DEFAULT_RETRY_BACKOFF))
    public EsAuth getAuthRequest(@NotBlank String userPath, @NotBlank String passPath) {
        return new EsAuth(getSecret(userPath), APP_NAME, getSecret(passPath));
    }

    public String getSecret(@NotBlank String path) {
        try {
            return nameService.getNonKey(path, VERSION_LAST_ENABLED).getNonKey();
        } catch (EsamsException ex) {
            throw new InternalServerErrorException(ex);
        }
    }
}
