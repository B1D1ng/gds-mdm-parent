package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.contract.model.AbstractStreamingComponent;
import com.ebay.behavior.gds.mdm.contract.model.KafkaSink;
import com.ebay.behavior.gds.mdm.contract.model.Routing;
import com.ebay.behavior.gds.mdm.contract.model.StreamingConfig;
import com.ebay.behavior.gds.mdm.contract.model.Transformer;
import com.ebay.behavior.gds.mdm.contract.model.UnstagedContract;
import com.ebay.behavior.gds.mdm.contract.model.client.UdcDataContract;
import com.ebay.behavior.gds.mdm.contract.model.client.UdcDataContractKafkaServerDetail;
import com.ebay.behavior.gds.mdm.contract.model.client.UdcDataContractSchemaInfo;
import com.ebay.behavior.gds.mdm.contract.model.client.UdcDataContractSchemaProperty;
import com.ebay.behavior.gds.mdm.contract.model.client.UdcDataContractSchemaSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@Validated
public class ContractConverterService {

    private static final String KAFKASINK = "KafkaSink";

    public String convertUnstagedContractToYaml(UnstagedContract unstagedContract, String env) {
        try {
            var udcDataContract = UdcDataContract.builder()
                    .id(UUID.nameUUIDFromBytes(unstagedContract.getName().getBytes()).toString())
                    .version(buildContractVersion(unstagedContract.getVersion()))
                    .name(unstagedContract.getName())
                    .description(unstagedContract.getDescription())
                    .primaryOwner(unstagedContract.getOwners())
                    .contact(unstagedContract.getDl())
                    .servers(extractServers(unstagedContract, env))
                    .schema(extractSchemas(unstagedContract, env))
                    .contractCreatedTs(unstagedContract.getCreateDate() != null
                            ? String.valueOf(unstagedContract.getCreateDate().getTime() / 1000)
                            : "0")
                    .lastReleasedDate(String.valueOf(System.currentTimeMillis() / 1000))
                    .build();

            ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
            return yamlMapper.writeValueAsString(udcDataContract);
        } catch (Exception e) {
            log.error("Failed to convert UnstagedContract to YAML", e);
            return null;
        }
    }

    private StreamingConfig extractStreamingConfigs(AbstractStreamingComponent kafkaComponent, String env) {
        if (kafkaComponent == null) {
            return null;
        }

        var kafkaComponentList = kafkaComponent.getStreamingConfigs()
                .stream()
                .filter(config -> org.apache.commons.lang3.StringUtils.equals(config.getEnv(), env))
                .toList();

        return kafkaComponentList.isEmpty() ? null : kafkaComponentList.get(0);
    }

    private List<UdcDataContractKafkaServerDetail> extractServers(UnstagedContract unstagedContract, String env) {
        if (unstagedContract == null) {
            return Collections.emptyList();
        }

        var servers = new ArrayList<UdcDataContractKafkaServerDetail>();
        var routings = unstagedContract.getRoutings();
        for (var routing : routings) {
            var sinkKafkaList = routing.getComponentChain()
                    .stream()
                    .filter(component -> org.apache.commons.lang3.StringUtils.equals(component.getType(), KAFKASINK))
                    .map(component -> (KafkaSink) component)
                    .toList();

            var sinkKafka = sinkKafkaList.isEmpty() ? new KafkaSink() : sinkKafkaList.get(0);
            var sinkKafkaConfig = extractStreamingConfigs(sinkKafka, env);

            if (sinkKafkaConfig != null) {
                servers.add(new UdcDataContractKafkaServerDetail(
                        sinkKafkaConfig.getStreamName(),
                        "kafka",
                        sinkKafkaConfig.getEnv()
                ));
            }
        }
        return servers;
    }

    private List<UdcDataContractSchemaInfo> extractSchemas(UnstagedContract unstagedContract, String env) {
        if (unstagedContract == null) {
            return Collections.emptyList();
        }

        var schemaList = new ArrayList<UdcDataContractSchemaInfo>();
        for (var routing : unstagedContract.getRoutings()) {
            var sourceKafkaConfig = getKafkaConfig(routing, "KafkaSource", env);
            String sourceKafkaTopicName = buildTopicName(sourceKafkaConfig);

            var sinkKafkaConfig = getKafkaConfig(routing, KAFKASINK, env);
            String sinkKafkaTopicName = buildTopicName(sinkKafkaConfig);

            var transformers = routing.getComponentChain()
                    .stream()
                    .filter(component -> org.apache.commons.lang3.StringUtils.equals(component.getType(), "Transformer"))
                    .map(component -> (Transformer) component)
                    .toList();

            var schemaProperties = new ArrayList<UdcDataContractSchemaProperty>();
            transformers.stream()
                    .reduce((first, second) -> second)
                    .ifPresent(lastTransformer -> lastTransformer.getTransformations().forEach(transformation -> {
                        var source = new UdcDataContractSchemaSource(
                                List.of(sourceKafkaTopicName),
                                "topic",
                                convertTransformSql(transformation.getExpression()),
                                "SQL",
                                transformation.getOwners()
                        );

                        var schemaProperty = new UdcDataContractSchemaProperty(
                                transformation.getField(),
                                transformation.getFieldType(),
                                transformation.getFieldType(),
                                true,
                                transformation.getDescription(),
                                List.of(source),
                                false,
                                -1,
                                false,
                                List.of(),
                                "internal"
                        );
                        schemaProperties.add(schemaProperty);
                    }));

            var sinkKafkaDescription = getKafkaDescription(routing, KAFKASINK);

            var schemaInfo = new UdcDataContractSchemaInfo(
                    sinkKafkaTopicName,
                    "object",
                    sinkKafkaTopicName,
                    "topic",
                    sinkKafkaDescription,
                    new ArrayList<>(),
                    schemaProperties
            );

            schemaList.add(schemaInfo);
        }

        return schemaList;
    }

    private String getKafkaDescription(Routing routing, String type) {
        var kafkaList = routing.getComponentChain()
                .stream()
                .filter(component -> org.apache.commons.lang3.StringUtils.equals(component.getType(), type))
                .map(component -> (AbstractStreamingComponent) component)
                .toList();
        var kafkaItem = kafkaList.isEmpty() ? new KafkaSink() : kafkaList.get(0);

        return kafkaItem.getDescription();
    }

    private StreamingConfig getKafkaConfig(Routing routing, String type, String env) {
        return routing.getComponentChain()
                .stream()
                .filter(component -> org.apache.commons.lang3.StringUtils.equals(component.getType(), type))
                .map(component -> (AbstractStreamingComponent) component)
                .map(component -> extractStreamingConfigs(component, env))
                .findFirst()
                .orElse(null);
    }

    private String buildTopicName(StreamingConfig streamingConfig) {
        if (streamingConfig == null || streamingConfig.getStreamName() == null || streamingConfig.getTopics().isEmpty()) {
            return "";
        }
        return streamingConfig.getTopics()
                .stream()
                .map(topic -> streamingConfig.getStreamName() + "/" + topic)
                .collect(Collectors.joining(","));
    }

    private String buildContractVersion(Integer version) {
        if (version == null) {
            return "";
        }
        return String.format("%d.%d.%d", version, 0, 0);
    }

    private String convertTransformSql(String originalSql) {
        if (originalSql == null || originalSql.isEmpty()) {
            return "";
        }

        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        try {
            return yamlMapper.readValue(originalSql, String.class);
        } catch (Exception e) {
            log.error("Failed to convert YAML transformSQL to String", e);
            return "";
        }
    }
}
