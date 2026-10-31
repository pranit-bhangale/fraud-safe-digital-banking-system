package com.banking.utility.configuration;

import com.banking.utility.aspect.LoggingAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Auto-configuration for the banking utility library.
 * <p>
 * Spring Boot discovers this class automatically via
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}.
 * When the library is on the classpath, the {@link LoggingAspect} bean is created
 * automatically - no configuration or properties are required.
 */
@Configuration(proxyBeanMethods = false)
@EnableAspectJAutoProxy
@ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
public class BeanConfiguration {

    /**
     * Default constructor for {@link BeanConfiguration}.
     */
    public BeanConfiguration() {
    }

    /**
     * Creates and registers the {@link LoggingAspect} bean for automatic execution logging.
     *
     * @return a new {@link LoggingAspect} instance
     */
    @Bean
    public LoggingAspect loggingAspect() {
        return new LoggingAspect();
    }
}
