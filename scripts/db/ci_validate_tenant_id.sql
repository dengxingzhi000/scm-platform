-- ======================================================================
-- CI Schema Validation: Verify all business tables have tenant_id
-- Run this in CI to catch missing tenant_id columns before deployment
-- Exit with error if any expected table is missing tenant_id
-- ======================================================================

DO $$
DECLARE
    missing_tables TEXT := '';
    tbl TEXT;
    expected_tables TEXT[] := ARRAY[
        -- Product
        'prod_category', 'prod_brand', 'prod_spu', 'prod_sku', 'prod_attribute_template',
        -- Inventory
        'inv_inventory', 'inv_reservation', 'inv_log', 'inv_snapshot', 'inv_alert', 'inv_tcc_reservation',
        -- Order
        'ord_order', 'ord_order_item', 'ord_status_history', 'ord_payment', 'ord_refund', 'ord_order_event',
        -- Warehouse
        'wms_warehouse', 'wms_location', 'wms_inbound', 'wms_inbound_item',
        'wms_outbound', 'wms_outbound_item', 'wms_wave_picking',
        -- Logistics
        'tms_carrier', 'tms_waybill', 'tms_tracking', 'tms_route', 'tms_delivery_area',
        -- Supplier
        'sup_supplier', 'sup_purchase_order', 'sup_purchase_order_item',
        'sup_supplier_evaluation', 'sup_settlement',
        -- Document (scm-document, db_document)
        'doc_file_metadata', 'doc_template', 'doc_template_version',
        'doc_template_schema', 'doc_document', 'doc_document_version',
        'doc_document_audit'
    ];
    rec RECORD;
BEGIN
    FOREACH tbl IN ARRAY expected_tables
    LOOP
        -- Check if table exists and has tenant_id column
        SELECT INTO rec
            t.table_name,
            c.column_name
        FROM information_schema.tables t
        LEFT JOIN information_schema.columns c
            ON t.table_name = c.table_name
            AND c.column_name = 'tenant_id'
            AND t.table_schema = c.table_schema
        WHERE t.table_name = tbl
            AND t.table_schema = 'public'
            AND c.column_name IS NULL;

        IF FOUND THEN
            missing_tables := missing_tables || tbl || ', ';
        END IF;
    END LOOP;

    IF missing_tables != '' THEN
        RAISE EXCEPTION 'CI SCHEMA VALIDATION FAILED: Tables missing tenant_id: %', rtrim(missing_tables, ', ');
    ELSE
        RAISE NOTICE 'CI SCHEMA VALIDATION PASSED: All % business tables have tenant_id', array_length(expected_tables, 1);
    END IF;
END $$;
