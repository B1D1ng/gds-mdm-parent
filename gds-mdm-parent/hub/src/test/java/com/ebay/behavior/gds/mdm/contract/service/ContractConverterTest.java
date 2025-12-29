package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.contract.model.KafkaSink;
import com.ebay.behavior.gds.mdm.contract.model.KafkaSource;
import com.ebay.behavior.gds.mdm.contract.model.Routing;
import com.ebay.behavior.gds.mdm.contract.model.StreamingConfig;
import com.ebay.behavior.gds.mdm.contract.model.Transformation;
import com.ebay.behavior.gds.mdm.contract.model.Transformer;
import com.ebay.behavior.gds.mdm.contract.model.UnstagedContract;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.INTEGRATION_TEST;
import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(IT)
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ContractConverterTest {
    @Autowired
    private ContractConverterService contractConverterService;

    @Test
    void convertUnstagedContractToYamlShouldReturnValidYaml() {
        // Arrange
        UnstagedContract contract = new UnstagedContract();

        contract.setName("TestContract");
        contract.setVersion(1);
        contract.setDescription("Test Description");
        contract.setOwners("owner1,owner2");
        contract.setCreateDate(java.sql.Timestamp.valueOf(now()));

        Routing routing = new Routing();
        routing.setName("TestRouting");

        KafkaSource kafkaSource = new KafkaSource();
        kafkaSource.setName("TestKafkaSource");
        kafkaSource.setType("KafkaSource");
        kafkaSource.setDescription("Kafka Source Description");

        KafkaSink kafkaSink = new KafkaSink();
        kafkaSink.setName("TestKafkaSink");
        kafkaSink.setType("KafkaSink");
        kafkaSink.setDescription("Kafka Sink Description");

        StreamingConfig streamingConfig = new StreamingConfig();
        streamingConfig.setEnv("testEnv");
        streamingConfig.setStreamName("testStream");
        streamingConfig.setTopics(Set.of("testTopic1", "testTopic2"));
        streamingConfig.setProperties("{\"key\":\"value\"}");

        kafkaSink.setStreamingConfigs(Set.of(streamingConfig));
        kafkaSource.setStreamingConfigs(Set.of(streamingConfig));

        Transformation transformation = new Transformation();
        transformation.setRevision(0);
        transformation.setCreateBy("unknown");
        transformation.setUpdateBy("unknown");
        transformation.setField("itemContext");
        transformation.setExpression("ROW_OF(\n" +
                "  MAP[\n" +
                "    'id', CAST(currentRecord.item_id AS STRING)\n" +
                "  ],\n" +
                "  CAST(NULL AS MAP<VARCHAR, VARCHAR>),\n" +
                "  CAST(NULL AS MAP<VARCHAR, VARCHAR>),\n" +
                "  MAP[\n" +
                "    'source', currentRecord.source,\n" +
                "    'evaluationTimestamp', currentRecord.evaluation_timestamp,\n" +
                "    'complianceProductType', currentRecord.compliance_features.compliance_product_type,\n" +
                "    'isBladed', currentRecord.compliance_features.is_bladed\n" +
                "  ],\n" +
                "  CAST(NULL AS MAP<VARCHAR, VARCHAR>),\n" +
                "  CAST(NULL AS MAP<VARCHAR, VARCHAR>),\n" +
                "  CAST(NULL AS MAP<VARCHAR, VARCHAR>),\n" +
                "  CAST(NULL AS MAP<VARCHAR, VARCHAR>)\n" +
                ")");
        transformation.setFieldType("Struct");
        transformation.setDescription("Item context, contains all item domain fields.");
        transformation.setOwners("testOwnerName");

        Transformer transformer1 = new Transformer();
        transformer1.setName("Transformer1");
        transformer1.setType("Transformer");
        transformer1.setDescription("First Transformer");
        transformer1.setTransformations(Set.of(transformation));

        routing.setComponentChain(List.of(kafkaSource, transformer1, kafkaSink));
        contract.setRoutings(Set.of(routing));

        contract.setDl("testName@ebay.com");
        contract.setCreateDate(java.sql.Timestamp.valueOf(now()));

        // Act
        String yaml = contractConverterService.convertUnstagedContractToYaml(contract, "testEnv");

        // Assert
        assertThat(yaml).isNotNull();
        assertThat(yaml).contains("TestContract");
        assertThat(yaml).contains("testStream");
        assertThat(yaml).contains("testTopic");
    }

    @Test
    void convertUnstagedContractToYamlShouldReturnNullOnException() {
        String yaml = contractConverterService.convertUnstagedContractToYaml(null, "testEnv");

        assertThat(yaml).isNull();
    }
}