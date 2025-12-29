package com.ebay.behavior.gds.mdm.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toMap;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "cache")
public class CacheConfiguration {

    public static final String AUTH_TOKEN_CACHE = "authToken";
    public static final String ES_AUTH_TOKEN_CACHE = "esAuthToken";
    public static final String JIRA_PROJECT_CACHE = "jiraProject";
    public static final String ALL_JIRA_PROJECTS_CACHE = "allJiraProjects";
    public static final String ALL_USERS_CACHE = "allUsers";

    @NotBlank
    private String reloadCron;

    @Valid
    @NotNull
    @Size(max = 100)
    private List<CacheConfig> caches;

    @PostConstruct
    protected void validate() {
        caches.forEach(CacheConfig::validate);
    }

    @Bean
    public CacheManager cacheManager() {
        val map = getCaches().stream()
                .collect(toMap(CacheConfig::getName, Function.identity()));
        val manager = new SimpleCacheManager();
        val caches = map.values().stream().map(this::buildCache).toList();
        manager.setCaches(caches);
        return manager;
    }

    private CaffeineCache buildCache(final CacheConfig config) {
        val builder = Caffeine.newBuilder()
                .initialCapacity(config.getInitialCapacity())
                .maximumSize(config.getMaxSize());

        if (Objects.nonNull(config.expireAfterWriteMinutes)) {
            builder.expireAfterWrite(config.getExpireAfterWriteMinutes(), MINUTES);
        } else {
            builder.expireAfterAccess(config.getExpireAfterAccessMinutes(), MINUTES);
        }

        return new CaffeineCache(config.getName(), builder.build());
    }

    @Getter
    @Setter
    public static class CacheConfig {
        @NotBlank
        private String name;

        private Integer expireAfterWriteMinutes;

        private Integer expireAfterAccessMinutes;

        @NotNull
        private Integer initialCapacity;

        @NotNull
        private Integer maxSize;

        public void validate() {
            if (Objects.isNull(expireAfterWriteMinutes) && Objects.isNull(expireAfterAccessMinutes)) {
                throw new IllegalStateException("expireAfterWriteMinutes or expireAfterAccessMinutes should not be null");
            }

            if (!Objects.isNull(expireAfterWriteMinutes) && !Objects.isNull(expireAfterAccessMinutes)) {
                throw new IllegalStateException("only one out of expireAfterWriteMinutes/expireAfterAccessMinutes should not be null");
            }
        }
    }
}
