-- Align scm-document tenant_id with the platform-wide UUID string convention.
-- V1_0_0 declared BIGINT (matching the old UUID.getMostSignificantBits() storage),
-- but every document entity exposes tenantId as String. Converting the column
-- with USING tenant_id::TEXT keeps existing rows addressable and lets new rows
-- store the full tenant UUID, matching scm-file's V1_0_1 migration.

ALTER TABLE doc_file_metadata
    ALTER COLUMN tenant_id TYPE VARCHAR(36) USING tenant_id::TEXT;

ALTER TABLE doc_template
    ALTER COLUMN tenant_id TYPE VARCHAR(36) USING tenant_id::TEXT;

ALTER TABLE doc_template_version
    ALTER COLUMN tenant_id TYPE VARCHAR(36) USING tenant_id::TEXT;

ALTER TABLE doc_template_schema
    ALTER COLUMN tenant_id TYPE VARCHAR(36) USING tenant_id::TEXT;

ALTER TABLE doc_document
    ALTER COLUMN tenant_id TYPE VARCHAR(36) USING tenant_id::TEXT;

ALTER TABLE doc_document_version
    ALTER COLUMN tenant_id TYPE VARCHAR(36) USING tenant_id::TEXT;

ALTER TABLE doc_document_audit
    ALTER COLUMN tenant_id TYPE VARCHAR(36) USING tenant_id::TEXT;
