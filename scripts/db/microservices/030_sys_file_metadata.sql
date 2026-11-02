-- ======================================================================
-- sys_file_metadata — scm-file metadata table (db_system)
--
-- The service's Flyway V1_0_0 also creates this table; this bootstrap copy
-- lets a database created by init-all-databases work before Flyway runs and
-- matches com.scmcloud.file.entity.FileMetadata:
--   * tenant_id / create_by are UUID strings, not BIGINT
--   * update_by stays BIGINT (the entity maps it to Long)
--   * deleted is the MyBatis-Plus logic-delete flag (0 = active, 1 = deleted)
-- ======================================================================
CREATE TABLE IF NOT EXISTS sys_file_metadata (
    id              VARCHAR(36) PRIMARY KEY,
    original_name   VARCHAR(255) NOT NULL,
    storage_key     VARCHAR(500) NOT NULL,
    content_type    VARCHAR(100),
    file_size       BIGINT,
    storage_engine  VARCHAR(50) NOT NULL,
    md5             VARCHAR(32),
    version         INTEGER DEFAULT 1,
    biz_type        VARCHAR(50),
    biz_id          VARCHAR(100),
    status          VARCHAR(20) DEFAULT 'NORMAL',
    ref_count       INTEGER DEFAULT 1,
    tenant_id       VARCHAR(36) NOT NULL,
    create_by       VARCHAR(36),
    update_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_file_metadata_md5 ON sys_file_metadata(md5);
CREATE INDEX IF NOT EXISTS idx_file_metadata_tenant ON sys_file_metadata(tenant_id);
CREATE INDEX IF NOT EXISTS idx_file_metadata_biz ON sys_file_metadata(biz_type, biz_id);
