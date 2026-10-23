-- V2: multi-tenant isolation for member tables.
ALTER TABLE mem_member ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE mem_member_level ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE mem_member_address ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE mem_member_points_log ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE mem_third_party_binding ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE mem_member_tag ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL DEFAULT gen_random_uuid();

CREATE INDEX IF NOT EXISTS idx_mem_member_tenant ON mem_member(tenant_id);
CREATE INDEX IF NOT EXISTS idx_mem_level_tenant ON mem_member_level(tenant_id);
CREATE INDEX IF NOT EXISTS idx_mem_address_tenant ON mem_member_address(tenant_id);
CREATE INDEX IF NOT EXISTS idx_mem_points_tenant ON mem_member_points_log(tenant_id);
CREATE INDEX IF NOT EXISTS idx_mem_binding_tenant ON mem_third_party_binding(tenant_id);
CREATE INDEX IF NOT EXISTS idx_mem_tag_tenant ON mem_member_tag(tenant_id);
