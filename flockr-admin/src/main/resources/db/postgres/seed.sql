-- Seed data for Flockr PostgreSQL Database

SET search_path TO flockr, public;

-- Seed supported connector types
INSERT INTO data_connector_types (kind, type, display_name, config_schema, is_active)
VALUES
  ('SOURCE', 'ATHENA', 'AWS Athena', NULL, TRUE)
ON CONFLICT (kind, type) DO UPDATE SET display_name=EXCLUDED.display_name, is_active=EXCLUDED.is_active;

INSERT INTO data_connector_types (kind, type, display_name, config_schema, is_active)
VALUES
  ('SOURCE', 'KAFKA', 'Apache Kafka (Source)', NULL, TRUE)
ON CONFLICT (kind, type) DO UPDATE SET display_name=EXCLUDED.display_name, is_active=EXCLUDED.is_active;

INSERT INTO data_connector_types (kind, type, display_name, config_schema, is_active)
VALUES
  ('SINK', 'KAFKA', 'Apache Kafka (Sink)', NULL, TRUE)
ON CONFLICT (kind, type) DO UPDATE SET display_name=EXCLUDED.display_name, is_active=EXCLUDED.is_active;

INSERT INTO data_connector_types (kind, type, display_name, config_schema, is_active)
VALUES
  ('SINK', 'S3_FOLDER', 'AWS S3 Folder', NULL, TRUE)
ON CONFLICT (kind, type) DO UPDATE SET display_name=EXCLUDED.display_name, is_active=EXCLUDED.is_active;

-- Insert sample data sources (configs match KafkaSourceConfig and AthenaSourceConfig POJOs)
INSERT INTO data_sources (name, type_id, config, status, created_by) VALUES
(
  'User Events Kafka Source',
  (SELECT id FROM data_connector_types WHERE kind = 'SOURCE' AND type = 'KAFKA' LIMIT 1),
  '{"connectorType": "KAFKA", "topic": "user-events", "bootstrapServersUrl": "localhost:9092"}',
  'ACTIVE',
  'system'
),
(
  'Analytics Athena Source',
  (SELECT id FROM data_connector_types WHERE kind = 'SOURCE' AND type = 'ATHENA' LIMIT 1),
  '{"connectorType": "ATHENA", "query": "SELECT user_id, event_type, timestamp FROM analytics.events WHERE date = CURRENT_DATE", "database": "analytics", "region": "us-east-1"}',
  'ACTIVE',
  'system'
);

-- Insert sample data sinks (configs match KafkaSinkConfig and S3FolderSinkConfig POJOs)
INSERT INTO data_sinks (name, type_id, config, status, created_by) VALUES
(
  'Audience Events Kafka Sink',
  (SELECT id FROM data_connector_types WHERE kind = 'SINK' AND type = 'KAFKA' LIMIT 1),
  '{"connectorType": "KAFKA", "topic": "audience-events", "bootstrapServersUrl": "localhost:9092"}',
  'ACTIVE',
  'system'
),
(
  'S3 Data Lake Sink',
  (SELECT id FROM data_connector_types WHERE kind = 'SINK' AND type = 'S3_FOLDER' LIMIT 1),
  '{"connectorType": "S3_FOLDER", "bucket": "flockr-data-lake", "folderPath": "audiences/export", "region": "us-west-2", "fileFormat": "json"}',
  'ACTIVE',
  'system'
),
(
  'S3 Parquet Export Sink',
  (SELECT id FROM data_connector_types WHERE kind = 'SINK' AND type = 'S3_FOLDER' LIMIT 1),
  '{"connectorType": "S3_FOLDER", "bucket": "flockr-exports", "folderPath": "parquet/audiences", "region": "us-east-1", "fileFormat": "parquet"}',
  'ACTIVE',
  'system'
);

