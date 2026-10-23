-- V2: multi-tenant isolation for promotion tables.
ALTER TABLE pro_activity ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE pro_coupon_template ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE pro_coupon ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE pro_flash_sale ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE pro_group_buy ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE pro_group_buy_member ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE pro_bundle ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE pro_bundle_item ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE pro_tiered_pricing ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE pro_recommendation_log ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE pro_ab_test ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE pro_ab_test_assignment ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();

-- Group-buy membership now tracks the resulting order and member status.
ALTER TABLE pro_group_buy_member ADD COLUMN IF NOT EXISTS order_no VARCHAR(64);
ALTER TABLE pro_group_buy_member ADD COLUMN IF NOT EXISTS status SMALLINT DEFAULT 1;

CREATE INDEX IF NOT EXISTS idx_pro_activity_tenant ON pro_activity(tenant_id);
CREATE INDEX IF NOT EXISTS idx_pro_coupon_tenant ON pro_coupon(tenant_id, user_id);
CREATE INDEX IF NOT EXISTS idx_pro_flash_tenant ON pro_flash_sale(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_pro_group_tenant ON pro_group_buy(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_pro_group_member_tenant ON pro_group_buy_member(tenant_id, group_buy_id);
