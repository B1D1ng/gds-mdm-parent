package com.ebay.behavior.gds.mdm.contract.client;

import com.ebay.behavior.gds.mdm.common.exception.ExternalCallException;
import com.ebay.behavior.gds.mdm.contract.model.UnstagedContract;
import com.ebay.behavior.gds.mdm.contract.service.ContractConverterService;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UdcClientTest {

    private final UnstagedContract mockContract = new UnstagedContract();

    @Mock
    private ContractConverterService contractConverterService;

    @Mock
    private WebTarget target;

    @Mock
    private Invocation.Builder builder;

    @Mock
    private Response response;

    @InjectMocks
    private UdcClient udcClient;

    @Test
    void shouldThrowExceptionWhenYamlConversionFails() {
        when(target.path(any())).thenReturn(target);
        when(target.request(eq(MediaType.APPLICATION_JSON_TYPE))).thenReturn(builder);
        when(builder.post(any(Entity.class))).thenReturn(response);

        when(contractConverterService.convertUnstagedContractToYaml(any(), any())).thenReturn("");
        when(response.getStatus()).thenReturn(200);
        when(response.getStatusInfo()).thenReturn(Response.Status.OK);
        when(response.readEntity(eq(String.class))).thenReturn("{\"data\": {\"key\": \"value\"}}");

        String result = udcClient.registerContractToUdc(mockContract, "testEnv");

        assertThat(result).isEqualTo("{\"query\":\"mutation registerFromContent($contractContent: String!) {"
                + " registerFromContent(contractContent: $contractContent) { graphPK resourceGroup contractSource data"
                + "ItemGraphPK sourceCommitSha contractLocation contractCommitSha contractId contractVersion contract"
                + "Name contractStatus deleted modifiedBy content} "
                + "}\",\"variables\":{\"contractContent\":\"\"}}");
        verify(contractConverterService).convertUnstagedContractToYaml(mockContract, "testEnv");
    }

    @Test
    void shouldThrowExceptionWhenResponseIsInvalid() {
        when(contractConverterService.convertUnstagedContractToYaml(any(), any())).thenReturn("yamlContent");

        assertThatThrownBy(() -> udcClient.registerContractToUdc(mockContract, "testEnv"))
                .isInstanceOf(ExternalCallException.class)
                .hasMessageContaining("Failed to register contract");
    }

    @Test
    void shouldThrowExceptionWhenResponseContainsErrors() {
        when(contractConverterService.convertUnstagedContractToYaml(any(), any())).thenReturn("yamlContent");

        assertThatThrownBy(() -> udcClient.registerContractToUdc(mockContract, "testEnv"))
                .isInstanceOf(ExternalCallException.class)
                .hasMessageContaining("Failed to register contract");
    }

    @Test
    void shouldReturnPayloadWhenRegistrationIsSuccessful() {
        when(target.path(any())).thenReturn(target);
        when(target.request(eq(MediaType.APPLICATION_JSON_TYPE))).thenReturn(builder);
        when(builder.post(any(Entity.class))).thenReturn(response);

        when(contractConverterService.convertUnstagedContractToYaml(any(), any())).thenReturn("yamlContent");
        when(response.getStatus()).thenReturn(200);
        when(response.getStatusInfo()).thenReturn(Response.Status.OK);
        when(response.readEntity(eq(String.class))).thenReturn("{\"data\": {\"key\": \"value\"}}");

        String result = udcClient.registerContractToUdc(mockContract, "testEnv");

        assertThat(result).isEqualTo("{\"query\":\"mutation registerFromContent($contractContent: String!) {"
                + " registerFromContent(contractContent: $contractContent) { graphPK resourceGroup contractSource data"
                + "ItemGraphPK sourceCommitSha contractLocation contractCommitSha contractId contractVersion contract"
                + "Name contractStatus deleted modifiedBy content} "
                + "}\",\"variables\":{\"contractContent\":\"yamlContent\"}}");
        verify(response).readEntity(String.class);
    }
}