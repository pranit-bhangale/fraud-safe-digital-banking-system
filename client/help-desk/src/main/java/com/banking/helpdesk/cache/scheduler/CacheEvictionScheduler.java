package com.pranit.praya.cache.scheduler;

import com.pranit.praya.cache.config.CacheConfig;
import com.pranit.praya.cache.service.AgentCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled background tasks for automated cache eviction across AI answer and tool knowledge caches.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheEvictionScheduler {

    private final AgentCacheService agentCacheService;
    private final CacheManager cacheManager;

    /**
     * Periodically evicts all cached AI answers every 6 hours.
     */
    @Scheduled(cron = "0 0 */6 * * *")
    public void scheduleAnswerCacheEviction() {
        final long startTime = System.currentTimeMillis();
        log.info("[SCHEDULER] [CACHE-EVICT] Starting scheduled eviction of AI agent response cache");
        this.agentCacheService.evictAllAnswers();
        final long durationMs = System.currentTimeMillis() - startTime;
        log.info("[SCHEDULER] [CACHE-EVICT] AI agent response cache successfully cleared (durationMs: {})", durationMs);
    }

    /**
     * Evicts portfolio and project tool knowledge caches every 24 hours at midnight.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void scheduleKnowledgeRefreshAndEviction() {
        final long startTime = System.currentTimeMillis();
        log.info("[SCHEDULER] [DATA-REFRESH] Starting scheduled eviction of tool knowledge caches");
        evictCache(CacheConfig.CACHE_PORTFOLIO_KNOWLEDGE);
        evictCache(CacheConfig.CACHE_PORTFOLIO_PROJECTS);
        final long durationMs = System.currentTimeMillis() - startTime;
        log.info("[SCHEDULER] [DATA-REFRESH] Tool knowledge caches successfully evicted (durationMs: {})", durationMs);
    }

    private void evictCache(String cacheName) {
        Cache cache = this.cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.debug("[SCHEDULER] [CACHE-CLEARED] Cleared cache: {}", cacheName);
        }
    }
}
