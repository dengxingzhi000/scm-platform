package com.scmcloud.system.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.scmcloud.common.tenant.TenantContextHolder;
import com.scmcloud.system.domain.entity.SysStatusDict;
import com.scmcloud.system.domain.entity.SysStatusTransition;
import com.scmcloud.system.mapper.SysStatusDictMapper;
import com.scmcloud.system.mapper.SysStatusTransitionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 状态字典三级缓存管理器。
 *
 * <p>L1: Caffeine (JVM 内存, 5分钟 TTL)
 * <p>L2: Redis (30分钟 TTL, key: status:dict:{tenantId}:{bizType})
 * <p>L3: PostgreSQL (DB)
 *
 * <p>访问链路: Caffeine → Redis → DB → 回填
 *
 * <p>缓存策略:
 * <ul>
 *   <li>启动预热: 只加载 global (tenant_id=NULL) 数据</li>
 *   <li>运行时: 按租户+业务类型懒加载</li>
 *   <li>变更时: 事件驱动刷新 (Kafka)</li>
 * </ul>
 */
@Slf4j
@Component
public class StatusDictCacheManager {

    private final SysStatusDictMapper statusDictMapper;
    private final SysStatusTransitionMapper transitionMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // L1: Caffeine 本地缓存
    // key = "tenantId:bizType", value = 状态字典列表
    private final Cache<String, List<SysStatusDict>> dictCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(5))
            .recordStats()
            .build();

    // key = "tenantId:bizType", value = 流转规则列表
    private final Cache<String, List<SysStatusTransition>> transitionCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(5))
            .recordStats()
            .build();

    // Redis key 前缀
    private static final String REDIS_DICT_PREFIX = "status:dict:";
    private static final String REDIS_TRANSITION_PREFIX = "status:transition:";
    private static final Duration REDIS_TTL = Duration.ofMinutes(30);

    public StatusDictCacheManager(SysStatusDictMapper statusDictMapper,
                                   SysStatusTransitionMapper transitionMapper,
                                   StringRedisTemplate redisTemplate,
                                   ObjectMapper objectMapper) {
        this.statusDictMapper = statusDictMapper;
        this.transitionMapper = transitionMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    // ─── 状态字典缓存 ─────────────────────────────────────────

    /**
     * 获取状态字典列表 (三级缓存)
     */
    public List<SysStatusDict> getStatusDict(String bizType) {
        String tenantId = resolveTenantId();
        String cacheKey = tenantId + ":" + bizType;

        // L1: Caffeine
        List<SysStatusDict> result = dictCache.getIfPresent(cacheKey);
        if (result != null) {
            log.trace("Cache HIT L1: status:dict:{}", (cacheKey + "").replace("\r", "_").replace("\n", "_"));
            return result;
        }

        // L2: Redis
        result = loadDictFromRedis(tenantId, bizType);
        if (result != null) {
            dictCache.put(cacheKey, result);
            log.trace("Cache HIT L2 (Redis): status:dict:{}", (cacheKey + "").replace("\r", "_").replace("\n", "_"));
            return result;
        }

        // L3: DB → 回填
        result = loadDictFromDb(tenantId, bizType);
        saveDictToRedis(tenantId, bizType, result);
        dictCache.put(cacheKey, result);
        log.debug("Cache MISS → DB loaded: status:dict:{}, size={}", (cacheKey + "").replace("\r", "_").replace("\n", "_"), (result.size() + "").replace("\r", "_").replace("\n", "_"));
        return result;
    }

    /**
     * 获取单个状态定义
     */
    public SysStatusDict getStatusByCode(String bizType, String statusCode) {
        return getStatusDict(bizType).stream()
                .filter(s -> s.getStatusCode().equals(statusCode))
                .findFirst()
                .orElse(null);
    }

    // ─── 流转规则缓存 ─────────────────────────────────────────

    /**
     * 获取流转规则列表 (三级缓存)
     */
    public List<SysStatusTransition> getTransitions(String bizType) {
        String tenantId = resolveTenantId();
        String cacheKey = tenantId + ":" + bizType;

        // L1: Caffeine
        List<SysStatusTransition> result = transitionCache.getIfPresent(cacheKey);
        if (result != null) {
            log.trace("Cache HIT L1: status:transition:{}", (cacheKey + "").replace("\r", "_").replace("\n", "_"));
            return result;
        }

        // L2: Redis
        result = loadTransitionFromRedis(tenantId, bizType);
        if (result != null) {
            transitionCache.put(cacheKey, result);
            log.trace("Cache HIT L2 (Redis): status:transition:{}", (cacheKey + "").replace("\r", "_").replace("\n", "_"));
            return result;
        }

        // L3: DB → 回填
        result = loadTransitionFromDb(tenantId, bizType);
        saveTransitionToRedis(tenantId, bizType, result);
        transitionCache.put(cacheKey, result);
        log.debug("Cache MISS → DB loaded: status:transition:{}, size={}", (cacheKey + "").replace("\r", "_").replace("\n", "_"), (result.size() + "").replace("\r", "_").replace("\n", "_"));
        return result;
    }

    /**
     * 获取从某个状态出发的流转规则
     */
    public List<SysStatusTransition> getTransitionsFrom(String bizType, String fromStatus) {
        return getTransitions(bizType).stream()
                .filter(t -> t.getFromStatus().equals(fromStatus))
                .toList();
    }

    // ─── 缓存刷新 ─────────────────────────────────────────

    /**
     * 刷新指定租户+业务类型的缓存 (DB → Redis → Caffeine)
     */
    public void refresh(String tenantId, String bizType) {
        String cacheKey = tenantId + ":" + bizType;

        // 重新加载
        List<SysStatusDict> dicts = loadDictFromDb(tenantId, bizType);
        List<SysStatusTransition> transitions = loadTransitionFromDb(tenantId, bizType);

        // 写入 Redis
        saveDictToRedis(tenantId, bizType, dicts);
        saveTransitionToRedis(tenantId, bizType, transitions);

        // 写入 Caffeine
        dictCache.put(cacheKey, dicts);
        transitionCache.put(cacheKey, transitions);

        log.info("Cache refreshed: tenantId={}, bizType={}, dicts={}, transitions={}",
                tenantId, bizType, dicts.size(), transitions.size());
    }

    /**
     * 刷新全局缓存 (tenant_id=NULL)
     */
    public void refreshGlobal(String bizType) {
        refresh("global", bizType);
    }

    /**
     * 清除所有缓存 (L1 + L2)
     */
    public void evictAll() {
        dictCache.invalidateAll();
        transitionCache.invalidateAll();
        // 注意: 不清除 Redis，让 TTL 自然过期
        log.info("L1 cache evicted all");
    }

    /**
     * 清除指定租户+业务类型的缓存
     */
    public void evict(String tenantId, String bizType) {
        String cacheKey = tenantId + ":" + bizType;
        dictCache.invalidate(cacheKey);
        transitionCache.invalidate(cacheKey);
        log.debug("L1 cache evicted: {}", cacheKey);
    }

    // ─── 启动预热 ─────────────────────────────────────────

    /**
     * 启动预热: 只加载 global (tenant_id=NULL) 数据到 L1 + L2
     * 调用时机: ApplicationReadyEvent
     */
    public void prewarmGlobal() {
        log.info("[CacheWarming] Pre-warming global status dict...");
        long start = System.currentTimeMillis();

        List<String> bizTypes = List.of("ORDER", "PURCHASE", "PURCHASE_REQUEST",
                "INBOUND", "OUTBOUND", "APPROVAL", "LOGISTICS");

        for (String bizType : bizTypes) {
            try {
                refresh("global", bizType);
            } catch (Exception e) {
                log.warn("[CacheWarming] Failed to pre-warm {}: {}", bizType, e.getMessage());
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("[CacheWarming] Global status dict pre-warmed in {}ms", elapsed);
    }

    // ─── 内部方法 ─────────────────────────────────────────

    private String resolveTenantId() {
        var tenantId = TenantContextHolder.getTenantId();
        return tenantId != null ? tenantId.toString() : "global";
    }

    private List<SysStatusDict> loadDictFromDb(String tenantId, String bizType) {
        LambdaQueryWrapper<SysStatusDict> wrapper = new LambdaQueryWrapper<SysStatusDict>()
                .eq(SysStatusDict::getBizType, bizType)
                .eq(SysStatusDict::getDeleted, false)
                .and(w -> w.isNull(SysStatusDict::getTenantId)
                        .or()
                        .eq(SysStatusDict::getTenantId, parseUuid(tenantId)))
                .orderByAsc(SysStatusDict::getSortOrder);
        return statusDictMapper.selectList(wrapper);
    }

    private List<SysStatusTransition> loadTransitionFromDb(String tenantId, String bizType) {
        LambdaQueryWrapper<SysStatusTransition> wrapper = new LambdaQueryWrapper<SysStatusTransition>()
                .eq(SysStatusTransition::getBizType, bizType)
                .eq(SysStatusTransition::getDeleted, false)
                .and(w -> w.isNull(SysStatusTransition::getTenantId)
                        .or()
                        .eq(SysStatusTransition::getTenantId, parseUuid(tenantId)))
                .orderByAsc(SysStatusTransition::getSortOrder);
        return transitionMapper.selectList(wrapper);
    }

    // ─── Redis 操作 ─────────────────────────────────────────

    private List<SysStatusDict> loadDictFromRedis(String tenantId, String bizType) {
        try {
            String key = REDIS_DICT_PREFIX + tenantId + ":" + bizType;
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) return null;
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Redis read failed for status:dict:{}:{}", (tenantId + "").replace("\r", "_").replace("\n", "_"), (bizType + "").replace("\r", "_").replace("\n", "_"), e);
            return null;
        }
    }

    private void saveDictToRedis(String tenantId, String bizType, List<SysStatusDict> data) {
        try {
            String key = REDIS_DICT_PREFIX + tenantId + ":" + bizType;
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(key, json, REDIS_TTL);
        } catch (Exception e) {
            log.warn("Redis write failed for status:dict:{}:{}", (tenantId + "").replace("\r", "_").replace("\n", "_"), (bizType + "").replace("\r", "_").replace("\n", "_"), e);
        }
    }

    private List<SysStatusTransition> loadTransitionFromRedis(String tenantId, String bizType) {
        try {
            String key = REDIS_TRANSITION_PREFIX + tenantId + ":" + bizType;
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) return null;
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Redis read failed for status:transition:{}:{}", (tenantId + "").replace("\r", "_").replace("\n", "_"), (bizType + "").replace("\r", "_").replace("\n", "_"), e);
            return null;
        }
    }

    private void saveTransitionToRedis(String tenantId, String bizType, List<SysStatusTransition> data) {
        try {
            String key = REDIS_TRANSITION_PREFIX + tenantId + ":" + bizType;
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(key, json, REDIS_TTL);
        } catch (Exception e) {
            log.warn("Redis write failed for status:transition:{}:{}", (tenantId + "").replace("\r", "_").replace("\n", "_"), (bizType + "").replace("\r", "_").replace("\n", "_"), e);
        }
    }

    private java.util.UUID parseUuid(String str) {
        if (str == null || "global".equals(str)) return null;
        try { return java.util.UUID.fromString(str); } catch (Exception e) { return null; }
    }
}
