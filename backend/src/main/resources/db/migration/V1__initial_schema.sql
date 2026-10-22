-- VORZAK Database Initial Schema Migration
-- Flyway V1: Core Tables, Relationships, and Indexes

CREATE TABLE IF NOT EXISTS investigation (
    id UUID PRIMARY KEY,
    input_type VARCHAR(32) NOT NULL,
    input_sha256 VARCHAR(64) NOT NULL,
    claim_text TEXT NOT NULL,
    state VARCHAR(32) NOT NULL,
    mode VARCHAR(16) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP WITH TIME ZONE,
    agent_steps_used INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS task (
    id UUID PRIMARY KEY,
    investigation_id UUID NOT NULL REFERENCES investigation(id) ON DELETE CASCADE,
    channel VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    tinyfish_run_id VARCHAR(128),
    started_at TIMESTAMP WITH TIME ZONE,
    deadline_at TIMESTAMP WITH TIME ZONE,
    attempts INT NOT NULL DEFAULT 0,
    error_text TEXT
);

CREATE TABLE IF NOT EXISTS evidence (
    id UUID PRIMARY KEY,
    investigation_id UUID NOT NULL REFERENCES investigation(id) ON DELETE CASCADE,
    kind VARCHAR(32) NOT NULL,
    source_url TEXT,
    retrieved_at TIMESTAMP WITH TIME ZONE NOT NULL,
    content_sha256 VARCHAR(64) NOT NULL,
    file_path TEXT,
    extraction_method VARCHAR(64),
    source_tier VARCHAR(32),
    date_value TIMESTAMP WITH TIME ZONE,
    date_source VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN',
    mode VARCHAR(16) NOT NULL,
    raw_json JSONB
);

CREATE TABLE IF NOT EXISTS comparison (
    id UUID PRIMARY KEY,
    investigation_id UUID NOT NULL REFERENCES investigation(id) ON DELETE CASCADE,
    submitted_evidence_id UUID NOT NULL REFERENCES evidence(id),
    candidate_evidence_id UUID NOT NULL REFERENCES evidence(id),
    alignment_status VARCHAR(32) NOT NULL,
    inlier_count INT,
    ssim_normalized DOUBLE PRECISION,
    diff_region_count INT,
    diff_image_path TEXT,
    params_json JSONB
);

CREATE TABLE IF NOT EXISTS finding (
    id UUID PRIMARY KEY,
    investigation_id UUID NOT NULL REFERENCES investigation(id) ON DELETE CASCADE,
    rule_id VARCHAR(64) NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    confidence VARCHAR(16) NOT NULL,
    explanation TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS finding_evidence (
    finding_id UUID NOT NULL REFERENCES finding(id) ON DELETE CASCADE,
    evidence_id UUID NOT NULL REFERENCES evidence(id) ON DELETE CASCADE,
    PRIMARY KEY (finding_id, evidence_id)
);

CREATE TABLE IF NOT EXISTS event (
    id UUID PRIMARY KEY,
    investigation_id UUID NOT NULL REFERENCES investigation(id) ON DELETE CASCADE,
    ts TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    type VARCHAR(64) NOT NULL,
    payload JSONB
);

-- Indexes for performance and referential lookups
CREATE INDEX IF NOT EXISTS idx_task_investigation_id ON task(investigation_id);
CREATE INDEX IF NOT EXISTS idx_evidence_investigation_id ON evidence(investigation_id);
CREATE INDEX IF NOT EXISTS idx_evidence_content_sha256 ON evidence(content_sha256);
CREATE INDEX IF NOT EXISTS idx_comparison_investigation_id ON comparison(investigation_id);
CREATE INDEX IF NOT EXISTS idx_finding_investigation_id ON finding(investigation_id);
CREATE INDEX IF NOT EXISTS idx_finding_evidence_evidence_id ON finding_evidence(evidence_id);
CREATE INDEX IF NOT EXISTS idx_event_investigation_id ON event(investigation_id);
