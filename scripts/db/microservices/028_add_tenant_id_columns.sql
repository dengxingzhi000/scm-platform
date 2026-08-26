-- ======================================================================
-- 028 — Add tenant_id column to business tables
--
-- Purpose
--   Bring every business table in the schema-validation list (see
--   scripts/db/ci_validate_tenant_id.sql) up to multi-tenant parity with
--   the finance/purchase tables that already carry tenant_id.
--
-- Why two tracks
--   Track A (this file): safe ALTER for any existing environment. Uses
--   `DEFAULT gen_random_uuid()` so existing rows receive a synthetic
--   tenant_id that can later be rewritten by a real backfill.
--   Track B (010_db_product.sql … 015_db_supplier.sql): new environments
--   that run init-all-databases.sh get tenant_id declared inline so
--   fresh inserts follow the same shape as finance/purchase tables
--   (UUID NOT NULL, no default — application fills from TenantContext).
--
-- Invocation model
--   This file is meant to be applied by `init-all-databases.sh`, which
--   invokes each script once per target database via
--   `psql -d <dbname> -f <script>`. Each invocation connects to one
--   database, so this script processes every table that exists in that
--   database and silently skips the rest — no \connect statements.
--   The full list of expected tables is taken from
--   ci_validate_tenant_id.sql so both stay in sync.
--
-- Scope
--   32 tables across 6 service databases. inv_tcc_reservation is already
--   covered by 021_inventory_tcc.sql and is left untouched.
--
-- Idempotency
--   Each ALTER is guarded by an information_schema column-existence check,
--   so re-running the migration is a no-op.
--
-- NOT in this file
--   Widening UNIQUE constraints to be tenant-scoped (e.g.
--   UNIQUE(tenant_id, category_code)). That is a separate change because
--   it requires deciding which business keys are tenant-scoped vs.
--   globally unique. Tracked as Track C.
-- ======================================================================

DO $$
DECLARE
    t TEXT;
    idx_name TEXT;
    expected_tables TEXT[] := ARRAY[
        -- Product
        'prod_category', 'prod_brand', 'prod_spu', 'prod_sku', 'prod_attribute_template',
        -- Inventory
        'inv_inventory', 'inv_reservation', 'inv_log', 'inv_snapshot', 'inv_alert',
        -- Order
        'ord_order', 'ord_order_item', 'ord_status_history', 'ord_payment', 'ord_refund',
        -- Warehouse
        'wms_warehouse', 'wms_location', 'wms_inbound', 'wms_inbound_item',
        'wms_outbound', 'wms_outbound_item', 'wms_wave_picking',
        -- Logistics
        'tms_carrier', 'tms_waybill', 'tms_tracking', 'tms_route', 'tms_delivery_area',
        -- Supplier
        'sup_supplier', 'sup_purchase_order', 'sup_purchase_order_item',
        'sup_supplier_evaluation', 'sup_settlement'
    ];
BEGIN
    FOREACH t IN ARRAY expected_tables LOOP
        -- Only act on tables that actually live in the current database
        IF EXISTS (
            SELECT 1 FROM information_schema.tables
            WHERE table_schema = 'public' AND table_name = t
        ) AND NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = t AND column_name = 'tenant_id'
        ) THEN
            EXECUTE format(
                'ALTER TABLE %I ADD COLUMN tenant_id UUID NOT NULL DEFAULT gen_random_uuid()',
                t
            );
            -- Index naming matches the convention in the original per-DB
            -- sections: prod_* → strip "prod_"; everything else → strip
            -- first 4 chars (inv_ / ord_ / wms_ / tms_ / sup_).
            IF t LIKE 'prod\_%' THEN
                idx_name := 'idx_' || replace(t, 'prod_', '') || '_tenant';
            ELSE
                idx_name := 'idx_' || substring(t FROM 5) || '_tenant';
            END IF;
            EXECUTE format(
                'CREATE INDEX %I ON %I(tenant_id) WHERE NOT deleted',
                idx_name, t
            );
            RAISE NOTICE 'Added tenant_id to current_db.%', t;
        END IF;
    END LOOP;
END $$;