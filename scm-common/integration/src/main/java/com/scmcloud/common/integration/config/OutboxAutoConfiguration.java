package com.scmcloud.common.integration.config;

import com.scmcloud.common.integration.outbox.OutboxService;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * Services scan only their own {@code com.scmcloud.<service>.mapper} package, so the outbox
 * mapper nested in {@link OutboxService} would never be registered and the shared
 * {@link OutboxService} bean would fail to start the context.
 */
@AutoConfiguration
@ConditionalOnClass(MapperFactoryBean.class)
@MapperScan(basePackageClasses = OutboxService.class)
public class OutboxAutoConfiguration {
}
