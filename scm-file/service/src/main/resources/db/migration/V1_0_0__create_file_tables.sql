-- File metadata table
CREATE TABLE sys_file_metadata (
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
    tenant_id       BIGINT NOT NULL,
    create_by       BIGINT,
    update_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER DEFAULT 0
);

CREATE INDEX idx_file_metadata_md5 ON sys_file_metadata(md5);
CREATE INDEX idx_file_metadata_tenant ON sys_file_metadata(tenant_id);
CREATE INDEX idx_file_metadata_biz ON sys_file_metadata(biz_type, biz_id);

-- File version table
CREATE TABLE sys_file_version (
    id              VARCHAR(36) PRIMARY KEY,
    file_id         VARCHAR(36) NOT NULL,
    version         INTEGER NOT NULL,
    storage_key     VARCHAR(500) NOT NULL,
    file_size       BIGINT,
    md5             VARCHAR(32),
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tenant_id       BIGINT NOT NULL
);

CREATE INDEX idx_file_version_file_id ON sys_file_version(file_id);

-- Upload task table
CREATE TABLE sys_upload_task (
    id              VARCHAR(36) PRIMARY KEY,
    file_name       VARCHAR(255) NOT NULL,
    file_size       BIGINT NOT NULL,
    md5             VARCHAR(32),
    storage_key     VARCHAR(500),
    total_parts     INTEGER,
    completed_parts INTEGER DEFAULT 0,
    status          SMALLINT NOT NULL DEFAULT 0,
    upload_id       VARCHAR(100),
    tenant_id       BIGINT NOT NULL,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER DEFAULT 0,
    lock_version    INTEGER DEFAULT 0
);

CREATE INDEX idx_upload_task_status ON sys_upload_task(status);
CREATE INDEX idx_upload_task_md5 ON sys_upload_task(md5);
