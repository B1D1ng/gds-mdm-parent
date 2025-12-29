package com.ebay.behavior.gds.mdm.contract.client;

import com.ebay.behavior.gds.mdm.common.exception.ExternalCallException;
import com.ebay.behavior.gds.mdm.contract.model.UnstagedContract;
import com.ebay.behavior.gds.mdm.contract.model.client.UdcDataContractRegisterPayload;
import com.ebay.behavior.gds.mdm.contract.service.ContractConverterService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Named;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class UdcClient {

    @Autowired
    private ContractConverterService contractConverterService;

    @Autowired
    @Named("udcContractMetadata")
    private WebTarget target;

    private String errorMessage = "Failed to register contract: %s";

    public String registerContractToUdc(UnstagedContract unstagedContract, String env) {
        var yamlContract = contractConverterService.convertUnstagedContractToYaml(unstagedContract, env);
        if (yamlContract == null) {
            var message = String.format("Failed to convert contract to YAML");
            throw new ExternalCallException(HttpStatus.BAD_REQUEST.value(), message);
        }

        String payload = buildPayload(yamlContract);

        try (Response response = this.target.path("/graphql")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(payload))) {
            if (isInvalidResponse(response)) {
                var message = String.format(errorMessage, response.getStatusInfo().getReasonPhrase());
                throw new ExternalCallException(HttpStatus.BAD_REQUEST.value(), message);
            }

            String responseText = response.readEntity(String.class);
            if (hasErrors(responseText)) {
                var message = String.format(errorMessage, responseText);
                throw new ExternalCallException(HttpStatus.OK.value(), message);
            }

            log.info("Contract registered successfully");
            return payload;
        } catch (Exception ex) {
            var message = String.format(errorMessage, ex);
            throw new ExternalCallException(message, ex);
        }
    }

    public boolean deleteContractFromUdc(String contractUdcKey, String env) {
        try (Response response = this.target.path("/graphql")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(buildDeletePayload(contractUdcKey)))) {
            if (response.getStatus() == HttpStatus.OK.value()) {
                log.info("Contract deleted successfully");
                return true;
            }
            log.error("Failed to delete contract: {}", response.getStatusInfo().getReasonPhrase());
            return false;
        } catch (Exception ex) {
            log.error("Exception occurred while deleting contract", ex);
            return false;
        }
    }

    private String buildDeletePayload(String contractUdcKey) {
        // 构建删除合同的 GraphQL 请求 payload
        return String.format("{\"query\":\"mutation deleteContract($key: String!) { deleteContract(key: $key) }\",\""
                + "variables\":{\"key\":\"%s\"}}", contractUdcKey);
    }

    protected String buildPayload(String yamlContract) {
        UdcDataContractRegisterPayload payload = new UdcDataContractRegisterPayload();
        payload.setVariables(Map.of("contractContent", yamlContract));

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new ExternalCallException(e);
        }
    }

    private boolean isInvalidResponse(Response response) {
        return response == null || response.getStatusInfo() == null || response.getStatus() != HttpStatus.OK.value();
    }

    protected boolean hasErrors(String responseBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            return responseMap.containsKey("errors") && responseMap.get("errors") != null;
        } catch (Exception e) {
            log.error("Failed to parse response body", e);
            return false;
        }
    }
}

