-- Data connector master table: supported data sources and sinks
CREATE TABLE IF NOT EXISTS data_connector_types (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  kind ENUM('SOURCE','SINK') NOT NULL,
  type VARCHAR(64) NOT NULL,
  display_name VARCHAR(128) NOT NULL,
  config_schema JSON NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_kind_type (kind, type)
);

-- Onboarded data sources (instances configured by users)
CREATE TABLE IF NOT EXISTS data_sources (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  type_id BIGINT NOT NULL,
  config JSON NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_by VARCHAR(128) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_data_sources_type FOREIGN KEY (type_id) REFERENCES data_connector_types(id)
);

-- Onboarded data sinks (instances configured by users)
CREATE TABLE IF NOT EXISTS data_sinks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  type_id BIGINT NOT NULL,
  config JSON NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_by VARCHAR(128) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_data_sinks_type FOREIGN KEY (type_id) REFERENCES data_connector_types(id)
);

CREATE TABLE IF NOT EXISTS audience (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id VARCHAR(64) NOT NULL,
  project_id VARCHAR(64) NOT NULL,
  name VARCHAR(255) NOT NULL,
  description VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  last_audience_updated_at TIMESTAMP NULL,
  user_count BIGINT NULL,
  custom_audience_config JSON NULL,
  type VARCHAR(64) NULL,
  verified TINYINT(1) NULL DEFAULT 0,
  rules_count INT NULL DEFAULT 0,
  expiry_date TIMESTAMP NULL,
  KEY idx_tenant_id (tenant_id),
  KEY idx_project_id (project_id)
);


CREATE TABLE IF NOT EXISTS audience_sinks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  audience_id BIGINT NOT NULL,
  sink_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_audience_sinks_audience FOREIGN KEY (audience_id) REFERENCES audience(id) ON DELETE CASCADE,
  CONSTRAINT fk_audience_sinks_sink FOREIGN KEY (sink_id) REFERENCES data_sinks(id) ON DELETE CASCADE,
  UNIQUE KEY uk_audience_sink (audience_id, sink_id)
);