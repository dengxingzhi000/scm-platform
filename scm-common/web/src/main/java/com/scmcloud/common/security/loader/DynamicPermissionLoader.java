package com.scmcloud.common.security.loader;

import com.scmcloud.common.dto.permission.ApiPermissionDTO;
import com.scmcloud.system.api.PermissionDubboService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 动态权限加载服务
 * 支持权限热更新，无需重启应用
 *
 * @author Deng
 * createData 2025/11/7 10:18
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicPermissionLoader {
    private final PermissionDubboService permissionDubboService;
    private final CacheManager cacheManager;
    private final ApplicationEventPublisher eventPublisher;

    private final Map<String, Set<String>> urlPermissionCache = new ConcurrentHashMap<>();
    private final AtomicLong permissionVersion = new AtomicLong(0L);

    private static final String PERMISSION_CACHE_NAME = "permissionMapping";
    private static final String PERM_MAPPING_CACHE_KEY = "dynamic:permission:mapping";

    @PostConstruct
    public void initFromCache() {
        try {
            Cache permCache = cacheManager.getCache(PERMISSION_CACHE_NAME);
            if (permCache != null) {
                Object rawCached = permCache.get(PERM_MAPPING_CACHE_KEY, Map.class);
                if (rawCached instanceof Map<?, ?> rawMap && !rawMap.isEmpty()) {
                    urlPermissionCache.clear();
                    urlPermissionCache.putAll(normalizePermissionMap(rawMap));
                    permissionVersion.set(1L);
                    log.info("Initialized dynamic permission cache from TwoLevelCache, size={}",
                            urlPermissionCache.size());
                }
            }
        } catch (Exception e) {
            log.warn("Init from cache failed", e);
        }
    }

    private static Map<String, Set<String>> normalizePermissionMap(Map<?, ?> rawMap) {
        Map<String, Set<String>> normalized = new HashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() instanceof String key && entry.getValue() instanceof Collection<?> values) {
                Set<String> permSet = new HashSet<>();
                for (Object val : values) {
                    if (val instanceof String strVal) {
                        permSet.add(strVal);
                    }
                }
                normalized.put(key, permSet);
            }
        }
        return normalized;
    }

    public void loadPermissions() {
        log.info("Loading dynamic permissions...");

        try {
            List<ApiPermissionDTO> apiPermissions = permissionDubboService.findApiPermissions();

            Map<String, Set<String>> newCache = new HashMap<>();

            for (ApiPermissionDTO perm : apiPermissions) {
                String apiPath = perm.getApiPath();
                String httpMethod = perm.getHttpMethod();
                String permissionCode = perm.getPermissionCode();

                if (apiPath != null && permissionCode != null) {
                    String key = buildKey(httpMethod, apiPath);
                    newCache.computeIfAbsent(key, k -> new HashSet<>())
                            .add(permissionCode);
                }
            }

            urlPermissionCache.clear();
            urlPermissionCache.putAll(newCache);

            Cache permCache = cacheManager.getCache(PERMISSION_CACHE_NAME);
            if (permCache != null) {
                permCache.put(PERM_MAPPING_CACHE_KEY, newCache);
            }

            permissionVersion.incrementAndGet();

            log.info("Loaded {} API permission mappings, version: {}",
                    newCache.size(), permissionVersion.get());

            eventPublisher.publishEvent(new PermissionRefreshEvent(this, permissionVersion.get()));

        } catch (Exception e) {
            log.error("Failed to load permissions", e);
        }
    }

    @Scheduled(fixedRate = 300000)
    public void scheduleRefresh() {
        log.debug("Scheduled permission refresh triggered");
        loadPermissions();
        clearRelatedCaches();
    }

    public void manualRefresh() {
        log.info("Manual permission refresh triggered");
        loadPermissions();
        clearRelatedCaches();
    }

    public boolean requiresPermission(String method, String url) {
        String key = buildKey(method, url);
        return urlPermissionCache.containsKey(key);
    }

    public Set<String> getRequiredPermissions(String method, String url) {
        String key = buildKey(method, url);
        Set<String> permissions = urlPermissionCache.get(key);

        if (permissions == null || permissions.isEmpty()) {
            permissions = matchWildcardPermissions(method, url);
        }

        return permissions != null ? permissions : Collections.emptySet();
    }

    private Set<String> matchWildcardPermissions(String method, String url) {
        for (Map.Entry<String, Set<String>> entry : urlPermissionCache.entrySet()) {
            String pattern = entry.getKey();
            if (matchesPattern(pattern, method, url)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean matchesPattern(String pattern, String method, String url) {
        String[] patternParts = pattern.split(":", 2);
        if (patternParts.length != 2) {
            return false;
        }

        String patternMethod = patternParts[0];
        String patternPath = patternParts[1];

        if (!"*".equals(patternMethod) && !method.equals(patternMethod)) {
            return false;
        }

        return matchesPath(patternPath, url);
    }

    private boolean matchesPath(String pattern, String path) {
        String[] patternSegments = pattern.split("/");
        String[] pathSegments = path.split("/");

        if (pattern.contains("**")) {
            return matchesDeepWildcard(patternSegments, pathSegments);
        }

        if (patternSegments.length != pathSegments.length) {
            return false;
        }

        for (int i = 0; i < patternSegments.length; i++) {
            String patternSeg = patternSegments[i];
            String pathSeg = pathSegments[i];

            if (patternSeg.startsWith("{") && patternSeg.endsWith("}")) {
                continue;
            }

            if ("*".equals(patternSeg)) {
                continue;
            }

            if (!patternSeg.equals(pathSeg)) {
                return false;
            }
        }

        return true;
    }

    private boolean matchesDeepWildcard(String[] patternSegments, String[] pathSegments) {
        int patternIdx = 0;
        int pathIdx = 0;

        while (patternIdx < patternSegments.length && pathIdx < pathSegments.length) {
            String patternSeg = patternSegments[patternIdx];

            if ("**".equals(patternSeg)) {
                return true;
            }

            if (patternSeg.equals(pathSegments[pathIdx]) || "*".equals(patternSeg) ||
                    (patternSeg.startsWith("{") && patternSeg.endsWith("}"))) {
                patternIdx++;
                pathIdx++;
            } else {
                return false;
            }
        }

        return patternIdx == patternSegments.length && pathIdx == pathSegments.length;
    }

    private String buildKey(String method, String path) {
        return (method != null ? method : "*") + ":" + path;
    }

    private void clearRelatedCaches() {
        try {
            String[] cacheNames = {
                    "userPermissions", "userRoles", "permissionTree",
                    "rolePermissions", "userInfo"
            };

            for (String cacheName : cacheNames) {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    log.debug("Cleared cache: {}", cacheName);
                }
            }
        } catch (Exception e) {
            log.error("Failed to clear caches", e);
        }
    }

    public long getPermissionVersion() {
        return permissionVersion.get();
    }

    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("version", permissionVersion.get());
        stats.put("cachedMappings", urlPermissionCache.size());
        stats.put("memorySize", estimateMemorySize());
        return stats;
    }

    private long estimateMemorySize() {
        long size = 0;
        for (Map.Entry<String, Set<String>> entry : urlPermissionCache.entrySet()) {
            size += entry.getKey().length() * 2L;
            size += entry.getValue().size() * 50L;
        }
        return size;
    }

    @Getter
    public static class PermissionRefreshEvent extends ApplicationEvent {
        private final long version;

        public PermissionRefreshEvent(Object source, long version) {
            super(source);
            this.version = version;
        }
    }
}
