package com.banking.helpdesk.version;

import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.accept.ApiVersionParser;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApiVersioning implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer
                .useRequestHeader("X-API-Version")
                .setVersionParser(new VersionParser())
                .addSupportedVersions("v1")
                .setDefaultVersion("v1")
                .detectSupportedVersions(false);
    }

    @NullMarked
    private static final class VersionParser implements ApiVersionParser<Integer> {

        @Override
        public Integer parseVersion(String version) {
            if (!version.matches("v\\d+")) {
                throw new IllegalArgumentException("Invalid API version: " + version);
            }
            return Integer.parseInt(version.substring(1));
        }
    }
}