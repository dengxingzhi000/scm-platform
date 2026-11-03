-- V2: multi-tenant isolation for payment tables.
ALTER TABLE pay_payment_order ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE pay_refund ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE pay_channel_config ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE pay_reconciliation ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();

CREATE INDEX IF NOT EXISTS idx_pay_order_tenant ON pay_payment_order(tenant_id, order_no);
CREATE INDEX IF NOT EXISTS idx_pay_refund_tenant ON pay_refund(tenant_id, payment_no);
CREATE INDEX IF NOT EXISTS idx_pay_recon_tenant ON pay_reconciliation(tenant_id, recon_date);
