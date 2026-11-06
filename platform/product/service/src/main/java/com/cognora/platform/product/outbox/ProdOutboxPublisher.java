package com.cognora.platform.product.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * prod_outbox 的定时发布器。
 *
 * <p>批量取未发布行，逐条发送到商品同步 topic（key = aggregate_id），成功后回填
 * published_at；失败累加 retry_count 并保留行，达到上限后 SQL 不再选中它，行仍可查。</p>
 *
 * <p>调度取舍：仓库里其它 outbox（{@code platform/common/integration}）同样用
 * {@code @Scheduled} 轮询，且本模块没有直接声明 xxl-job-core，接入 XXL-Job 还要在
 * 调度中心注册任务，所以这里沿用 {@code @Scheduled}，间隔可配。</p>
 *
 * <p>投递语义是 at-least-once：重复投递时消费者只是再 save 一次同一个文档，幂等。</p>
 */
@Slf4j
@Component
public class ProdOutboxPublisher {

    /** 一轮内连续失败这么多条就放弃本轮，避免 Kafka 不可用时把调度线程按「每条 5 秒」占满。 */
    private static final int MAX_CONSECUTIVE_FAILURES = 3;

    private final ProdOutboxService prodOutboxService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private final String topic;
    private final int batchSize;
    private final int maxRetries;
    private final long sendTimeoutMs;

    public ProdOutboxPublisher(ProdOutboxService prodOutboxService,
                               KafkaTemplate<String, String> kafkaTemplate,
                               @Value("${kafka.product.sync.topic:scm_product_server.public.prod_spu}") String topic,
                               @Value("${product.outbox.batch-size:100}") int batchSize,
                               @Value("${product.outbox.max-retries:5}") int maxRetries,
                               @Value("${product.outbox.send-timeout-ms:5000}") long sendTimeoutMs) {
        this.prodOutboxService = prodOutboxService;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
        this.sendTimeoutMs = sendTimeoutMs;
    }

    @Scheduled(fixedDelayString = "${product.outbox.poll-interval-ms:1000}")
    public void publishPending() {
        List<ProdOutboxEvent> events = prodOutboxService.findPublishable(maxRetries, batchSize);
        if (events.isEmpty()) {
            return;
        }

        int published = 0;
        int failed = 0;
        int consecutiveFailures = 0;
        for (ProdOutboxEvent event : events) {
            if (publish(event)) {
                published++;
                consecutiveFailures = 0;
            } else {
                failed++;
                if (++consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                    log.warn("[商品 outbox] 连续 {} 条投递失败，提前结束本轮: topic={}", consecutiveFailures, topic);
                    break;
                }
            }
        }
        log.info("[商品 outbox] 本轮投递完成: topic={}, published={}, failed={}", topic, published, failed);
    }

    /**
     * 发送单条事件。成功回填 published_at；失败累加 retry_count 并保留未发布状态。
     *
     * @return 是否投递成功
     */
    boolean publish(ProdOutboxEvent event) {
        if (event.getRetryCount() != null && event.getRetryCount() >= maxRetries) {
            log.warn("[商品 outbox] 已达重试上限，跳过投递: id={}, aggregateId={}, retryCount={}",
                    event.getId(), event.getAggregateId(), event.getRetryCount());
            return false;
        }

        try {
            kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload())
                    .get(sendTimeoutMs, TimeUnit.MILLISECONDS);
            prodOutboxService.markPublished(event.getId());
            log.debug("[商品 outbox] 投递成功: id={}, aggregateId={}, type={}",
                    event.getId(), event.getAggregateId(), event.getEventType());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            prodOutboxService.incrementRetryCount(event.getId());
            log.warn("[商品 outbox] 投递被中断: id={}, aggregateId={}", event.getId(), event.getAggregateId());
            return false;
        } catch (Exception e) {
            prodOutboxService.incrementRetryCount(event.getId());
            log.warn("[商品 outbox] 投递失败，已累加重试次数: id={}, aggregateId={}, retryCount={}, error={}",
                    event.getId(), event.getAggregateId(), event.getRetryCount(), e.getMessage());
            return false;
        }
    }
}
