package com.scmcloud.common.redis.config;

import com.scmcloud.common.cache.spring.TwoLevelCacheInvalidationListener;
import com.scmcloud.common.cache.spring.TwoLevelCacheManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Redis 配置
 *
 * @author Deng
 * @since 2025/10/15
 * @version 1.1
 * @apiNote 1.1 消除重复序列化器配置，提取缓存 TTL 常量
 */
@Configuration
@EnableCaching
public class RedisConfig {

    private static final String TWOLEVEL_INVALIDATION_CHANNEL = "cache:invalidation:twolevel";

    private static final StringRedisSerializer STRING_SERIALIZER = new StringRedisSerializer();
    private static final RedisSerializer<Object> JSON_SERIALIZER = RedisSerializer.json();

    /** 缓存名称与 TTL 映射，两套 CacheManager 共享 */
    private static final Map<String, Duration> CACHE_TTLS = Map.ofEntries(
            // 用户信息
            Map.entry("user", Duration.ofMinutes(30)),
            Map.entry("userInfo", Duration.ofMinutes(30)),
            Map.entry("userDetails", Duration.ofMinutes(30)),
            // 权限和角色
            Map.entry("userRoles", Duration.ofHours(1)),
            Map.entry("userPermissions", Duration.ofHours(1)),
            Map.entry("userDataScope", Duration.ofHours(1)),
            Map.entry("userMaxRoleLevel", Duration.ofHours(1)),
            Map.entry("roleLevel", Duration.ofHours(2)),
            Map.entry("permissionTree", Duration.ofHours(2)),
            Map.entry("permissionMapping", Duration.ofMinutes(5)),
            Map.entry("roles", Duration.ofHours(1)),
            Map.entry("role", Duration.ofHours(1)),
            Map.entry("rolePermissions", Duration.ofHours(1)),
            Map.entry("apiPermissions", Duration.ofHours(2)),
            // 部门相关
            Map.entry("userDeptId", Duration.ofMinutes(30)),
            Map.entry("deptPath", Duration.ofHours(2)),
            Map.entry("deptTree", Duration.ofHours(1)),
            Map.entry("deptChildren", Duration.ofHours(1)),
            Map.entry("accessibleDeptIds", Duration.ofHours(1)),
            // 临时角色
            Map.entry("userTemporaryRoles", Duration.ofMinutes(15))
    );

    @Bean
    @ConditionalOnMissingBean(RedisTemplate.class)
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(STRING_SERIALIZER);
        template.setHashKeySerializer(STRING_SERIALIZER);
        template.setValueSerializer(JSON_SERIALIZER);
        template.setHashValueSerializer(JSON_SERIALIZER);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @ConditionalOnProperty(name = "scm.cache.two-level.enabled", havingValue = "true")
    public RedisMessageListenerContainer twoLevelCacheListenerContainer(
            RedisConnectionFactory connectionFactory,
            TwoLevelCacheInvalidationListener twoLevelListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(new MessageListenerAdapter(twoLevelListener), new PatternTopic(TWOLEVEL_INVALIDATION_CHANNEL));
        return container;
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "scm.cache.two-level.enabled", havingValue = "true")
    public CacheManager twoLevelCacheManager(RedisTemplate<String, Object> redisTemplate) {
        return new TwoLevelCacheManager(redisTemplate, Duration.ofHours(1), CACHE_TTLS, 10_000L);
    }

    @Bean
    @ConditionalOnMissingBean(TwoLevelCacheManager.class)
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultConfig = cacheConfig(Duration.ofHours(1));

        Map<String, RedisCacheConfiguration> configs = CACHE_TTLS.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> cacheConfig(e.getValue())
                ));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(configs)
                .build();
    }

    private RedisCacheConfiguration cacheConfig(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(STRING_SERIALIZER))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(JSON_SERIALIZER))
                .entryTtl(ttl);
    }
}
