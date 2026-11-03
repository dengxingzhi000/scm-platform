package com.cognora.platform.common.tenant;

import org.apache.ibatis.plugin.Interceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Puts {@link TenantInterceptor} on the MyBatis plugin chain, so tenant isolation is enforced by
 * the SQL itself instead of by whoever remembered to add the predicate.
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.apache.ibatis.plugin.Interceptor")
public class TenantMybatisAutoConfiguration {

    /**
     * Lowest precedence on purpose: MyBatis wraps plugins in registration order, so the last one
     * added ends up outermost and runs first. Being outermost is what makes the tenant predicate
     * part of the SQL before MyBatis-Plus derives the pagination COUNT query from it.
     */
    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    @ConditionalOnMissingBean(TenantInterceptor.class)
    @ConditionalOnProperty(prefix = "cognora.tenant", name = "sql-isolation", matchIfMissing = true)
    public Interceptor tenantInterceptor(TenantProperties properties) {
        return new TenantInterceptor(properties);
    }
}
