-- ======================================================================
-- Order 模块 schema 对齐 (db_order)
--
-- 充分利用 PostgreSQL 13+ 特性:
--   * UUID + gen_random_uuid() 主键 (pgcrypto)
--   * INCLUDE 子句 —— 覆盖索引,避免回表
--   * BRIN 索引 —— 时序字段专用,索引大小比 B-tree 小一个数量级
--   * 部分索引 —— 仅索引活跃子集,节省存储
--   * NOT VALID 外键 —— 大表添加外键不阻塞读写
--   * UNIQUE 约束 —— 数据库层业务不变量防御
--   * COMMENT ON 覆盖表/列/约束 —— 自文档化,导出到 pg_dump
--   * TIMESTAMPTZ —— 始终带时区,避免 UTC/本地时间混用
--   * DECIMAL 而非 FLOAT —— 金额精确小数,无浮点误差
--
-- 执行时机: 项目尚未上线,数据可重建;在 init-all-databases.sh 跑完后
--          再单独执行本文件。
-- ======================================================================

-- 1. 删除状态历史自动触发器
--    Java 路径 (OrdOrderCommandService.updateOrderStatus / cancelTimeoutOrder)
--    在同一事务内手动写入 ord_status_history,提供 operator_name/type/remark
--    等触发器无法填写的字段;双写会导致同一状态变更出现两行。
DROP TRIGGER IF EXISTS trg_order_status_history ON ord_order;
DROP FUNCTION IF EXISTS fn_record_order_status_change();

-- 2. 删除 ord_refund.refund_items JSONB 列
--    拆分为 ord_refund_item 子表,支持按 sku 维度查询/索引/统计。
ALTER TABLE ord_refund DROP COLUMN IF EXISTS refund_items;

-- 3. 新建 ord_refund_item 子表
CREATE TABLE IF NOT EXISTS ord_refund_item (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID         NOT NULL,

    -- 关联原单(便于快速定位订单/退款单,避免回表)
    refund_id       UUID         NOT NULL,
    refund_no       VARCHAR(128) NOT NULL,
    order_id        UUID         NOT NULL,
    order_no        VARCHAR(128) NOT NULL,
    order_item_id   UUID         NOT NULL,

    -- 商品信息(sku_name / sku_code 反范式冗余,避免查询时 JOIN ord_order_item)
    sku_id          UUID         NOT NULL,
    sku_code        VARCHAR(128),
    sku_name        VARCHAR(256) NOT NULL,

    -- 退款明细
    quantity        INT          NOT NULL,
    refund_amount   DECIMAL(12, 2) NOT NULL,

    remark          TEXT,
    create_time     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    -- ─── 约束 ────────────────────────────────────────────────
    -- 业务约束
    CONSTRAINT chk_refund_item_quantity CHECK (quantity > 0),
    CONSTRAINT chk_refund_item_amount  CHECK (refund_amount >= 0),

    -- 同一退款单内同一订单明细只能出现一次(防重复录入)
    CONSTRAINT uq_refund_item_refund_order_item UNIQUE (refund_id, order_item_id),

    -- FK: 退款单删除时级联清理明细
    -- 使用 NOT VALID: 项目尚未上线,理论上没有孤儿行;保留此选项是为后续
    -- 存量数据导入场景避免全表扫描阻塞 DML,部署后异步 VALIDATE。
    CONSTRAINT fk_refund_item_refund
        FOREIGN KEY (refund_id) REFERENCES ord_refund(id) ON DELETE CASCADE NOT VALID,
    CONSTRAINT fk_refund_item_order_item
        FOREIGN KEY (order_item_id) REFERENCES ord_order_item(id) NOT VALID
);
-- 上线后或导入存量数据后异步校验 FK(不阻塞表读写):
-- ALTER TABLE ord_refund_item VALIDATE CONSTRAINT fk_refund_item_refund;
-- ALTER TABLE ord_refund_item VALIDATE CONSTRAINT fk_refund_item_order_item;

-- ─── 索引策略 ────────────────────────────────────────────────
-- 主查询路径: 按 refund_id 查明细 + 按时间倒序
-- INCLUDE 子句: 把 sku_id / quantity / refund_amount 纳入索引,
--               列表查询可走 index-only scan,免回表。
CREATE INDEX idx_refund_item_refund_time
    ON ord_refund_item (refund_id, create_time DESC)
    INCLUDE (sku_id, quantity, refund_amount);

-- 按 order_item_id 反查(用于"哪些退款单退了这行明细?")
CREATE INDEX idx_refund_item_order_item
    ON ord_refund_item (order_item_id);

