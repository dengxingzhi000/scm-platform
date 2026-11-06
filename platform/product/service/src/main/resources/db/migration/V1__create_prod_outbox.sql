-- 商品 SPU 变更事件 outbox。
--
-- 背景：ES 索引 scm_product 与 Kafka topic scm_product_server.public.prod_spu 之前
-- 依赖 Debezium 抓取 PostgreSQL WAL，但库里既没有 publication/slot，也没有
-- Kafka Connect，因此事件从未产生。这里改成应用侧 outbox 生产者（方案 A）：
-- 业务数据与事件行在同一个事务内落库，再由 ProdOutboxPublisher 定时投递。
--
-- 幂等：手工建过表的环境重复执行不会失败。

CREATE TABLE IF NOT EXISTS prod_outbox (
    id            UUID PRIMARY KEY,
    tenant_id     UUID NOT NULL,
    aggregate_id  VARCHAR(64) NOT NULL,
    event_type    VARCHAR(32) NOT NULL,
    payload       JSONB NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at  TIMESTAMPTZ,
    retry_count   INTEGER NOT NULL DEFAULT 0
);

COMMENT ON TABLE prod_outbox IS '商品域变更事件 outbox：与业务写入同事务，发布后回填 published_at';
COMMENT ON COLUMN prod_outbox.event_type IS 'SPU_UPSERT / SPU_DELETE';
COMMENT ON COLUMN prod_outbox.aggregate_id IS '聚合根 ID，作为 Kafka 消息 key 保证同一商品的顺序';
COMMENT ON COLUMN prod_outbox.payload IS 'Debezium 兼容的事件体（op + after/before），消费者无需改动';
COMMENT ON COLUMN prod_outbox.retry_count IS '投递失败次数，达到上限后保留行但不再投递';

-- 发布器只扫未发布的行，partial index 避免扫描历史已发布数据。
CREATE INDEX IF NOT EXISTS idx_prod_outbox_unpublished
    ON prod_outbox (created_at)
    WHERE published_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_prod_outbox_aggregate
    ON prod_outbox (aggregate_id);
