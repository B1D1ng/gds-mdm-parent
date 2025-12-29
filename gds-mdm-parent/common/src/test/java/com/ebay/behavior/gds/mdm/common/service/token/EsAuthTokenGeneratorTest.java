package com.ebay.behavior.gds.mdm.common.service.token;

import com.ebay.behavior.gds.mdm.common.config.SamConfiguration;
import com.ebay.behavior.gds.mdm.common.service.FideliusService;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class EsAuthTokenGeneratorTest {

    private final int errorStatus = HttpStatus.INTERNAL_SERVER_ERROR.value();
    private final SamConfiguration.SamConfig samConfig = new SamConfiguration.SamConfig("domain", "name", "path");

    @Mock
    private Response response;

    @Mock
    private FideliusService fideliusService;

    @Mock
    private SamConfiguration configuration;

    @Mock
    private WebTarget target;

    @Spy
    @InjectMocks
    private EsAuthTokenGenerator service;

    @BeforeEach
    void setUp() {
        doReturn(samConfig).when(configuration).getFideliusConfig(any());
        service.init();
    }

    @Test
    void getToken_errorResponse_error() {
        doReturn(response).when(service).call(anyString(), any());
        doReturn(errorStatus).when(response).getStatus();

        assertThatThrownBy(() -> service.getToken())
                .isInstanceOf(InternalServerErrorException.class)
                .hasMessageContaining(String.format("status = %d", errorStatus));
    }

    @Test
    void getToken_esamsError_error() {
        var message = "esams error";
        doThrow(new InternalServerErrorException(message)).when(fideliusService).getAuthRequest(anyString(), anyString());

        assertThatThrownBy(() -> service.getToken())
                .isInstanceOf(InternalServerErrorException.class)
                .hasMessageContaining(message);
    }
}
