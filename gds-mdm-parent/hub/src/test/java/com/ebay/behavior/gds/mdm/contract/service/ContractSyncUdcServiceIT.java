package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.contract.client.UdcClient;
import com.ebay.behavior.gds.mdm.contract.model.KafkaSink;
import com.ebay.behavior.gds.mdm.contract.model.KafkaSource;
import com.ebay.behavior.gds.mdm.contract.model.Routing;
import com.ebay.behavior.gds.mdm.contract.model.StreamingConfig;
import com.ebay.behavior.gds.mdm.contract.model.Transformation;
import com.ebay.behavior.gds.mdm.contract.model.Transformer;
import com.ebay.behavior.gds.mdm.contract.model.UnstagedContract;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.ebay.behavior.gds.mdm.common.util.SpringConstants.IT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles(IT)
class ContractSyncUdcServiceIT {

    @Autowired
    private ContractSyncUdcService contractSyncUdcService;

    @Autowired
    private UdcClient udcClient;

    @MockitoBean
    private UnstagedContractService unstagedContractService;

    private UnstagedContract contract;
    private String udcContractId;

    @BeforeEach
    void initMocks() {
        MockitoAnnotations.openMocks(this);
    }

    @BeforeEach
    void setUp() {
        contract = new UnstagedContract();
        contract.setId(999L);
        contract.setName("TestContract");
        contract.setVersion(1);
        contract.setDescription("Test Description");
        contract.setOwners("owner1,owner2");
        contract.setCreateDate(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));

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
        transformation.setExpression("""
                ROW_OF(
                  MAP[
                    'id', CAST(currentRecord.item_id AS STRING)
                  ],
                  CAST(NULL AS MAP<VARCHAR, VARCHAR>),
                  CAST(NULL AS MAP<VARCHAR, VARCHAR>),
                  MAP[
                    'source', currentRecord.source,
                    'evaluationTimestamp', currentRecord.evaluation_timestamp,
                    'complianceProductType', currentRecord.compliance_features.compliance_product_type,
                    'isBladed', currentRecord.compliance_features.is_bladed
                  ],
                  CAST(NULL AS MAP<VARCHAR, VARCHAR>),
                  CAST(NULL AS MAP<VARCHAR, VARCHAR>),
                  CAST(NULL AS MAP<VARCHAR, VARCHAR>),
                  CAST(NULL AS MAP<VARCHAR, VARCHAR>)
                )""");
        transformation.setFieldType("Struct");
        transformation.setDescription("Item context, contains all item domain fields.");
        transformation.setOwners("ywang73");

        Transformer transformer1 = new Transformer();
        transformer1.setName("Transformer1");
        transformer1.setType("Transformer");
        transformer1.setDescription("First Transformer");
        transformer1.setTransformations(Set.of(transformation));

        routing.setComponentChain(List.of(kafkaSource, transformer1, kafkaSink));
        contract.setRoutings(Set.of(routing));

        udcContractId = UUID.nameUUIDFromBytes(contract.getName().getBytes()).toString();
    }

    @Test
    @Disabled
    void shouldSyncContractToUdc() {
        assertThat(mockingDetails(unstagedContractService).isMock()).isTrue();
        when(unstagedContractService.getLatestVersion(anyLong())).thenReturn(1);
        when(unstagedContractService.getByIdWithAssociations(any(), anyBoolean())).thenReturn(contract);

        String response = contractSyncUdcService.syncContractToUdc(contract.getId(), "testEnv");

        assertThat(response).isNotNull();
    }

    @AfterEach
    void tearDown() {
        if (udcContractId != null) {
            udcClient.deleteContractFromUdc(udcContractId, "testEnv");
        }
    }
}