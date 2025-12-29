package com.ebay.behavior.gds.mdm.common.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestSpringConfig {

    @Bean
    public FideliusService fideliusService() {
        return new FideliusService();
    }

    @Bean
    public InfohubService infohubService() {
        return new InfohubService();
    }
}
