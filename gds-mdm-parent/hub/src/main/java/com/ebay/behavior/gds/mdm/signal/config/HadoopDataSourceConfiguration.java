package com.ebay.behavior.gds.mdm.signal.config;

import com.ebay.behavior.gds.mdm.common.service.FideliusService;
import com.ebay.hadoop.kite2.client.KiteClientFactory;

import com.sun.security.auth.module.UnixSystem;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.security.UserGroupInformation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;

import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.HADOOP_SECURITY_AUTHENTICATION;

@Slf4j
@Getter
@Setter
@Validated
@Profile({"!IT", "!Dev"})
@org.springframework.context.annotation.Configuration
@ConditionalOnProperty(prefix = "hadoop", name = "enable", havingValue = "true", matchIfMissing = false) // need to mark these beans optional?
public class HadoopDataSourceConfiguration {

    @Autowired
    private FideliusService fideliusService;

    @Autowired
    @Lazy
    private UserGroupInformation userGroupInformation;

    @Autowired
    private HadoopConfiguration hadoopConfiguration;

    @Value("${hadoop.config.core}")
    private Resource hadoopConfigCore;

    @Value("${hadoop.config.hdfs}")
    private Resource hadoopConfigHdfs;

    @Bean
    @ConditionalOnExpression("#{!T(com.ebay.behavior.gds.mdm.common.util.DevZoneUtils).isDevZone()}")
    public HikariDataSource hiveDataSource() {
        log.info("Creating Hive data source");
        startKiteClient(hadoopConfiguration.getKite());

        return new HikariDataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                try {
                    return createJdbcConnection();
                } catch (Exception ex) {
                    log.error("Failed to create Hive JDBC connection", ex);
                    throw new SQLException("Failed to create Hive JDBC connection", ex);
                }
            }
        };
    }

    private void startKiteClient(HadoopConfiguration.KiteConfig kiteConfig) {
        Properties config = new Properties();
        config.setProperty("kite.server.endpoint", kiteConfig.getServerEndpoint());
        config.setProperty("keystone.api.key", fideliusService.getSecret(kiteConfig.getKeystoneApiKeyPath()));
        config.setProperty("keystone.api.secret", fideliusService.getSecret(kiteConfig.getKeystoneApiSecretPath()));
        config.setProperty("kite.krb5.conf.location", kiteConfig.getKrb5ConfLocation());
        config.setProperty("kite.user.principal", kiteConfig.getUserPrincipal());

        var client = KiteClientFactory.create(config);
        try {
            log.info("Starting Kite client");
            client.start().get();
        } catch (Exception ex) {
            log.error("Failed to start Kite client", ex);
            client.stop();
            throw new IllegalStateException("Failed to start Kite client", ex);
        }
    }

    @Bean
    @Lazy
    public Configuration hadoopConfigBean() throws IOException {
        val configuration = new Configuration();
        configuration.clear();
        try (var in = hadoopConfigCore.getInputStream()) {
            configuration.addResource(in, hadoopConfigCore.getFilename());
        }
        try (var in = hadoopConfigHdfs.getInputStream()) {
            configuration.addResource(in, hadoopConfigHdfs.getFilename());
        }
        configuration.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
        configuration.set(HADOOP_SECURITY_AUTHENTICATION, "kerberos");

        return configuration;
    }

    @Bean
    @Lazy
    public UserGroupInformation userGroupInformation(Configuration configuration) throws IOException {
        UserGroupInformation.setConfiguration(configuration);
        val location = hadoopConfiguration.getHive().getKrb5TicketCacheLocationPrefix() + new UnixSystem().getUid();
        return UserGroupInformation.getUGIFromTicketCache(location, hadoopConfiguration.getKite().getUserPrincipal());
    }

    @Bean
    @Lazy
    public FileSystem hdfsFileSystem(Configuration configuration) throws IOException {
        return FileSystem.get(configuration);
    }

    private Connection createJdbcConnection() throws Exception {
        UserGroupInformation.setLoginUser(userGroupInformation(hadoopConfigBean()));

        Class.forName(hadoopConfiguration.getHive().getDriverName());
        return userGroupInformation(hadoopConfigBean())
                .doAs((PrivilegedExceptionAction<Connection>) () -> DriverManager.getConnection(hadoopConfiguration.getHive().getCarmelUrl()));
    }
}
