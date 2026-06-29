package com.scmcloud.common.redis.script;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Classpath-based Lua script loader with in-memory caching.
 * <p>
 * Loads {@code .lua} files from classpath and returns cached {@link RedisScript} instances.
 * Does NOT preload to Redis — use {@link LuaScriptRegistry} for that.
 * <p>
 * Usage:
 * <pre>
 * private static final RedisScript&lt;Long&gt; SCRIPT =
 *     RedisLuaScriptLoader.load("lua/auth/increment_and_expire.lua", Long.class);
 * </pre>
 *
 * @author Deng
 * @since 2025-11-27
 */
@Slf4j
public final class RedisLuaScriptLoader {

    private static final Map<String, RedisScript<?>> CACHE = new ConcurrentHashMap<>();

    private RedisLuaScriptLoader() {
    }

    /**
     * Load a Lua script from classpath and cache it.
     *
     * @param classpath  classpath location (e.g. "lua/auth/increment_and_expire.lua")
     * @param resultType expected return type (e.g. {@code Long.class})
     * @return cached RedisScript instance
     */
    @SuppressWarnings("unchecked")
    public static <T> RedisScript<T> load(String classpath, Class<T> resultType) {
        String key = classpath + ":" + resultType.getName();
        return (RedisScript<T>) CACHE.computeIfAbsent(key, k -> {
            String scriptText = readClasspath(classpath);
            DefaultRedisScript<T> script = new DefaultRedisScript<>(scriptText, resultType);
            log.debug("Loaded Lua script: {}", classpath);
            return script;
        });
    }

    /**
     * Load a Lua script without caching (for testing or one-off scripts).
     */
    public static <T> RedisScript<T> loadNoCache(String classpath, Class<T> resultType) {
        String scriptText = readClasspath(classpath);
        return new DefaultRedisScript<>(scriptText, resultType);
    }

    /**
     * Clear the script cache. Mainly for testing.
     */
    public static void clearCache() {
        CACHE.clear();
    }

    private static String readClasspath(String classpath) {
        Resource resource = new ClassPathResource(classpath);
        if (!resource.exists()) {
            throw new IllegalArgumentException("Lua script not found: " + classpath);
        }
        try (InputStreamReader reader = new InputStreamReader(
                resource.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read Lua script: " + classpath, e);
        }
    }
}
