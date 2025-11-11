-- Seed supported connector types
INSERT INTO data_connector_types (kind, type, display_name, config_schema, is_active)
VALUES
  ('SOURCE', 'ATHENA', 'AWS Athena', NULL, 1)
ON DUPLICATE KEY UPDATE display_name=VALUES(display_name), is_active=VALUES(is_active);

INSERT INTO data_connector_types (kind, type, display_name, config_schema, is_active)
VALUES
  ('SOURCE', 'KAFKA', 'Apache Kafka (Source)', NULL, 1)
ON DUPLICATE KEY UPDATE display_name=VALUES(display_name), is_active=VALUES(is_active);

INSERT INTO data_connector_types (kind, type, display_name, config_schema, is_active)
VALUES
  ('SINK', 'KAFKA', 'Apache Kafka (Sink)', NULL, 1)
ON DUPLICATE KEY UPDATE display_name=VALUES(display_name), is_active=VALUES(is_active);


