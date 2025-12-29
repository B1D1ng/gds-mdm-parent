package com.ebay.behavior.gds.mdm.contract.resource;

import com.ebay.behavior.gds.mdm.commonTestUtil.AbstractResourceTest;
import com.ebay.behavior.gds.mdm.contract.model.StreamingConfig;
import com.ebay.behavior.gds.mdm.contract.service.KafkaSinkService;
import com.ebay.behavior.gds.mdm.contract.service.KafkaSourceService;
import com.ebay.behavior.gds.mdm.contract.service.StreamingConfigService;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.V1;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomString;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpec;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.requestSpecWithBody;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.kafkaSource;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.randomStreamingConfig;
import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.streamingConfig;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.CMM;
import static com.ebay.behavior.gds.mdm.contract.util.ApiConstants.DEFINITION;
import static org.assertj.core.api.Assertions.assertThat;

class StreamingConfigResourceIT extends AbstractResourceTest {

    private Long componentId;

    @Autowired
    private StreamingConfigService streamingConfigService;

    @Autowired
    private KafkaSourceService kafkaSourceService;

    @Autowired
    private KafkaSinkService kafkaSinkService;

    @BeforeEach
    void setup() {
        url = getBaseUrl() + V1 + CMM + DEFINITION + "/streaming-config";

        // Create a KafkaSource component to bind StreamingConfig to
        val kafkaSource = kafkaSourceService.create(kafkaSource(getRandomString()));
        componentId = kafkaSource.getId();
    }

    @Test
    void test_create_success() {
        val streamingConfig = randomStreamingConfig().toBuilder()
                .componentId(componentId)
                .build();
        val created = requestSpecWithBody(streamingConfig)
                .when().post(url)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", StreamingConfig.class);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getGroupId()).isEqualTo(streamingConfig.getGroupId());
        assertThat(created.getEnv()).isEqualTo(streamingConfig.getEnv());
        assertThat(created.getStreamName()).isEqualTo(streamingConfig.getStreamName());
        assertThat(created.getTopics()).isEqualTo(streamingConfig.getTopics());
    }

    @Test
    void test_create_failsWithDuplicateTopic() {
        // Create first config with unique env/streamName for this test
        val env = "test-dup-" + getRandomString();
        val streamName = "stream-dup-" + getRandomString();
        val config1 = streamingConfig(env, streamName, Set.of("user-events", "order-events"))
                .toBuilder().componentId(componentId).build();
        streamingConfigService.create(config1);

        // Try to create second config with overlapping topic
        val config2 = streamingConfig(env, streamName, Set.of("user-events", "payment-events"))
                .toBuilder().componentId(componentId).build();

        requestSpecWithBody(config2)
                .when().post(url)
                .then().statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    void test_create_allowsSameTopicForDifferentEnv() {
        // Use unique streamName to avoid conflicts with other tests
        val streamName = "stream-env-" + getRandomString();
        val topic = "shared-topic-" + getRandomString();

        val prodConfig = streamingConfig("prod-" + getRandomString(), streamName, Set.of(topic))
                .toBuilder().componentId(componentId).build();
        val stagingConfig = streamingConfig("staging-" + getRandomString(), streamName, Set.of(topic))
                .toBuilder().componentId(componentId).build();

        val createdProd = requestSpecWithBody(prodConfig)
                .when().post(url)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", StreamingConfig.class);

        val createdStaging = requestSpecWithBody(stagingConfig)
                .when().post(url)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", StreamingConfig.class);

        assertThat(createdProd.getId()).isNotNull();
        assertThat(createdStaging.getId()).isNotNull();
        assertThat(createdProd.getId()).isNotEqualTo(createdStaging.getId());
    }

    @Test
    void test_getById() {
        val streamingConfig = randomStreamingConfig().toBuilder()
                .componentId(componentId)
                .build();
        val created = streamingConfigService.create(streamingConfig);
        val configId = created.getId();

        val retrieved = requestSpec()
                .when().get(url + '/' + configId)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", StreamingConfig.class);

        assertThat(retrieved.getId()).isEqualTo(created.getId());
        assertThat(retrieved.getGroupId()).isEqualTo(created.getGroupId());
        assertThat(retrieved.getEnv()).isEqualTo(created.getEnv());
        assertThat(retrieved.getStreamName()).isEqualTo(created.getStreamName());
        assertThat(retrieved.getTopics()).isEqualTo(created.getTopics());
    }

    @Test
    void test_update_success() {
        val streamingConfig = randomStreamingConfig().toBuilder()
                .componentId(componentId)
                .build();
        val created = streamingConfigService.create(streamingConfig);
        val configId = created.getId();

        val updateRequest = created.toBuilder()
                .groupId("updated-group")
                .topics(Set.of("new-topic1", "new-topic2"))
                .build();

        val updated = requestSpecWithBody(updateRequest)
                .when().patch(url + "/" + configId)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", StreamingConfig.class);

        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getGroupId()).isEqualTo("updated-group");
        assertThat(updated.getTopics()).containsExactlyInAnyOrder("new-topic1", "new-topic2");
    }

