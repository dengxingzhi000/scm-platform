-- Align scm-message tenant_id with the platform-wide UUID string convention.
-- V1_0_0 declared BIGINT (matching the old UUID.getMostSignificantBits() storage),
-- but the platform's TenantInterceptor rewrites every tenant-scoped statement with
-- a full tenant UUID as a string literal. Against a BIGINT column PostgreSQL
-- rejects that literal with 22P02 (invalid input syntax for type bigint), so
-- every message query failed. Converting the column with USING tenant_id::TEXT
-- matches scm-file's and scm-document's V1_0_1 migrations.
--
-- The conversion rewrites the stored representation, it never drops a row: a
-- legacy BIGINT id becomes its decimal text. Such a row no longer matches the
-- UUID tenant predicate, so the guard below reports it instead of leaving that
-- loss silent. In practice both tables are created by the immediately preceding
-- V1_0_0 migration, so there is nothing to backfill.

DO $$
DECLARE
    legacy_rows BIGINT;
BEGIN
    SELECT (SELECT COUNT(*) FROM sys_event_outbox) + (SELECT COUNT(*) FROM sys_event_dlq)
        INTO legacy_rows;

    IF legacy_rows > 0 THEN
        RAISE WARNING 'scm-message: % row(s) carry a legacy BIGINT tenant_id; they are kept as decimal text and will not match the UUID tenant predicate', legacy_rows;
    END IF;
END $$;

ALTER TABLE sys_event_outbox
    ALTER COLUMN tenant_id TYPE VARCHAR(36) USING tenant_id::TEXT;

ALTER TABLE sys_event_dlq
    ALTER COLUMN tenant_id TYPE VARCHAR(36) USING tenant_id::TEXT;
