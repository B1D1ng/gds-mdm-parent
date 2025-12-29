package com.ebay.behavior.gds.mdm.contract.service;

import com.ebay.behavior.gds.mdm.common.exception.DuplicateResourceException;
import com.ebay.behavior.gds.mdm.contract.model.StreamingConfig;
import com.ebay.behavior.gds.mdm.contract.repository.StreamingConfigRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Set;

import static com.ebay.behavior.gds.mdm.contract.testUtil.TestModelUtils.streamingConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StreamingConfigServiceTest {

    @InjectMocks
    private StreamingConfigService service;

    @Mock
    private StreamingConfigRepository repository;

    private static final Long TEST_ID = 1L;
    private static final String ENV = "prod";
    private static final String STREAM_NAME = "orders";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void test_create_success_noExistingTopics() {
        var newConfig = streamingConfig(ENV, STREAM_NAME, Set.of("topic1", "topic2"))
                .toBuilder().componentId(100L).build();
        var savedConfig = newConfig.toBuilder().id(TEST_ID).build();

        when(repository.existsByTopicsInAndEnvAndStreamNameAndTypeAndIdNot(
                anyCollection(), eq(ENV), eq(STREAM_NAME), eq(100L), isNull()
        )).thenReturn(false);
        when(repository.save(newConfig)).thenReturn(savedConfig);

        var result = service.create(newConfig);

        assertThat(result).isEqualTo(savedConfig);
        assertThat(result.getId()).isEqualTo(TEST_ID);
        verify(repository).existsByTopicsInAndEnvAndStreamNameAndTypeAndIdNot(
                newConfig.getTopics(), ENV, STREAM_NAME, 100L, null
        );
        verify(repository).save(newConfig);
    }

    @Test
    void test_create_throwsException_whenTopicAlreadyExists() {
        var newConfig = streamingConfig(ENV, STREAM_NAME, Set.of("duplicate-topic"))
                .toBuilder().componentId(100L).build();

        when(repository.existsByTopicsInAndEnvAndStreamNameAndTypeAndIdNot(
                anyCollection(), eq(ENV), eq(STREAM_NAME), eq(100L), isNull()
        )).thenReturn(true);

        assertThatThrownBy(() -> service.create(newConfig))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Duplicate resources violation")
                .hasMessageContaining("duplicate-topic")
                .hasMessageContaining(ENV)
                .hasMessageContaining(STREAM_NAME);

        verify(repository).existsByTopicsInAndEnvAndStreamNameAndTypeAndIdNot(
                newConfig.getTopics(), ENV, STREAM_NAME, 100L, null
        );
        verify(repository, never()).save(any());
    }

    @Test
    void test_validateUniqueTopics_skipsValidation_whenTopicsNull() {
        var configWithoutTopics = streamingConfig(ENV, STREAM_NAME, null)
                .toBuilder().componentId(100L).build();

        service.validateUniqueTopics(configWithoutTopics);

        verify(repository, never()).existsByTopicsInAndEnvAndStreamNameAndTypeAndIdNot(
                anyCollection(), anyString(), anyString(), anyLong(), anyLong()
        );
    }

    @Test
    void test_validateUniqueTopics_skipsValidation_whenTopicsEmpty() {
        var configWithEmptyTopics = streamingConfig(ENV, STREAM_NAME, Set.of())
                .toBuilder().componentId(100L).build();

        service.validateUniqueTopics(configWithEmptyTopics);

        verify(repository, never()).existsByTopicsInAndEnvAndStreamNameAndTypeAndIdNot(
                anyCollection(), anyString(), anyString(), anyLong(), anyLong()
        );
    }

    @Test
    void test_validateUniqueTopics_skipsValidation_whenEnvNull() {
        var configWithoutEnv = streamingConfig(null, STREAM_NAME, Set.of("topic1"))
                .toBuilder().componentId(100L).build();

        service.validateUniqueTopics(configWithoutEnv);

        verify(repository, never()).existsByTopicsInAndEnvAndStreamNameAndTypeAndIdNot(
                anyCollection(), anyString(), anyString(), anyLong(), anyLong()
        );
    }

    @Test
    void test_validateUniqueTopics_skipsValidation_whenStreamNameNull() {
        var configWithoutStreamName = streamingConfig(ENV, null, Set.of("topic1"))
                .toBuilder().componentId(100L).build();

        service.validateUniqueTopics(configWithoutStreamName);

        verify(repository, never()).existsByTopicsInAndEnvAndStreamNameAndTypeAndIdNot(
                anyCollection(), anyString(), anyString(), anyLong(), anyLong()
        );
    }

    @Test
    void test_create_allowsSameTopics_forDifferentEnv() {
        var config1 = streamingConfig("prod", STREAM_NAME, Set.of("topic1"))
                .toBuilder().componentId(100L).build();
        var config2 = streamingConfig("staging", STREAM_NAME, Set.of("topic1"))
                .toBuilder().componentId(100L).build();

        when(repository.existsByTopicsInAndEnvAndStreamNameAndTypeAndIdNot(
                anyCollection(), eq("prod"), eq(STREAM_NAME), eq(100L), isNull()
        )).thenReturn(false);
        when(repository.existsByTopicsInAndEnvAndStreamNameAndTypeAndIdNot(
                anyCollection(), eq("staging"), eq(STREAM_NAME), eq(100L), isNull()
        )).thenReturn(false);
        when(repository.save(any())).thenAnswer(i -> {
            StreamingConfig arg = i.getArgument(0);
            return arg.toBuilder().id(TEST_ID).build();
        });

        service.create(config1);
        service.create(config2);

        verify(repository).existsByTopicsInAndEnvAndStreamNameAndTypeAndIdNot(
                config1.getTopics(), "prod", STREAM_NAME, 100L, null
        );
        verify(repository).existsByTopicsInAndEnvAndStreamNameAndTypeAndIdNot(
                config2.getTopics(), "staging", STREAM_NAME, 100L, null
        );
    }

    @Test
    void test_create_allowsSameTopics_forDifferentStreamName() {
        var config1 = streamingConfig(ENV, "stream1", Set.of("topic1"))
                .toBuilder().componentId(100L).build();
        var config2 = streamingConfig(ENV, "stream2", Set.of("topic1"))
                .toBuilder().componentId(100L).build();

        when(repository.existsByTopicsInAndEnvAndStreamNameAndTypeAndIdNot(
                anyCollection(), eq(ENV), eq("stream1"), eq(100L), isNull()
        )).thenReturn(false);
        when(repository.existsByTopicsInAndEnvAndStreamNameAndTypeAndIdNot(
                anyCollection(), eq(ENV), eq("stream2"), eq(100L), isNull()
        )).thenReturn(false);
        when(repository.save(any())).thenAnswer(i -> {
            StreamingConfig arg = i.getArgument(0);
            return arg.toBuilder().id(TEST_ID).build();
        });

        service.create(config1);
        service.create(config2);

        verify(repository).existsByTopicsInAndEnvAndStreamNameAndTypeAndIdNot(
                config1.getTopics(), ENV, "stream1", 100L, null
        );
        verify(repository).existsByTopicsInAndEnvAndStreamNameAndTypeAndIdNot(
                config2.getTopics(), ENV, "stream2", 100L, null
        );
    }
}
