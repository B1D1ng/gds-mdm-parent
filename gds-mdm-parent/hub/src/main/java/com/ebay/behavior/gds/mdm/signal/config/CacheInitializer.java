package com.ebay.behavior.gds.mdm.signal.config;

import com.ebay.behavior.gds.mdm.common.service.InfohubService;
import com.ebay.raptor.kernel.lifecycle.RaptorWarmupContainer;
import com.ebay.raptor.kernel.lifecycle.WarmupHandler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.runAsync;

@Slf4j
@Configuration
@RaptorWarmupContainer
@Profile("!IT")
public class CacheInitializer {

    @Autowired
    public InfohubService infohub;

    @WarmupHandler
    public void loadInfohubCache() {
        log.info("Loading caches...");
        allOf(runAsync(infohub::readAllProjects), runAsync(infohub::readAllUsers)).join();
        log.info("Loading caches DONE.");
    }

    @Scheduled(cron = "#{cacheConfiguration.reloadCron}")
    public void scheduledReload() {
        try {
            log.debug("Reloading caches...");
            allOf(runAsync(infohub::readAllProjects), runAsync(infohub::readAllUsers)).join();
            log.debug("Reloading caches DONE.");
        } catch (Exception ex) {
            log.warn("Reloading caches failed.", ex); // might fail in rare cases auth token still not refreshed
        }
    }
}