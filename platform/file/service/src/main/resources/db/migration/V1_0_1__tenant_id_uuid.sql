-- Align scm-file tenant_id with the platform-wide UUID string convention.
-- The previous BIGINT (derived via UUID.getMostSignificantBits()) was lossy and
-- prevented correlation with other modules. Tenant identity is now stored as the
-- full UUID string, matching TenantAwareEntity and the rest of the platform.

ALTER TABLE sys_file_metadata
    ALTER COLUMN tenant_id TYPE VARCHAR(36) USING tenant_id::TEXT;

ALTER TABLE sys_file_version
    ALTER COLUMN tenant_id TYPE VARCHAR(36) USING tenant_id::TEXT;

ALTER TABLE sys_upload_task
    ALTER COLUMN tenant_id TYPE VARCHAR(36) USING tenant_id::TEXT;

ALTER TABLE sys_file_metadata
    ALTER COLUMN create_by TYPE VARCHAR(36) USING create_by::TEXT;

ALTER TABLE sys_file_version
    ALTER COLUMN create_by TYPE VARCHAR(36) USING create_by::TEXT;

ALTER TABLE sys_upload_task
    ALTER COLUMN create_by TYPE VARCHAR(36) USING create_by::TEXT;
