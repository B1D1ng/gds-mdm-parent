package com.ebay.behavior.gds.mdm.signal.common;

import com.ebay.behavior.gds.mdm.common.annotation.ModuleLevelConfiguration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@ModuleLevelConfiguration
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, ElasticsearchRestClientAutoConfiguration.class},
        scanBasePackages = "com.ebay.behavior.gds.mdm")
@SuppressWarnings("PMD.UseUtilityClass")
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}