-- BRIN 索引: 时序范围扫描专用
--   索引大小 ≈ 表大小的 1/1000,适合"最近 N 天的退款"类查询 / 对账
--   pages_per_range=32 在索引大小和选择率间取得平衡
CREATE INDEX idx_refund_item_time_brin
    ON ord_refund_item USING BRIN (create_time)
    WITH (pages_per_range = 32);

-- 部分索引: 仅索引有备注的记录
--   假设绝大多数退款明细 remark 为空,部分索引只占少量空间,
--   审计搜索 "WHERE remark LIKE '%xxx%'" 时直接命中
CREATE INDEX idx_refund_item_with_remark
    ON ord_refund_item (refund_id) WHERE remark IS NOT NULL AND remark <> '';

-- ─── 文档化 ─────────────────────────────────────────────────────
COMMENT ON TABLE  ord_refund_item                IS '退款单明细(每行对应一个 SKU 的退款)';

COMMENT ON COLUMN ord_refund_item.id             IS '主键(UUID,pgcrypto gen_random_uuid())';
COMMENT ON COLUMN ord_refund_item.tenant_id      IS '租户 ID';
COMMENT ON COLUMN ord_refund_item.refund_id     IS '退款单 ID';
COMMENT ON COLUMN ord_refund_item.refund_no     IS '退款单号(冗余便于查询)';
COMMENT ON COLUMN ord_refund_item.order_id      IS '原订单 ID';
COMMENT ON COLUMN ord_refund_item.order_no      IS '原订单号(冗余便于查询)';
COMMENT ON COLUMN ord_refund_item.order_item_id  IS '关联订单明细行 ord_order_item.id';
COMMENT ON COLUMN ord_refund_item.sku_id         IS '商品 SKU ID';
COMMENT ON COLUMN ord_refund_item.sku_code      IS 'SKU 编码(冗余便于查询)';
COMMENT ON COLUMN ord_refund_item.sku_name      IS 'SKU 名称(冗余,列表展示免 JOIN)';
COMMENT ON COLUMN ord_refund_item.quantity      IS '退款件数(必须 > 0)';
COMMENT ON COLUMN ord_refund_item.refund_amount IS '退款金额(元,DECIMAL(12,2) 保证精确小数)';
COMMENT ON COLUMN ord_refund_item.remark        IS '备注';
COMMENT ON COLUMN ord_refund_item.create_time   IS '创建时间(TIMESTAMPTZ 始终带时区)';

COMMENT ON CONSTRAINT chk_refund_item_quantity         ON ord_refund_item IS '退款件数必须为正';
COMMENT ON CONSTRAINT chk_refund_item_amount          ON ord_refund_item IS '退款金额不可为负';
COMMENT ON CONSTRAINT uq_refund_item_refund_order_item ON ord_refund_item IS '同一退款单内同一订单明细只能出现一次';
COMMENT ON CONSTRAINT fk_refund_item_refund            ON ord_refund_item IS '退款单删除时级联清理明细';
COMMENT ON CONSTRAINT fk_refund_item_order_item        ON ord_refund_item IS '关联订单明细行(异步校验)';

COMMENT ON INDEX idx_refund_item_refund_time  IS '退款明细主查询索引(覆盖索引,免回表)';
COMMENT ON INDEX idx_refund_item_order_item   IS '按订单明细反查退款单';
COMMENT ON INDEX idx_refund_item_time_brin   IS '时序范围扫描专用 BRIN 索引';
COMMENT ON INDEX idx_refund_item_with_remark  IS '仅索引有备注的部分索引(审计搜索)';

-- ─── 派生列:ord_order_item.refund_remaining_quantity ─────────
-- DB 层自动维护 quantity - refund_quantity,避免应用层每次聚合。
-- 仅保留 quantity > refund_quantity 的不变量;触发退款超出时由 CHECK 约束报错。
ALTER TABLE ord_order_item
    ADD COLUMN IF NOT EXISTS refund_remaining_quantity INT
    GENERATED ALWAYS AS (quantity - refund_quantity) STORED;

-- 业务不变量:已退件数不超过原件数
-- chk_refund_quantity 原有约束 chk_refund_quantity >= 0 AND refund_quantity <= quantity
-- 已由生成列自动满足,无需新增约束。

COMMENT ON COLUMN ord_order_item.refund_remaining_quantity IS
    '剩余可退件数(quantity - refund_quantity,DB 自动维护,应用层只读)';

-- 配合生成列的索引:加速"剩余可退的订单明细"查询
CREATE INDEX IF NOT EXISTS idx_order_item_remaining_refundable
    ON ord_order_item (order_id) WHERE refund_remaining_quantity > 0;
