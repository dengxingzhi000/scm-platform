package com.scmcloud.common.cache.metrics;

import com.scmcloud.common.redis.script.LuaScriptRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Actuator endpoint for Redis Lua script management.
 *
 * <p>Provides visibility into registered scripts, their versions, and preload status.</p>
 *
 * <p>Access paths:</p>
 * <ul>
 *   <li>{@code GET /actuator/luascripts} - List all scripts</li>
 *   <li>{@code GET /actuator/luascripts/{name}} - Get script details</li>
 * </ul>
 *
 * @author SCM Platform
 * @since 2026-06-29
 */
@Component
@Endpoint(id = "luascripts")
@RequiredArgsConstructor
public class LuaScriptEndpoint {

    private final LuaScriptRegistry registry;

    /**
     * List all registered Lua scripts with metadata.
     *
     * <p>GET /actuator/luascripts</p>
     */
    @ReadOperation
    public Map<String, Object> scripts() {
        Map<String, Object> result = new HashMap<>();
        result.put("total", registry.getScriptCount());
        result.put("preloaded", registry.getPreloadedCount());
        result.put("preloadFailed", registry.getPreloadFailedCount());
        result.put("scripts", registry.getAllScripts());
        return result;
    }

    /**
     * Get details of a specific Lua script.
     *
     * <p>GET /actuator/luascripts/{name}</p>
     *
     * @param name script name (e.g. "inventory:deduct_stock")
     */
    @ReadOperation
    public Map<String, Object> script(@Selector String name) {
        Map<String, Object> result = new HashMap<>();

        LuaScriptRegistry.ScriptMetadata meta = registry.getMetadata(name);
        if (meta == null) {
            result.put("error", "Script not found: " + name);
            return result;
        }

        result.put("name", name);
        result.put("description", meta.description());
        result.put("version", meta.version());
        result.put("sha1", meta.sha1());
        result.put("preloaded", registry.isPreloaded(name));

        String preloadedSha = registry.getPreloadedSha(name);
        if (preloadedSha != null) {
            result.put("redisSha", preloadedSha);
        }

        return result;
    }
}
