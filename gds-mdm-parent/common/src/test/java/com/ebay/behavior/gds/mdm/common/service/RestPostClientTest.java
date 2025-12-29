package com.ebay.behavior.gds.mdm.common.service;

import com.ebay.behavior.gds.mdm.common.exception.ExternalCallException;
import com.ebay.behavior.gds.mdm.common.model.external.AckValue;
import com.ebay.behavior.gds.mdm.common.model.external.WithAckAndErrorMessage;
import com.ebay.behavior.gds.mdm.common.service.token.UdcTokenGenerator;
import com.ebay.behavior.gds.mdm.common.util.ResourceUtils;

import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;

import static com.ebay.behavior.gds.mdm.common.service.RestGetClient.HttpMethod.PATCH;
import static com.ebay.behavior.gds.mdm.common.service.RestGetClient.HttpMethod.POST;
import static com.ebay.behavior.gds.mdm.common.service.RestGetClient.HttpMethod.PUT;
import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.AUTOWIRING_INSPECTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class RestPostClientTest {

    @Mock
    private WebTarget target;

    @Mock
    private URI uri;

    @Mock
    private UdcTokenGenerator tokenGenerator;

    @Mock
    private Response response;

    @Mock
    private WithAckAndErrorMessage ackResponse;

    @Spy
    @InjectMocks
    private TestRestPostClient client;

    @Test
    void post() {
        doReturn(response).when(client).call(anyString(), any(), anyString(), eq(POST));
        doReturn("response").when(client).extract(any(Response.class), anyString(), any());

        var result = client.post("/testMethod", null, "request", String.class);

        assertThat(result).isEqualTo("response");
    }

    @Test
    void post_withWithFailedAckResponse_error() {
        doReturn(response).when(client).call(anyString(), any(), anyString(), eq(POST));
        doReturn(uri).when(target).getUri();
        doReturn("host").when(uri).getHost();
        doReturn(AckValue.FAILURE).when(ackResponse).getAck();

        try (MockedStatic<ResourceUtils> utilities = mockStatic(ResourceUtils.class)) {
            utilities.when(() -> ResourceUtils.handleResponse(any(), any(), any(), any(), any()))
                    .thenReturn(ackResponse);

            assertThatThrownBy(() -> client.post("/testMethod", null, "request", String.class))
                    .isInstanceOf(ExternalCallException.class);
        }
    }

    @Test
    void put() {
        doReturn(response).when(client).call(anyString(), any(), anyString(), eq(PUT));
        doReturn("response").when(client).extract(any(Response.class), anyString(), any());

        var result = client.put("/testMethod", null, "request", String.class);

        assertThat(result).isEqualTo("response");
    }

    @Test
    void patch_withBody() {
        doReturn(response).when(client).call(anyString(), any(), anyString(), eq(PATCH));
        doReturn("response").when(client).extract(any(Response.class), anyString(), any());

        var result = client.patch("/testMethod", null, "request", String.class);

        assertThat(result).isEqualTo("response");
    }

    @Test
    void patch_withoutBody() {
        doReturn(response).when(client).call(anyString(), any(), Mockito.isNull(), eq(PATCH));
        doReturn("response").when(client).extract(any(Response.class), anyString(), any());

        var result = client.patch("/testMethod", null, null, String.class);

        assertThat(result).isEqualTo("response");
    }

    @Getter
    @SuppressWarnings(AUTOWIRING_INSPECTION)
    private static class TestRestPostClient extends AbstractRestPostClient {

        @Autowired
        private final WebTarget target;

        @Autowired
        private final UdcTokenGenerator tokenGenerator;

        private final String path = "path";

        public TestRestPostClient(WebTarget target, UdcTokenGenerator tokenGenerator) {
            this.target = target;
            this.tokenGenerator = tokenGenerator;
        }
    }
}