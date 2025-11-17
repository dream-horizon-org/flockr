-- Flockr Database Schema for PostgreSQL

-- Create schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS flockr;

CREATE TABLE data_connectors (
	tenant_id VARCHAR(255) NOT NULL,
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL,
    config JSONB,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE data_sinks (
	tenant_id VARCHAR(255) NOT NULL,
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL,
    config JSONB,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE data_sources (
	tenant_id VARCHAR(255) NOT NULL,
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL,
    config JSONB,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audiences (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    project_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE' NOT NULL,
    sinks BIGINT[] NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_audience_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_count BIGINT DEFAULT 0,
    custom_audience_config JSONB,
    type VARCHAR(50) DEFAULT 'STANDARD' NOT NULL,
    verified BOOLEAN DEFAULT FALSE,
    expire_date TIMESTAMP not NULL,
    name_vector tsvector,
    UNIQUE (tenant_id, project_id, name)
);

CREATE INDEX idx_audiences_name_vector ON audiences USING GIN(name_vector);
CREATE INDEX idx_audiences_tenant_project ON audiences(tenant_id, project_id);

CREATE TABLE rules (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    audience_id BIGINT NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    project_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    rule_action VARCHAR(50) NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'SCHEDULED' NOT NULL,
    configuration JSONB NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (audience_id) REFERENCES audiences(id) ON DELETE CASCADE
);

CREATE INDEX idx_rules_audience_id ON rules(audience_id);
CREATE INDEX idx_rules_tenant_project ON rules(tenant_id, project_id);
CREATE INDEX idx_rules_audience_tenant_project ON rules(audience_id, tenant_id, project_id);