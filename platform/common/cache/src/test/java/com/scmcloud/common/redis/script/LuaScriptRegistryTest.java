package com.scmcloud.common.redis.script;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LuaScriptRegistryTest {

    private LuaScriptRegistry registry;
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        registry = new LuaScriptRegistry(redisTemplate);
    }

    @Test
    void shouldRegisterScript() {
        RedisScript<Long> script = new DefaultRedisScript<>("return 1", Long.class);

        registry.register("test:script", script, "Test script", "1.0.0");

        assertTrue(registry.isRegistered("test:script"));
        assertEquals(1, registry.getScriptCount());
    }

    @Test
    void shouldGetScriptByName() {
        RedisScript<Long> script = new DefaultRedisScript<>("return 1", Long.class);
        registry.register("test:script", script);

        RedisScript<Long> retrieved = registry.get("test:script", Long.class);

        assertNotNull(retrieved);
        assertEquals(script.getScriptAsString(), retrieved.getScriptAsString());
    }

    @Test
    void shouldThrowExceptionForUnknownScript() {
        assertThrows(IllegalArgumentException.class, () -> {
            registry.get("unknown:script", Long.class);
        });
    }

    @Test
    void shouldReturnAllScriptsMetadata() {
        RedisScript<Long> script1 = new DefaultRedisScript<>("return 1", Long.class);
        RedisScript<Long> script2 = new DefaultRedisScript<>("return 2", Long.class);

        registry.register("script:one", script1, "First script", "1.0.0");
        registry.register("script:two", script2, "Second script", "2.0.0");

        Map<String, LuaScriptRegistry.ScriptMetadata> allScripts = registry.getAllScripts();

        assertEquals(2, allScripts.size());
        assertTrue(allScripts.containsKey("script:one"));
        assertTrue(allScripts.containsKey("script:two"));
        assertEquals("First script", allScripts.get("script:one").description());
        assertEquals("2.0.0", allScripts.get("script:two").version());
    }

    @Test
    void shouldReturnScriptNames() {
        RedisScript<Long> script = new DefaultRedisScript<>("return 1", Long.class);
        registry.register("test:script", script);

        Set<String> names = registry.getRegisteredNames();

        assertEquals(1, names.size());
        assertTrue(names.contains("test:script"));
    }

    @Test
    void shouldReturnMetadata() {
        RedisScript<Long> script = new DefaultRedisScript<>("return 1", Long.class);
        registry.register("test:script", script, "Test description", "1.2.3");

        LuaScriptRegistry.ScriptMetadata metadata = registry.getMetadata("test:script");

        assertNotNull(metadata);
        assertEquals("Test description", metadata.description());
        assertEquals("1.2.3", metadata.version());
        assertNotNull(metadata.sha1());
    }

    @Test
    void shouldReturnNullMetadataForUnknownScript() {
        LuaScriptRegistry.ScriptMetadata metadata = registry.getMetadata("unknown:script");
        assertNull(metadata);
    }

    @Test
    void shouldGetRawScript() {
        RedisScript<Long> script = new DefaultRedisScript<>("return 1", Long.class);
        registry.register("test:script", script);

        RedisScript<?> raw = registry.getRaw("test:script");

        assertNotNull(raw);
    }

    @Test
    void shouldReturnNullForUnknownRawScript() {
        RedisScript<?> raw = registry.getRaw("unknown:script");
        assertNull(raw);
    }

    @Test
    void shouldTrackPreloadStatus() {
        RedisScript<Long> script = new DefaultRedisScript<>("return 1", Long.class);
        registry.register("test:script", script);

        // Before preloading
        assertEquals(0, registry.getPreloadedCount());
        assertFalse(registry.isPreloaded("test:script"));
        assertNull(registry.getPreloadedSha("test:script"));
    }
}
