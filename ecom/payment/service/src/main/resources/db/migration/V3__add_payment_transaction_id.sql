-- 支付回调需要保存渠道交易号，用于对账与幂等判断
ALTER TABLE pay_payment_order ADD COLUMN IF NOT EXISTS transaction_id VARCHAR(128);

CREATE INDEX IF NOT EXISTS idx_pay_transaction ON pay_payment_order(transaction_id);
