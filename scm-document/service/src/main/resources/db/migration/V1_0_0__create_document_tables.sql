-- Document platform tables (scm-document / db_document)

-- file domain: catalog/reference of document-platform artifacts (bytes live in scm-file)
CREATE TABLE doc_file_metadata (
    id            VARCHAR(36) PRIMARY KEY,
    original_name VARCHAR(255) NOT NULL,
    source_type   VARCHAR(30),
    ref_id        VARCHAR(36),
    file_ref      VARCHAR(36) NOT NULL,
    tenant_id     BIGINT NOT NULL,
    create_by     VARCHAR(36),
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted       INTEGER DEFAULT 0
);
CREATE INDEX idx_doc_file_tenant ON doc_file_metadata(tenant_id);
CREATE INDEX idx_doc_file_ref ON doc_file_metadata(ref_id);
CREATE INDEX idx_doc_file_file_ref ON doc_file_metadata(file_ref);

-- template domain
CREATE TABLE doc_template (
    id              VARCHAR(36) PRIMARY KEY,
    template_code   VARCHAR(100) NOT NULL,
    template_name   VARCHAR(255) NOT NULL,
    template_type   VARCHAR(50),
    category        VARCHAR(50),
    file_format     VARCHAR(20),
    status          VARCHAR(20) DEFAULT 'DRAFT',
    current_version INTEGER DEFAULT 1,
    description     VARCHAR(500),
    tenant_id       BIGINT NOT NULL,
    created_by      VARCHAR(36),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER DEFAULT 0
);
CREATE INDEX idx_doc_template_tenant ON doc_template(tenant_id);
CREATE INDEX idx_doc_template_code ON doc_template(template_code, tenant_id);

CREATE TABLE doc_template_version (
    id         VARCHAR(36) PRIMARY KEY,
    template_id VARCHAR(36) NOT NULL,
    version    INTEGER NOT NULL,
    file_id    VARCHAR(36),
    schema_id  VARCHAR(36),
    status     VARCHAR(20) DEFAULT 'DRAFT',
    checksum   VARCHAR(64),
    created_by VARCHAR(36),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    tenant_id  BIGINT NOT NULL
);
CREATE INDEX idx_doc_tpl_ver_template ON doc_template_version(template_id);

-- template variable schema (drives render-time data validation)
CREATE TABLE doc_template_schema (
    id           VARCHAR(36) PRIMARY KEY,
    template_id  VARCHAR(36) NOT NULL,
    schema_code  VARCHAR(100),
    name         VARCHAR(255),
    description  VARCHAR(500),
    variables    TEXT,
    status       VARCHAR(20) DEFAULT 'DRAFT',
    tenant_id    BIGINT NOT NULL,
    created_by   VARCHAR(36),
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_doc_schema_template ON doc_template_schema(template_id);

-- document domain
CREATE TABLE doc_document (
    id               VARCHAR(36) PRIMARY KEY,
    doc_code         VARCHAR(100) NOT NULL,
    doc_name         VARCHAR(255) NOT NULL,
    template_id      VARCHAR(36),
    template_version INTEGER,
    current_version  INTEGER DEFAULT 1,
    status           VARCHAR(20) DEFAULT 'GENERATED',
    business_type    VARCHAR(50),
    business_id      VARCHAR(100),
    tenant_id        BIGINT NOT NULL,
    created_by       VARCHAR(36),
    create_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted          INTEGER DEFAULT 0
);
CREATE INDEX idx_doc_document_tenant ON doc_document(tenant_id);
CREATE INDEX idx_doc_document_biz ON doc_document(business_type, business_id);

CREATE TABLE doc_document_version (
    id                  VARCHAR(36) PRIMARY KEY,
    document_id         VARCHAR(36) NOT NULL,
    version             INTEGER NOT NULL,
    file_id             VARCHAR(36),
    render_params_checksum VARCHAR(64),
    create_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tenant_id           BIGINT NOT NULL
);
CREATE INDEX idx_doc_doc_ver_document ON doc_document_version(document_id);

CREATE TABLE doc_document_audit (
    id            VARCHAR(36) PRIMARY KEY,
    document_id   VARCHAR(36) NOT NULL,
    operation     VARCHAR(30) NOT NULL,
    operator_id   VARCHAR(36),
    operator_name VARCHAR(100),
    ip            VARCHAR(64),
    user_agent    VARCHAR(255),
    before_version INTEGER,
    after_version  INTEGER,
    trace_id      VARCHAR(64),
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tenant_id     BIGINT NOT NULL
);
CREATE INDEX idx_doc_doc_audit_document ON doc_document_audit(document_id);
