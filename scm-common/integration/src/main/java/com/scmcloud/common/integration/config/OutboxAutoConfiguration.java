package com.scmcloud.common.integration.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scmcloud.common.integration.messaging.KafkaMessagePublisher;
import com.scmcloud.common.integration.outbox.OutboxPoller;
import com.scmcloud.common.integration.outbox.OutboxService;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Services scan only their own {@code com.scmcloud.<service>.mapper} package, so the outbox
 * mapper nested in {@link OutboxService} would never be registered and the shared
 * {@link OutboxService} bean would fail to start the context.
 *
 * <p>The poller is registered here (not as a scanned {@code @Component}) because
 * {@code @ConditionalOnBean} on a component is evaluated during scanning, before the Kafka
 * publisher from {@link KafkaIntegrationAutoConfiguration} exists.
 */
@AutoConfiguration(after = KafkaIntegrationAutoConfiguration.class)
@ConditionalOnClass({MapperFactoryBean.class, KafkaMessagePublisher.class})
@MapperScan(basePackageClasses = OutboxService.class)
public class OutboxAutoConfiguration {

    @Bean
    @ConditionalOnBean(KafkaMessagePublisher.class)
    @ConditionalOnMissingBean
    public OutboxPoller outboxPoller(OutboxService outboxService,
                                     KafkaMessagePublisher kafkaMessagePublisher,
                                     ObjectMapper objectMapper) {
        return new OutboxPoller(outboxService, kafkaMessagePublisher, objectMapper);
    }
}
