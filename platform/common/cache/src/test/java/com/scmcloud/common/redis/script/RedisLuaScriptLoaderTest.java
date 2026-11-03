package com.scmcloud.common.redis.script;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.script.RedisScript;

import static org.junit.jupiter.api.Assertions.*;

class RedisLuaScriptLoaderTest {

    @BeforeEach
    void setUp() {
        RedisLuaScriptLoader.clearCache();
    }

    @Test
    void shouldLoadLuaScriptFromClasspath() {
        RedisScript<Long> script = RedisLuaScriptLoader.load(
                "lua/inventory/deduct_stock.lua", Long.class);

        assertNotNull(script);
        assertNotNull(script.getScriptAsString());
        assertTrue(script.getScriptAsString().contains("redis.call"));
    }

    @Test
    void shouldCacheLoadedScript() {
        RedisScript<Long> script1 = RedisLuaScriptLoader.load(
                "lua/inventory/deduct_stock.lua", Long.class);
        RedisScript<Long> script2 = RedisLuaScriptLoader.load(
                "lua/inventory/deduct_stock.lua", Long.class);

        assertSame(script1, script2);
    }

    @Test
    void shouldLoadDifferentResultTypes() {
        RedisScript<Long> longScript = RedisLuaScriptLoader.load(
                "lua/inventory/deduct_stock.lua", Long.class);
        RedisScript<String> stringScript = RedisLuaScriptLoader.load(
                "lua/inventory/deduct_stock.lua", String.class);

        assertNotNull(longScript);
        assertNotNull(stringScript);
        assertNotSame(longScript, stringScript);
    }

    @Test
    void shouldThrowExceptionForMissingScript() {
        assertThrows(IllegalArgumentException.class, () -> {
            RedisLuaScriptLoader.load("lua/nonexistent.lua", Long.class);
        });
    }

    @Test
    void shouldLoadWithoutCache() {
        RedisScript<Long> script1 = RedisLuaScriptLoader.loadNoCache(
                "lua/inventory/deduct_stock.lua", Long.class);
        RedisScript<Long> script2 = RedisLuaScriptLoader.loadNoCache(
                "lua/inventory/deduct_stock.lua", Long.class);

        assertNotNull(script1);
        assertNotNull(script2);
        assertNotSame(script1, script2);
    }

    @Test
    void shouldClearCache() {
        RedisLuaScriptLoader.load("lua/inventory/deduct_stock.lua", Long.class);
        RedisLuaScriptLoader.clearCache();

        // After clearing, loading should create new instance
        RedisScript<Long> script = RedisLuaScriptLoader.load(
                "lua/inventory/deduct_stock.lua", Long.class);
        assertNotNull(script);
    }
}
