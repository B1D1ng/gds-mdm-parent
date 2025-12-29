package com.ebay.behavior.gds.mdm.common.resource.filter;

import com.ebay.platform.security.sso.context.ServletRequestContext;
import com.ebay.platform.security.sso.core.SiteSsoProcessor;
import com.ebay.platform.security.sso.domain.ProcessorResponse;
import com.ebay.platform.security.sso.domain.UserSession;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Collections;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SiteSsoFilterTest {
    @Mock
    private HttpServletRequest servletRequest;

    @Mock
    private SiteSsoProcessor processor;

    @Mock
    private ContainerRequestContext requestContext;

    @InjectMocks
    private SiteSsoFilter siteSsoFilter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void filter_deny_unauthorized() throws IOException {
        ProcessorResponse processorResponse = new ProcessorResponse(ProcessorResponse.Status.DENY);
        when(processor.doProcess(any(ServletRequestContext.class))).thenReturn(processorResponse);

        siteSsoFilter.filter(requestContext);

        verify(requestContext)
                .abortWith(argThat(response -> response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode()));
    }

    @Test
    void filter_redirect_to_login() throws IOException {
        ProcessorResponse processorResponse = ProcessorResponse.redirect("http://example.com");
        when(processor.doProcess(any(ServletRequestContext.class))).thenReturn(processorResponse);

        siteSsoFilter.filter(requestContext);

        verify(requestContext)
                .abortWith(argThat(response -> response.getStatus() == Response.Status.TEMPORARY_REDIRECT.getStatusCode()));
    }

    @Test
    void filter_loggedIn_continue() throws IOException {
        UserSession userSession = new UserSession();
        ProcessorResponse processorResponse = ProcessorResponse.loggedIn(userSession);
        when(processor.doProcess(any(ServletRequestContext.class))).thenReturn(processorResponse);
        when(servletRequest.getAttribute("USER-ROLES")).thenReturn(Collections.emptyList());

        siteSsoFilter.filter(requestContext);

        verify(servletRequest).setAttribute("SITE-SSO-SESSION", userSession);
    }

    @Test
    void filter_error_handling() throws IOException {
        ProcessorResponse processorResponse = new ProcessorResponse(ProcessorResponse.Status.ERROR);
        when(processor.doProcess(any(ServletRequestContext.class))).thenReturn(processorResponse);

        siteSsoFilter.filter(requestContext);

        verify(requestContext)
                .abortWith(argThat(response -> response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }
}
