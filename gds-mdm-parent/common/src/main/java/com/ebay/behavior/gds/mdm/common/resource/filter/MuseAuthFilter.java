package com.ebay.behavior.gds.mdm.common.resource.filter;

import com.ebay.behavior.gds.mdm.common.service.AuthService;
import com.ebay.behavior.gds.mdm.common.util.ResourceUtils;
import com.ebay.kernel.util.StringUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;

@Component
@Profile("!IT")
@ConditionalOnProperty(prefix = "sitesso", name = "enable", havingValue = "false", matchIfMissing = true)
public class MuseAuthFilter extends OncePerRequestFilter {

    @Autowired
    private AuthService authService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        val token = request.getHeader(AUTHORIZATION);
        if (!StringUtils.isEmpty(token)) {
            val user = authService.getUser(token);
            ResourceUtils.setRequestUser(user);
        }

        filterChain.doFilter(request, response);
    }
}
