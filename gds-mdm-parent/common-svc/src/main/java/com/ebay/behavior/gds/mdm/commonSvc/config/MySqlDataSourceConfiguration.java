package com.ebay.behavior.gds.mdm.commonSvc.config;

import com.ebay.fount.managedfountclient.ManagedFountClientBuilder;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.validation.constraints.NotBlank;
import javax.sql.DataSource;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;

@Profile("!IT")
@Slf4j
@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "datasource")
public class MySqlDataSourceConfiguration {

    @NotBlank
    private String env; // see valid values under com.ebay.fount.fountclient.util.EndPointUtil

    @NotBlank
    private String appName;

    @NotBlank
    private String logicalHost;

    @Bean
    @Primary
    public DataSource mysqlDataSource() {
        val fountClient = new ManagedFountClientBuilder()
                .plaintextPasswords(true)
                .immutable(false)
                .dbEnv(this.env)
                .appName(this.appName)
                .logicalDsNames(this.logicalHost)
                .build();

        val dsConfig = fountClient.getDatasourceConfig(logicalHost);
        val dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(dsConfig.getUrl());
        dataSource.setUsername(dsConfig.getUser());
        dataSource.setPassword(dsConfig.getPassword());

        log.info("Datasource created for logical host: {}, env: {}", logicalHost, env);
        return dataSource;
    }
}
