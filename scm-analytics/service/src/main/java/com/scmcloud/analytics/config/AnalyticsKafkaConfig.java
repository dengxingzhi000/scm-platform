package com.scmcloud.analytics.config;

import com.clickhouse.client.api.Client;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring beans for the analytics CDC ingest path:
 * <ul>
 *   <li>{@link ClickHouseProperties} (via {@code @EnableConfigurationProperties})</li>
 *   <li>{@code odsKafkaListenerContainerFactory} — batched consumer factory used by
 *       {@link com.scmcloud.analytics.ingest.OdsIngestConsumer}; manual ack,
 *       max-poll-records=1000, String key/value deserializers.</li>
 *   <li>{@link Client} — client-v2 connection to ClickHouse, lifecycle managed by Spring.</li>
 * </ul>
 * <p>
 * Pulls broker connection settings from {@link KafkaProperties}, which is
 * auto-configured by Spring Boot from {@code spring.kafka.*} keys and can be
 * overridden in test contexts.
 */
@Configuration
@EnableConfigurationProperties(ClickHouseProperties.class)
public class AnalyticsKafkaConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> odsKafkaListenerContainerFactory(
            KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1000);

        DefaultKafkaConsumerFactory<String, String> consumerFactory =
                new DefaultKafkaConsumerFactory<>(props);

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }

    @Bean(destroyMethod = "close")
    public Client clickHouseClient(ClickHouseProperties props) {
        Client.Builder builder = new Client.Builder()
                .addEndpoint(props.getUrl())
                .setUsername(props.getUsername())
                .setPassword(props.getPassword() == null ? "" : props.getPassword())
                .setDefaultDatabase(props.getDatabase());
        return builder.build();
    }
}
