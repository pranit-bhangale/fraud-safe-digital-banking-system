package com.pranit.praya.cache.config;

import com.pranit.praya.cache.spring.AtomicReferenceCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Duration;

/**
 * Spring Cache and Scheduling Configuration enabling annotation-based caching
 * and scheduled eviction/cleanup.
 */
@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig {

    public static final String CACHE_AGENT_RESPONSES = "agentResponses";
    public static final String CACHE_PORTFOLIO_KNOWLEDGE = "portfolioKnowledge";
    public static final String CACHE_PORTFOLIO_PROJECTS = "portfolioProjects";

    @Bean
    @Primary
    public CacheManager cacheManager() {
        return new AtomicReferenceCacheManager(1000, Duration.ofDays(1));
    }
}
