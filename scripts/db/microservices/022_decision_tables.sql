-- Decision rules (SpEL expressions)
CREATE TABLE IF NOT EXISTS sys_decision_rule (
    id          VARCHAR(64) PRIMARY KEY,
    engine_type VARCHAR(32) NOT NULL,
    scene       VARCHAR(64),
    rule_type   VARCHAR(16) NOT NULL,
    expression  TEXT NOT NULL,
    description VARCHAR(256),
    enabled     BOOLEAN DEFAULT TRUE,
    priority    INT DEFAULT 100,
    version     INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT NOW(),
    update_time TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_rule_engine_type ON sys_decision_rule(engine_type);
CREATE INDEX idx_rule_scene ON sys_decision_rule(scene);

-- Weight profiles (versioned)
CREATE TABLE IF NOT EXISTS sys_weight_profile (
    id          VARCHAR(64) PRIMARY KEY,
    engine_type VARCHAR(32) NOT NULL,
    scene       VARCHAR(64),
    version     INT NOT NULL,
    weights     JSONB NOT NULL,
    conditions  JSONB,
    active      BOOLEAN DEFAULT FALSE,
    create_time TIMESTAMP DEFAULT NOW(),
    UNIQUE(engine_type, scene, version)
);

CREATE INDEX idx_weight_active ON sys_weight_profile(engine_type, scene, active);

-- A/B experiments
CREATE TABLE IF NOT EXISTS sys_decision_experiment (
    id          VARCHAR(64) PRIMARY KEY,
    engine_type VARCHAR(32) NOT NULL,
    name        VARCHAR(128),
    status      VARCHAR(16) DEFAULT 'DRAFT',
    start_time  TIMESTAMP,
    end_time    TIMESTAMP,
    create_time TIMESTAMP DEFAULT NOW()
);

-- Experiment variants
CREATE TABLE IF NOT EXISTS sys_decision_variant (
    id                VARCHAR(64) PRIMARY KEY,
    experiment_id     VARCHAR(64) NOT NULL,
    name              VARCHAR(64),
    traffic_percent   INT NOT NULL,
    weight_profile_id VARCHAR(64),
    overrides         JSONB,
    FOREIGN KEY (experiment_id) REFERENCES sys_decision_experiment(id)
);

-- Decision feedback
CREATE TABLE IF NOT EXISTS sys_decision_feedback (
    id            VARCHAR(64) PRIMARY KEY,
    engine_type   VARCHAR(32) NOT NULL,
    decision_id   VARCHAR(64) NOT NULL,
    outcome       VARCHAR(16) NOT NULL,
    metrics       JSONB,
    context       JSONB,
    create_time   TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_feedback_engine ON sys_decision_feedback(engine_type);
CREATE INDEX idx_feedback_decision ON sys_decision_feedback(decision_id);
