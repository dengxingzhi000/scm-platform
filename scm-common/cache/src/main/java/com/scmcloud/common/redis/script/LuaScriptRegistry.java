package com.scmcloud.common.redis.script;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Centralized registry for Redis Lua scripts.
 * <p>
 * Provides script lookup by name, metadata tracking, and startup preloading via EVALSHA.
 * Scripts are loaded from classpath {@code lua/**//*.lua} and registered with a logical name.
 * <p>
 * Usage:
 * <pre>
 * // Load and register at startup
 * luaScriptRegistry.register("auth:check_and_lock",
 *     RedisLuaScriptLoader.load("lua/auth/check_and_lock.lua", Long.class),
 *     "Check lock status atomically", "1.0.0");
 *
 * // Lookup by name
 * RedisScript&lt;Long&gt; script = luaScriptRegistry.get("auth:check_and_lock", Long.class);
 *
 * // Execute
 * redisTemplate.execute(script, keys, args);
 * </pre>
 *
 * @author Deng
 * @since 2025-11-27
 */
@Slf4j
@Component
public class LuaScriptRegistry {
    private static final String LUA_PATTERN = "classpath*:lua/**/*.lua";

    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<String, RedisScript<?>> scripts = new ConcurrentHashMap<>();
    private final Map<String, ScriptMetadata> metadata = new ConcurrentHashMap<>();
    private final Map<String, String> preloadedShas = new ConcurrentHashMap<>();
    private final AtomicInteger preloadSuccessCount = new AtomicInteger(0);
    private final AtomicInteger preloadFailedCount = new AtomicInteger(0);
    private static final RedisScript<String> SCRIPT_LOAD_WRAPPER =
            new DefaultRedisScript<>("return redis.call('SCRIPT', 'LOAD', ARGV[1])", String.class);

    public LuaScriptRegistry(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Register a script with metadata.
     *
     * @param name        unique script name (e.g. "auth:check_and_lock")
     * @param script      the RedisScript instance
     * @param description human-readable description
     * @param version     script version (e.g. "1.0.0")
     */
    public <T> void register(String name, RedisScript<T> script, String description, String version) {
        scripts.put(name, script);
        metadata.put(name, new ScriptMetadata(description, version, script.getSha1()));
        log.debug("Registered Lua script: {} v{}", name, version);
    }

    /**
     * Register a script (simplified).
     */
    public <T> void register(String name, RedisScript<T> script) {
        register(name, script, "", "1.0.0");
    }

    /**
     * Get a script by name and result type.
     *
     * @param name       script name
     * @param resultType expected return type
     * @return the RedisScript instance
     * @throws IllegalArgumentException if script not found
     */
    @SuppressWarnings("unchecked")
    public <T> RedisScript<T> get(String name, Class<T> resultType) {
        RedisScript<?> script = scripts.get(name);
        if (script == null) {
            throw new IllegalArgumentException("Lua script not found: " + name);
        }
        return (RedisScript<T>) script;
    }

    /**
     * Get a script by name (raw type, no casting).
     *
     * @param name script name
     * @return the RedisScript instance, or null if not found
     */
    public RedisScript<?> getRaw(String name) {
        return scripts.get(name);
    }

    /**
     * Check if a script is registered.
     */
    public boolean isRegistered(String name) {
        return scripts.containsKey(name);
    }

    /**
     * Get all registered script names.
     *
     * @return unmodifiable set of script names
     */
    public Set<String> getRegisteredNames() {
        return Collections.unmodifiableSet(scripts.keySet());
    }

    /**
     * Get metadata for a script.
     */
    public ScriptMetadata getMetadata(String name) {
        return metadata.get(name);
    }

    /**
     * Get all scripts with their metadata (for Actuator endpoint).
     *
     * @return unmodifiable map of script name to metadata
     */
    public Map<String, ScriptMetadata> getAllScripts() {
        return Collections.unmodifiableMap(metadata);
    }

    /**
     * Get total number of registered scripts.
     */
    public int getScriptCount() {
        return scripts.size();
    }

    /**
     * Get number of scripts successfully preloaded to Redis.
     */
    public int getPreloadedCount() {
        return preloadSuccessCount.get();
    }

    /**
     * Get number of scripts that failed to preload.
     */
    public int getPreloadFailedCount() {
        return preloadFailedCount.get();
    }

    /**
     * Check if a script has been successfully preloaded to Redis.
     *
     * @param name script name
     * @return true if preloaded
     */
    public boolean isPreloaded(String name) {
        return preloadedShas.containsKey(name);
    }

    /**
     * Get the Redis SHA1 for a preloaded script.
     *
     * @param name script name
     * @return SHA1 hash, or null if not preloaded
     */
    public String getPreloadedSha(String name) {
        return preloadedShas.get(name);
    }

    /**
     * Auto-scan classpath {@code lua/**//*.lua} files, register them, and preload to Redis.
     * Called at application startup.
     */
    @PostConstruct
    public void init() {
        scanAndRegister();
        preloadAll();
    }

    /**
     * Scan classpath for .lua files and register them.
     * Script name is derived from path: "lua/auth/check_and_lock.lua" → "auth:check_and_lock"
     */
    private void scanAndRegister() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(LUA_PATTERN);
            for (Resource resource : resources) {
                String path = resource.getFilename();
                if (path == null || !path.endsWith(".lua")) {
                    continue;
                }
                String scriptText = readResource(resource);
                String name = deriveName(resource);
                DefaultRedisScript<Long> script = new DefaultRedisScript<>(scriptText, Long.class);
                register(name, script);
            }
            log.info("Auto-scanned {} Lua scripts from classpath", scripts.size());
        } catch (IOException e) {
            log.warn("Failed to scan Lua scripts: {}", e.getMessage());
        }
    }

    /**
     * Derive script name from resource path.
     * "classpath*:lua/auth/check_and_lock.lua" → "auth:check_and_lock"
     */
    private String deriveName(Resource resource) {
        try {
            String url = resource.getURL().getPath();
            int luaIdx = url.indexOf("/lua/");
            if (luaIdx >= 0) {
                String relative = url.substring(luaIdx + 5);
                return relative.replace(".lua", "").replace('/', ':');
            }
        } catch (IOException e) {
            log.warn("Failed to derive name for resource: {}", resource);
        }
        String filename = resource.getFilename();
        return filename != null ? filename.replace(".lua", "") : "unknown";
    }

    /**
     * Preload all registered scripts to Redis for EVALSHA optimization.
     */
    private void preloadAll() {
        if (scripts.isEmpty()) {
            log.info("No Lua scripts registered for preloading");
            return;
        }

        preloadSuccessCount.set(0);
        preloadFailedCount.set(0);
        for (Map.Entry<String, RedisScript<?>> entry : scripts.entrySet()) {
            try {
                String sha = preloadScript(entry.getValue());
                if (sha != null) {
                    preloadedShas.put(entry.getKey(), sha);
                    preloadSuccessCount.incrementAndGet();
                } else {
                    preloadFailedCount.incrementAndGet();
                }
            } catch (Exception e) {
                log.warn("Failed to preload script {}: {}", entry.getKey(), e.getMessage());
                preloadFailedCount.incrementAndGet();
            }
        }
        log.info("Lua script preloading complete: {} success, {} failed",
                preloadSuccessCount.get(), preloadFailedCount.get());
    }

    /**
     * Preload a single script to Redis via SCRIPT LOAD.
     *
     * @return the SHA1 hash, or null on failure
     */
    private String preloadScript(RedisScript<?> script) {
        try {
            Object result = redisTemplate.execute(
                    SCRIPT_LOAD_WRAPPER,
                    java.util.Collections.emptyList(),
                    script.getScriptAsString()
            );
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            log.warn("Failed to preload Lua script SHA: {}", e.getMessage());
            return null;
        }
    }

    private static String readResource(Resource resource) {
        try (InputStreamReader reader = new InputStreamReader(
                resource.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read Lua script: " + resource, e);
        }
    }

    /**
     * Script metadata.
     */
    public record ScriptMetadata(String description, String version, String sha1) {
    }
}
