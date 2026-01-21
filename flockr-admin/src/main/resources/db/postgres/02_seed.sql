-- Seed data for Flockr PostgreSQL Database

set
search_path to flockr,
public;
-- Seed supported connector types

insert
	into
	data_connector_types (kind,
	type,
	display_name,
	config_schema,
	is_active)
values
  ('SINK',
'KAFKA',
'Apache Kafka (Sink)',
   '{"type": "object", "properties": {"topic": {"type": "string"}, "bootstrapServersUrl": {"type": "string"}}, "required": ["topic", "bootstrapServersUrl"]}',
   true)
on
CONFLICT (kind,
type) DO
update
set
	display_name = EXCLUDED.display_name,
	config_schema = EXCLUDED.config_schema,
	is_active = EXCLUDED.is_active;

insert
	into
	data_connector_types (kind,
	type,
	display_name,
	config_schema,
	is_active)
values
  ('SINK',
'S3_FOLDER',
'AWS S3 Folder',
   '{"type": "object", "properties": {"bucket": {"type": "string"}, "folderPath": {"type": "string"}, "region": {"type": "string"}, "accessKey": {"type": "string"}, "secretKey": {"type": "string"}, "fileFormat": {"type": "string"}}, "required": ["bucket", "folderPath", "accessKey", "secretKey"]}',
   true)
on
CONFLICT (kind,
type) DO
update
set
	display_name = EXCLUDED.display_name,
	config_schema = EXCLUDED.config_schema,
	is_active = EXCLUDED.is_active;

insert
	into
	data_connector_types (kind,
	type,
	display_name,
	config_schema,
	is_active)
values
  ('SINK',
'WEBHOOK',
'Webhook/HTTP API',
   '{"type": "object", "properties": {"url": {"type": "string", "description": "The webhook/API URL to call"}, "method": {"type": "string", "enum": ["POST", "PUT", "PATCH"], "default": "POST"}, "headers": {"type": "object", "additionalProperties": {"type": "string"}, "description": "Custom HTTP headers"}, "timeoutMs": {"type": "integer", "default": 30000}, "contentType": {"type": "string", "default": "application/json"}, "batchMode": {"type": "boolean", "default": true}}, "required": ["url"]}',
   true)
on
CONFLICT (kind,
type) DO
update
set
	display_name = EXCLUDED.display_name,
	config_schema = EXCLUDED.config_schema,
	is_active = EXCLUDED.is_active;

-- Insert sample data sinks (configs match KafkaSinkConfig and S3FolderSinkConfig POJOs)
insert
	into
	data_sinks (name,
	type_id,
	config,
	status,
	created_by)
values
(
  'User Cohort Mapping Webhook',
  (
select
	id
from
	data_connector_types
where
	kind = 'SINK'
	and type = 'WEBHOOK'
limit 1),
  '{"connectorType": "WEBHOOK", "url": "http://flockr-users:8080/flockr/users/map-cohorts/batch", "method": "POST", "headers": {"Content-Type": "application/json"}, "timeoutMs": 30000, "contentType": "application/json", "batchMode": true}',
  'ACTIVE',
  'system'
);