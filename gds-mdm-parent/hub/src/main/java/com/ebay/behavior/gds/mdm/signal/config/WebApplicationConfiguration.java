package com.ebay.behavior.gds.mdm.signal.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.ext.Provider;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.util.ResourceUtils.APP_PATH;

@Configuration
@ApplicationPath(APP_PATH)
@SecurityScheme(name = "app_scope_auth", scheme = "bearer", type = SecuritySchemeType.HTTP, in = SecuritySchemeIn.HEADER)
public class WebApplicationConfiguration extends Application {

    private final Feature etsFeature;

    @Autowired
    private ApplicationContext context;

    @Autowired
    public WebApplicationConfiguration(@Qualifier("ets-feature") Feature etsFeature) {
        this.etsFeature = etsFeature;
    }

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> providers = new LinkedHashSet<>();

        context.getBeansWithAnnotation(RestController.class).values()
                .forEach(bean -> providers.add(AopUtils.getTargetClass(bean)));

        context.getBeansWithAnnotation(Provider.class).values()
                .forEach(bean -> providers.add(AopUtils.getTargetClass(bean)));

        return providers;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> providers = new LinkedHashSet<>();
        providers.add(etsFeature);
        return providers;
    }
}
