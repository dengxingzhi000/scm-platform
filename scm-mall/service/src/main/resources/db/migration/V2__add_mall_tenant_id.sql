-- V2: multi-tenant isolation for mall tables.
ALTER TABLE mall_cart ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE mall_seller ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE mall_seller_shop ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE mall_product_display ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE mall_review ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE mall_review_reply ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE mall_browsing_history ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE mall_favorite ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();

CREATE INDEX IF NOT EXISTS idx_mall_cart_tenant ON mall_cart(tenant_id, user_id);
CREATE INDEX IF NOT EXISTS idx_mall_seller_tenant ON mall_seller(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_mall_review_tenant ON mall_review(tenant_id, product_id, status);
