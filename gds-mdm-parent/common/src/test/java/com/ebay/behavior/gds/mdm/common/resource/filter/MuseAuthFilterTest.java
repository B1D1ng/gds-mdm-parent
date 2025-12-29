package com.ebay.behavior.gds.mdm.common.resource.filter;

import com.ebay.behavior.gds.mdm.common.service.AuthService;
import com.ebay.behavior.gds.mdm.common.testUtil.TestRequestContextUtils;
import com.ebay.behavior.gds.mdm.common.util.ResourceUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.HttpHeaders;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MuseAuthFilterTest {

    @Mock
    private AuthService authService;

    @Spy
    @InjectMocks
    private MuseAuthFilter museAuthFilter;

    @Test
    void doFilterInternal() throws ServletException, IOException {
        var request = mock(HttpServletRequest.class);
        var token = RandomStringUtils.randomAlphanumeric(10);
        var user = RandomStringUtils.randomAlphanumeric(10);

        when(authService.getUser(token)).thenReturn(user);
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(token);
        TestRequestContextUtils.setRequestAttributes();

        museAuthFilter.doFilterInternal(request, null, mock(FilterChain.class));

        var userSetInFilter = ResourceUtils.getRequestUser();
        assertThat(userSetInFilter).isEqualTo(user);
    }
}