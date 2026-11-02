package com.scmcloud.notify.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Bounded retries for listener failures: three attempts two seconds apart, then the record is
 * logged and skipped instead of blocking the partition forever.
 */
@Configuration
public class NotificationKafkaConfig {

    @Bean
    public DefaultErrorHandler notificationErrorHandler() {
        return new DefaultErrorHandler(new FixedBackOff(2_000L, 3L));
    }
}