    @Test
    void test_update_allowsSameConfigToKeepItsTopics() {
        // Use unique env/streamName to avoid conflicts with other tests
        val env = "env-update-" + getRandomString();
        val streamName = "stream-update-" + getRandomString();
        val topic1 = "topic1-" + getRandomString();
        val topic2 = "topic2-" + getRandomString();
        val topics = Set.of(topic1, topic2);

        val created = streamingConfigService.create(streamingConfig(env, streamName, topics).toBuilder().componentId(componentId).build());
        val configId = created.getId();

        // Update the same config with same topics - should succeed
        val updateRequest = created.toBuilder()
                .groupId("updated-group")
                .build();

        val updated = requestSpecWithBody(updateRequest)
                .when().patch(url + "/" + configId)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", StreamingConfig.class);

        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getTopics()).containsExactlyInAnyOrder(topic1, topic2);
    }

    @Test
    void test_delete() {
        val streamingConfig = randomStreamingConfig().toBuilder()
                .componentId(componentId)
                .build();
        val created = streamingConfigService.create(streamingConfig);
        val configId = created.getId();

        requestSpec().when().delete(url + "/" + configId)
                .then().statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void test_create_withMultipleTopics_failsIfAnyTopicDuplicates() {
        // Use unique env/streamName to avoid conflicts with other tests
        val env = "env-multi-" + getRandomString();
        val streamName = "stream-multi-" + getRandomString();
        val sharedTopic = "topic2-" + getRandomString();

        // Create first config with multiple topics
        streamingConfigService.create(
                streamingConfig(env, streamName, Set.of("topic1-" + getRandomString(), sharedTopic, "topic3-" + getRandomString()))
                        .toBuilder().componentId(componentId).build()
        );

        // Try to create second config where only one topic conflicts
        val config2 = streamingConfig(env, streamName, Set.of("topic4-" + getRandomString(), sharedTopic, "topic5-" + getRandomString()))
                .toBuilder().componentId(componentId).build();

        requestSpecWithBody(config2)
                .when().post(url)
                .then().statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    void test_create_allowsSameTopicForDifferentComponentType() {
        // Use unique env/streamName for this test
        val env = "env-type-" + getRandomString();
        val streamName = "stream-type-" + getRandomString();
        val topic = "shared-topic-" + getRandomString();

        // Create a KafkaSink with different type
        val kafkaSink = kafkaSinkService.create(
                com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.kafkaSink(getRandomString())
        );
        val sinkComponentId = kafkaSink.getId();

        // Create config for first component (KafkaSource type)
        val config1 = streamingConfig(env, streamName, Set.of(topic))
                .toBuilder().componentId(componentId).build();
        val created1 = requestSpecWithBody(config1)
                .when().post(url)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", StreamingConfig.class);

        // Create config for second component (KafkaSink type) with same topic
        val config2 = streamingConfig(env, streamName, Set.of(topic))
                .toBuilder().componentId(sinkComponentId).build();
        val created2 = requestSpecWithBody(config2)
                .when().post(url)
                .then().statusCode(HttpStatus.OK.value())
                .extract().body().jsonPath().getObject(".", StreamingConfig.class);

        assertThat(created1.getId()).isNotNull();
        assertThat(created2.getId()).isNotNull();
        assertThat(created1.getId()).isNotEqualTo(created2.getId());
        assertThat(created1.getTopics()).containsExactly(topic);
        assertThat(created2.getTopics()).containsExactly(topic);
    }

    @Test
    void test_create_failsWithDuplicateTopicForSameComponentType() {
        // Use unique env/streamName for this test
        val env = "env-sametype-" + getRandomString();
        val streamName = "stream-sametype-" + getRandomString();
        val topic = "dup-topic-" + getRandomString();

        // Create a second KafkaSource (same type as first)
        val kafkaSource2 = kafkaSourceService.create(kafkaSource(getRandomString()));
        val component2Id = kafkaSource2.getId();

        // Create config for first component
        val config1 = streamingConfig(env, streamName, Set.of(topic))
                .toBuilder().componentId(componentId).build();
        streamingConfigService.create(config1);

        // Try to create config for second component (same type) with same topic - should fail
        val config2 = streamingConfig(env, streamName, Set.of(topic))
                .toBuilder().componentId(component2Id).build();

        requestSpecWithBody(config2)
                .when().post(url)
                .then().statusCode(HttpStatus.CONFLICT.value());
    }
}
