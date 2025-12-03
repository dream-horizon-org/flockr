-- Flockr Database Schema for PostgreSQL

-- Create schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS flockr;

-- Data connector master table: supported data sources and sinks
CREATE TABLE IF NOT EXISTS data_connector_types (
    id BIGSERIAL PRIMARY KEY,
    kind VARCHAR(10) NOT NULL CHECK (kind IN ('SOURCE', 'SINK')),
    type VARCHAR(64) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    config_schema JSONB NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (kind, type)
);

-- Onboarded data sources (instances configured by users)
CREATE TABLE IF NOT EXISTS data_sources (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    type_id BIGINT NOT NULL,
    config JSONB NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_by VARCHAR(128) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_data_sources_type FOREIGN KEY (type_id) REFERENCES data_connector_types(id)
);

-- Onboarded data sinks (instances configured by users)
CREATE TABLE IF NOT EXISTS data_sinks (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    type_id BIGINT NOT NULL,
    config JSONB NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_by VARCHAR(128) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_data_sinks_type FOREIGN KEY (type_id) REFERENCES data_connector_types(id)
);

CREATE TABLE audiences (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    x_project_id VARCHAR(512) NOT NULL,
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
    type VARCHAR(50) NOT NULL CHECK (type IN ('CONDITIONAL', 'STATIC')),
    verified BOOLEAN DEFAULT FALSE,
    expire_date TIMESTAMP not NULL,
    name_vector tsvector,
    UNIQUE (x_project_id, name)
);

CREATE INDEX idx_audiences_name_vector ON audiences USING GIN(name_vector);
CREATE INDEX idx_audiences_x_project_id ON audiences(x_project_id);

CREATE TABLE rules (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    audience_id BIGINT NOT NULL,
    x_project_id VARCHAR(512) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    rule_action VARCHAR(50) NOT NULL,
    rule_type VARCHAR(50) NOT NULL CHECK (rule_type IN ('STREAM', 'BATCH')),
    status VARCHAR(50) NOT NULL,
    configuration JSONB NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (audience_id) REFERENCES audiences(id)
);

CREATE INDEX idx_rules_audience_id ON rules(audience_id);
CREATE INDEX idx_rules_x_project_id ON rules(x_project_id);
CREATE INDEX idx_rules_audience_x_project ON rules(audience_id, x_project_id);


CREATE TABLE audience_owners (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    audience_id BIGINT NOT NULL,
    x_project_id VARCHAR(512) NOT NULL,
    owner_email VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE' NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Foreign key constraint with cascade delete
    CONSTRAINT fk_audience_owners_audience FOREIGN KEY (audience_id) REFERENCES audiences(id) ON DELETE CASCADE,
    
    -- Unique constraint: one active owner record per audience
    CONSTRAINT uq_active_audience_owner UNIQUE (audience_id, owner_email)
);

-- Indexes for performance optimization
CREATE INDEX idx_audience_owners_audience_id ON audience_owners(audience_id);
CREATE INDEX idx_audience_owners_x_project_id ON audience_owners(x_project_id);
CREATE INDEX idx_audience_owners_email ON audience_owners(owner_email);
CREATE INDEX idx_audience_owners_status ON audience_owners(status) WHERE status = 'ACTIVE';
CREATE INDEX idx_audience_owners_lookup ON audience_owners(audience_id, x_project_id, status);