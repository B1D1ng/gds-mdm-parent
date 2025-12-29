package com.ebay.behavior.gds.mdm.contract.client;

import com.ebay.behavior.gds.mdm.contract.model.client.ControlPlaneRequest;
import com.ebay.behavior.gds.mdm.contract.model.client.ControlPlaneResponse;
import com.ebay.behavior.gds.mdm.contract.model.client.ProcessType;
import com.ebay.behavior.gds.mdm.contract.model.exception.ControlPlaneManagerException;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.val;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static com.ebay.behavior.gds.mdm.contract.client.ControlPlaneManagerClient.CONTRACT_RESOURCE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControlPlaneManagerClientTest {

    @Mock
    private WebTarget target;

    @InjectMocks
    private ControlPlaneManagerClient client;

    private ControlPlaneRequest request;
    private Response response;

    @BeforeEach
    void setup() {
        val builder = mock(Invocation.Builder.class);
        when(target.request(any(MediaType.class))).thenReturn(builder);
        response = mock(Response.class);
        when(builder.post(any(Entity.class))).thenReturn(response);

        request = ControlPlaneRequest.builder()
                .resourceType(CONTRACT_RESOURCE)
                .resourceId("contract-123")
                .processType(ProcessType.DEPLOY.name())
                .description("Deploy contract for testing")
                .context(Map.of(
                        "environment", "STAGING",
                        "test", "true",
                        "contractId", "contract-123"
                ))
                .requester("test-user")
                .build();
    }

    @Test
    void requestWorkflow_success() {
        when(response.getStatus()).thenReturn(HttpStatus.SC_CREATED);
        val res = new ControlPlaneResponse();
        res.setId(1L);
        res.setStatus("SUBMITTED");
        res.setResourceType(CONTRACT_RESOURCE);
        res.setResourceId("contract-123");
        res.setProcessType(ProcessType.DEPLOY.name());
        res.setWorkflowId("workflow-456");
        res.setRequester("test-user");
        when(response.readEntity(ControlPlaneResponse.class)).thenReturn(res);

        val result = client.requestWorkflow(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("SUBMITTED");
        assertThat(result.getResourceType()).isEqualTo(CONTRACT_RESOURCE);
        assertThat(result.getResourceId()).isEqualTo("contract-123");
        assertThat(result.getProcessType()).isEqualTo(ProcessType.DEPLOY.name());
        assertThat(result.getWorkflowId()).isEqualTo("workflow-456");
        assertThat(result.getRequester()).isEqualTo("test-user");
    }

    @Test
    void requestWorkflow_serverError_throwsException() {
        when(response.getStatus()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        when(response.readEntity(String.class)).thenReturn("Internal server error");

        assertThatThrownBy(() -> client.requestWorkflow(request))
                .isInstanceOf(ControlPlaneManagerException.class)
                .hasMessage("com.ebay.behavior.gds.mdm.contract.model.exception.ControlPlaneManagerException: Internal server error");
    }

    @Test
    void requestWorkflow_badRequest_throwsException() {
        when(response.getStatus()).thenReturn(HttpStatus.SC_BAD_REQUEST);
        when(response.readEntity(String.class)).thenReturn("Invalid request parameters");

        assertThatThrownBy(() -> client.requestWorkflow(request))
                .isInstanceOf(ControlPlaneManagerException.class)
                .hasMessage("com.ebay.behavior.gds.mdm.contract.model.exception.ControlPlaneManagerException: Invalid request parameters");
    }

    @Test
    void requestWorkflow_runtimeException_throwsControlPlaneManagerException() {
        when(response.getStatus()).thenThrow(new RuntimeException("Connection timeout"));

        assertThatThrownBy(() -> client.requestWorkflow(request))
                .isInstanceOf(ControlPlaneManagerException.class)
                .hasCauseInstanceOf(RuntimeException.class);
    }
}