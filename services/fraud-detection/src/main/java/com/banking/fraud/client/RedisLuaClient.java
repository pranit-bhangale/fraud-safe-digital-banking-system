package com.banking.fraud.client;

import com.banking.fraud.exception.RedisOperationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic, reusable, cluster-safe client for executing Redis Lua scripts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLuaClient {

    private static final String SCRIPT_DIR = "lua/";
    private static final String SCRIPT_EXT = ".lua";

    private final StringRedisTemplate redisTemplate;
    private final Map<String, RedisScript<?>> scriptCache = new ConcurrentHashMap<>();

    /**
     * Executes a script without expecting a return value.
     */
    public void run(String scriptName, List<String> keys, Object... args) {
        execute(scriptName, Object.class, keys, args);
    }

    /**
     * Executes a Lua script and maps its result to the specified return type.
     */
    @SuppressWarnings("unchecked")
    public <T> T execute(String scriptName, Class<T> returnType, List<String> keys, Object... args) {
        try {
            RedisScript<T> script = (RedisScript<T>) this.scriptCache.computeIfAbsent(
                    scriptName + "#" + returnType.getName(),
                    k -> loadScript(scriptName, returnType));
            return this.redisTemplate.execute(script, keys, toStringArgs(args));
        } catch (Exception ex) {
            log.error("Failed to execute Lua script '{}' with keys {}: {}", scriptName, keys, ex.getMessage());
            throw new RedisOperationException("Error executing Lua script: " + scriptName, ex);
        }
    }

    private <T> RedisScript<T> loadScript(String scriptName, Class<T> returnType) {
        DefaultRedisScript<T> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource(SCRIPT_DIR + scriptName + SCRIPT_EXT)));
        script.setResultType(returnType);
        return script;
    }

    private static Object[] toStringArgs(Object[] args) {
        if (args == null || args.length == 0) return new Object[0];
        Object[] out = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            out[i] = String.valueOf(args[i]);
        }
        return out;
    }

    /**
     * Convenience method for scripts returning a boolean flag (1 or 0).
     */
    public boolean executeForBoolean(String scriptName, List<String> keys, Object... args) {
        Long result = execute(scriptName, Long.class, keys, args);
        return result != null && result == 1L;
    }

    /**
     * Convenience method for scripts returning a list of numeric flags.
     */
    @SuppressWarnings("unchecked")
    public List<Long> executeForLongList(String scriptName, List<String> keys, Object... args) {
        List<Long> result = (List<Long>) execute(scriptName, List.class, keys, args);
        return result != null ? result : Collections.emptyList();
    }
}