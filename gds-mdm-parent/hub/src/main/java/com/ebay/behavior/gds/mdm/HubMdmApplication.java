package com.ebay.behavior.gds.mdm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableRetry
@EnableCaching
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.ebay.behavior.gds.mdm")
@EnableJpaRepositories(basePackages = {
        "com.ebay.behavior.gds.mdm.commonSvc.repository",
        "com.ebay.behavior.gds.mdm.signal.repository",
        "com.ebay.behavior.gds.mdm.contract.repository",
        "com.ebay.behavior.gds.mdm.dec.repository",
        "com.ebay.behavior.gds.mdm.udf.repository"
})
@SuppressWarnings("PMD.UseUtilityClass")
public class HubMdmApplication {
    public static void main(String[] args) {
        SpringApplication.run(HubMdmApplication.class, args);
    }
}
