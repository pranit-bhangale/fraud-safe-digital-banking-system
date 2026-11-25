package com.banking.fraud.client;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic, reusable client for running Lua scripts on Redis.
 */
@Component
@RequiredArgsConstructor
public class RedisLuaClient {

    private static final String SCRIPT_DIR = "lua/";
    private static final String SCRIPT_EXT = ".lua";

    private final StringRedisTemplate redisTemplate;
    private final Map<String, RedisScript<?>> scriptCache = new ConcurrentHashMap<>();

    private static Object[] toStringArgs(Object[] args) {
        Object[] out = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            out[i] = String.valueOf(args[i]);
        }
        return out;
    }

    /**
     * Execute a script and return its result converted to the given type.
     */
    @SuppressWarnings("unchecked")
    public <T> T execute(String scriptName, Class<T> returnType, List<String> keys, Object... args) {
        RedisScript<T> script = (RedisScript<T>) this.scriptCache.computeIfAbsent(
                scriptName + "#" + returnType.getName(),
                k -> loadScript(scriptName, returnType));
        return this.redisTemplate.execute(script, keys, toStringArgs(args));
    }

    /**
     * Execute a script when you don't need a return value.
     */
    public void run(String scriptName, List<String> keys, Object... args) {
        execute(scriptName, Object.class, keys, args);
    }

    /**
     * Convenience: script that returns 1/0 as a boolean.
     */
    public boolean executeForBoolean(String scriptName, List<String> keys, Object... args) {
        Long result = execute(scriptName, Long.class, keys, args);
        return result != null && result == 1L;
    }

    /**
     * Convenience: script that returns a Lua table (array) of numbers.
     */
    @SuppressWarnings("unchecked")
    public List<Long> executeForLongList(String scriptName, List<String> keys, Object... args) {
        return (List<Long>) execute(scriptName, List.class, keys, args);
    }

    private <T> RedisScript<T> loadScript(String scriptName, Class<T> returnType) {
        DefaultRedisScript<T> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource
                (new ClassPathResource(SCRIPT_DIR + scriptName + SCRIPT_EXT)));
        script.setResultType(returnType);
        return script;
    }
}
