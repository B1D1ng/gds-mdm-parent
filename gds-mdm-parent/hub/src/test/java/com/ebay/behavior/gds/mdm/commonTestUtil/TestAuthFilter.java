package com.ebay.behavior.gds.mdm.commonTestUtil;

import com.ebay.behavior.gds.mdm.common.util.ResourceUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Profile("IT")
public class TestAuthFilter extends OncePerRequestFilter implements Ordered {

    public static final String IT_TEST_USER = "IT_test_user";

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ResourceUtils.setRequestUser(IT_TEST_USER);
        filterChain.doFilter(request, response);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
