package com.ebay.behavior.gds.mdm.contract.testUtil;

import com.ebay.behavior.gds.mdm.common.model.Environment;
import com.ebay.behavior.gds.mdm.common.model.ExpressionType;
import com.ebay.behavior.gds.mdm.contract.model.BesSource;
import com.ebay.behavior.gds.mdm.contract.model.ContractStatus;
import com.ebay.behavior.gds.mdm.contract.model.DoneFileType;
import com.ebay.behavior.gds.mdm.contract.model.Filter;
import com.ebay.behavior.gds.mdm.contract.model.HiveConfig;
import com.ebay.behavior.gds.mdm.contract.model.HiveSource;
import com.ebay.behavior.gds.mdm.contract.model.HiveStorage;
import com.ebay.behavior.gds.mdm.contract.model.KafkaSink;
import com.ebay.behavior.gds.mdm.contract.model.KafkaSource;
import com.ebay.behavior.gds.mdm.contract.model.LdmViewSink;
import com.ebay.behavior.gds.mdm.contract.model.Routing;
import com.ebay.behavior.gds.mdm.contract.model.StreamingConfig;
import com.ebay.behavior.gds.mdm.contract.model.Transformation;
import com.ebay.behavior.gds.mdm.contract.model.Transformer;
import com.ebay.behavior.gds.mdm.contract.model.UnstagedContract;
import com.ebay.behavior.gds.mdm.contract.model.UpdateContractRequest;

import com.google.common.collect.Lists;
import lombok.experimental.UtilityClass;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomString;

@UtilityClass
public class TestModelUtils {
    public static UnstagedContract unstagedContract(String name) {
        return UnstagedContract.builder()
                .name(name)
                .dl("dl")
                .description("description")
                .owners("owners")
                .domain("domain")
                .status(ContractStatus.IN_DEVELOPMENT)
                .environment(Environment.UNSTAGED)
                .build();
    }

    public static Routing routing(String name) {
        return Routing.builder()
                .name(name)
                .contractId(1L)
                .contractVersion(1)
                .build();
    }

    public static KafkaSource kafkaSource(String name) {
        return KafkaSource.builder()
                .name(name)
                .metadataId("udcId")
                .connectorType("kafka")
                .dl("dl")
                .description("description")
                .owners("owners")
                .type("KafkaSource")
                .connectorType("connectorType")
                .build();
    }

    public static KafkaSink kafkaSink(String name) {
        return KafkaSink.builder()
                .name(name)
                .metadataId("udcId")
                .connectorType("kafka")
                .dl("dl")
                .description("description")
                .owners("owners")
                .type("KafkaSink")
                .build();
    }

    public static Transformation transformation(String field, String description, String expression) {
        return Transformation.builder()
                .field(field)
                .description(description)
                .expression(expression)
                .expressionType(ExpressionType.SQL)
                .fieldType("String")
                .owners("owners")
                .build();
    }

    public static Filter filter(String statement) {
        return Filter.builder()
                .type(ExpressionType.LITERAL)
                .statement(statement)
                .build();
    }

    public static StreamingConfig randomStreamingConfig() {
        return streamingConfig(getRandomString(), getRandomString(), Set.of(getRandomString()));
    }

    public static StreamingConfig streamingConfig(String group) {
        return StreamingConfig.builder()
                .groupId(group)
                .env("sampleEnv")
                .streamName("sampleStream")
                .schemaId(1L)
                .format("sampleFormat")
                .scanStartupMode("sampleMode")
                .properties("sampleProperties")
                .topics(Set.of("sampleTopic"))
                .build();
    }

    public static StreamingConfig streamingConfig(String env, String streamName, Set<String> topics) {
        return StreamingConfig.builder()
                .groupId("sampleGroup")
                .env(env)
                .streamName(streamName)
                .schemaId(1L)
                .format("sampleFormat")
                .scanStartupMode("sampleMode")
                .properties("sampleProperties")
                .topics(topics)
                .build();
    }

    public static Transformer transformer(String name) {
        return Transformer.builder()
                .name(name)
                .dl("dl")
                .description("description")
                .owners("owners")
                .type("Transformer")
                .build();
    }

    public static UpdateContractRequest updateContractRequest(String name) {
        return UpdateContractRequest.builder()
                .name(name)
                .dl("dl")
                .description("description")
                .owners("owners")
                .status(ContractStatus.IN_DEVELOPMENT)
                .environment(Environment.UNSTAGED)
                .build();
    }

    public static BesSource besSource(String name) {
        return BesSource.builder()
                .name(name)
                .metadataId("besId")
                .connectorType("BES")
                .dl("dl")
                .description("description")
                .owners("owners")
                .type("BesSource")
                .build();
    }

    public static LdmViewSink ldmViewSink(String name) {
        return LdmViewSink.builder()
                .name(name)
                .viewId(2L)
                .dl("dl")
                .description("description")
                .owners("owners")
                .type("LdmViewSink")
                .build();
    }

    public static HiveSource hiveSource(String name) {
        return HiveSource.builder()
                .name(name)
                .dl("dl")
                .description("description")
                .owners("owners")
                .type("HiveSource")
                .build();
    }

    public static HiveConfig hiveConfig(Long componentId) {
        return HiveConfig.builder()
                .componentId(componentId)
                .env(Environment.UNSTAGED)
                .build();
    }

    public static HiveConfig createHiveConfig(Environment env, HiveStorage storage) {
        return HiveConfig.builder()
                .env(env)
                .hiveStorage(storage)
                .build();
    }

    public static HiveStorage hiveStorage(String tableName) {
        return HiveStorage.builder()
                .dbName("sample_db")
                .tableName(tableName)
                .dataCenter(Lists.newArrayList("apollo"))
                .format("parquet")
                .doneFilePath("/path/to/donefile")
                .doneFileType(DoneFileType.FILE).build();
    }

    public static HiveStorage createHiveStorage(String tableName, String donePath) {
        return HiveStorage.builder()
                .tableName(tableName)
                .doneFilePath(donePath)
                .build();
    }
}