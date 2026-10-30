package com.banking.security.config;

import com.banking.security.filter.CorrelationIdFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration(proxyBeanMethods = false)
public class CorrelationFilterConfig {

    @Bean
    @ConditionalOnMissingBean(CorrelationIdFilter.class)
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "correlationIdFilterBean")
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterBean(final CorrelationIdFilter filter) {
        final var bean = new FilterRegistrationBean<>(filter);
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
