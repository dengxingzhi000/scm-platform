-- V2: multi-tenant isolation for order-center tables.
ALTER TABLE oc_order ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE oc_order_item ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE oc_order_payment ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE oc_order_logistics ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE oc_order_state_history ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE oc_order_event ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE oc_order_split ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();

CREATE INDEX IF NOT EXISTS idx_oc_order_tenant ON oc_order(tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_oc_order_item_tenant ON oc_order_item(tenant_id, order_no);
CREATE INDEX IF NOT EXISTS idx_oc_event_tenant ON oc_order_event(tenant_id, order_no);
