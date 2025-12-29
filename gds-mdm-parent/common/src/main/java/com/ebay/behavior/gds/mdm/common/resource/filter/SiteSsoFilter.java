package com.ebay.behavior.gds.mdm.common.resource.filter;

import com.ebay.behavior.gds.mdm.common.config.SiteSsoConfiguration;
import com.ebay.platform.security.sso.config.ProviderConfig;
import com.ebay.platform.security.sso.config.SiteSsoConfig;
import com.ebay.platform.security.sso.context.ServletRequestContext;
import com.ebay.platform.security.sso.core.SiteSsoProcessor;
import com.ebay.platform.security.sso.domain.ProcessorResponse;
import com.ebay.platform.security.sso.providers.impl.DefaultProviderFactory;
import com.ebay.platform.security.trustfabric.client.TfTokenClient;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

import static com.ebay.behavior.gds.mdm.common.util.SiteSsoConstants.SITE_SSO_AUTO_LOGIN_REDIRECT;
import static com.ebay.behavior.gds.mdm.common.util.SiteSsoConstants.SITE_SSO_COOKIE_SECURE;
import static com.ebay.behavior.gds.mdm.common.util.SiteSsoConstants.SITE_SSO_HOME_URL;
import static com.ebay.behavior.gds.mdm.common.util.SiteSsoConstants.SITE_SSO_PATRONUS_KEY_AUTO_GENERATE;
import static com.ebay.behavior.gds.mdm.common.util.SiteSsoConstants.SITE_SSO_SESSION;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.AUTOWIRING_INSPECTION;

@Slf4j
@Component
@Provider
@PreMatching
@Priority(1)
@ConditionalOnProperty(prefix = "sitesso", name = "enable", havingValue = "true", matchIfMissing = false)
public class SiteSsoFilter implements ContainerRequestFilter {

    @Context
    private HttpServletRequest request;

    @Context
    private HttpServletResponse response;

    @Inject
    @Named("patronusService.patronusClient")
    @SuppressWarnings(AUTOWIRING_INSPECTION)
    private WebTarget patronusTarget;

    @Inject
    @Named("siteSsoService.siteSsoClient")
    @SuppressWarnings(AUTOWIRING_INSPECTION)
    private WebTarget siteSsoTarget;

    @Inject
    private TfTokenClient tfTokenClient;

    private SiteSsoProcessor processor;

    @Autowired
    private SiteSsoConfiguration config;

    @PostConstruct
    public void init() {
        log.info("SiteSsoFilter initialized");
        SiteSsoConfig siteSsoConfig = getSiteSsoConfig();

        ProviderConfig providerConfig = new ProviderConfig() {
            @Override
            public WebTarget getPatronusWebTarget(String webTarget) {
                return patronusTarget.path(webTarget);
            }

            @Override
            public WebTarget getSiteSsoWebTarget(String webTarget) {
                return siteSsoTarget.path(webTarget);
            }

            @Override
            public String getTrustFabricToken() {
                return tfTokenClient.getToken();
            }
        };
        processor = new SiteSsoProcessor(siteSsoConfig, new DefaultProviderFactory(providerConfig));
    }

    private SiteSsoConfig getSiteSsoConfig() {
        SiteSsoConfig config = new SiteSsoConfig();

        config.put("sitesso.session.patronus-key-auto-generate", SITE_SSO_PATRONUS_KEY_AUTO_GENERATE);
        config.put("sitesso.cookie.secure", SITE_SSO_COOKIE_SECURE);
        config.put("sitesso.filter.default-home-url", SITE_SSO_HOME_URL);
        config.put("sitesso.session.auto-login-redirect", SITE_SSO_AUTO_LOGIN_REDIRECT);
        config.put("sitesso.oidc.auth-redirect-url", this.config.getOidcAuthRedirectUrl());
        config.put("sitesso.session.patronus-key-ref", this.config.getSessionPatronusKeyRef());
        config.put("sitesso.filter.login-url", this.config.getFilterLoginUrl());
        config.put("sitesso.filter.logout-url", this.config.getFilterLogoutUrl());
        config.put("sitesso.oidc.scopes", this.config.getOidcScopes());
        config.put("sitesso.filter.allowed-cross-domains", this.config.getFilterAllowedCrossDomains());

        return config;
    }

    // Filters incoming requests and processes them using Site SSO
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        ServletRequestContext context = new ServletRequestContext(request, response);
        ProcessorResponse processorResponse = processor.doProcess(context);
        switch (processorResponse.getStatus()) {
            case DENY: {
                // Abort the request with a 401 Unauthorized response
                log.warn("site-sso request denied");
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
                break;
            }
            case EXCLUDED: {
                // Let the request pass through if it is excluded under sitesso.filter.excluded-urls configuration
                break;
            }
            case REDIRECT: {
                // Redirect the request to the specified location
                requestContext.abortWith(Response.temporaryRedirect(URI.create(processorResponse.getLocation())).build());
                break;
            }
            case LOGGED_IN: {
                // Set the user session attribute if the user is logged in
                // The SiteSsoProcessor extracts the cookie from the HttpServletRequest during its doProcess method.
                context.getRequest().setAttribute(SITE_SSO_SESSION, processorResponse.getUserSession());
                break;
            }
            case ERROR: {
                log.error("site-sso failed", processorResponse.getThrowable());
                requestContext.abortWith(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
                break;
            }
        }
    }
}
