package com.ebay.behavior.gds.mdm.common.service.token;

import com.ebay.platform.security.trustfabric.client.TfTokenClient;
import com.ebay.platform.security.trustfabric.exception.TokenException;

import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UdcTokenGeneratorTest {

    @Mock
    private TfTokenClient tfTokenClient;

    @InjectMocks
    private UdcTokenGenerator tokenGenerator;

    @Test
    void getToken_clientNotAvailable_error() {
        when(tfTokenClient.isTokenAvailable()).thenReturn(false);

        assertThatThrownBy(() -> tokenGenerator.getToken())
                .isInstanceOf(TokenException.class);
    }

    @Test
    void getToken() {
        var value = "token";
        when(tfTokenClient.isTokenAvailable()).thenReturn(true);
        when(tfTokenClient.getTokenWithBearerPrefix()).thenReturn(value);

        var token = tokenGenerator.getToken();

        assertThat(token).isEqualTo(value);
    }

    @Test
    void getTokenHeaderName() {
        var headerName = tokenGenerator.getTokenHeaderName();

        assertThat(headerName).isEqualTo(HttpHeaders.AUTHORIZATION);
    }
}