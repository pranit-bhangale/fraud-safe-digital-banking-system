package com.banking.utility.version.config;

import com.banking.utility.version.service.VersionParser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * API versioning configuration for Spring MVC applications.
 * <p>
 * Automatically configures the {@code X-API-Version} request header
 * for resolving API versions using {@link VersionParser}.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(WebMvcConfigurer.class)
public class VersionConfig implements WebMvcConfigurer {

    /**
     * Default constructor for {@link VersionConfig}.
     */
    public VersionConfig() {
    }

    @Override
    public void configureApiVersioning(final ApiVersionConfigurer configurer) {
        configurer
                .useRequestHeader("X-API-Version")
                .setVersionParser(new VersionParser())
                .addSupportedVersions("v1")
                .setDefaultVersion("v1")
                .detectSupportedVersions(false);
    }
}
