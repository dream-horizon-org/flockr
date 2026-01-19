-- Seed data for Flockr PostgreSQL Database

set
search_path to flockr,
public;
-- Seed supported connector types

-- SOURCE: AWS Athena
insert
	into
	data_connector_types (kind,
	type,
	display_name,
	config_schema,
	is_active)
values
  ('SOURCE',
'ATHENA',
'AWS Athena',
   '{"type": "object", "properties": {"accessKey": {"type": "string", "description": "AWS Access Key ID"}, "secretKey": {"type": "string", "description": "AWS Secret Access Key"}, "queryOutputLocation": {"type": "string", "description": "S3 path for query results (e.g., s3://bucket-name/path/)"}, "workgroup": {"type": "string", "description": "Athena workgroup name", "default": "primary"}, "region": {"type": "string", "description": "AWS region (e.g., us-east-1)", "default": "us-east-1"}}, "required": ["accessKey", "secretKey", "queryOutputLocation"]}',
   true)
on
CONFLICT (kind,
type) DO
update
set
	display_name = EXCLUDED.display_name,
	config_schema = EXCLUDED.config_schema,
	is_active = EXCLUDED.is_active;

-- SOURCE: AWS Athena
insert
	into
	data_connector_types (kind,
	type,
	display_name,
	config_schema,
	is_active)
values
  ('SOURCE',
'SESSION ATHENA',
'SESSION AWS Athena',
   '{"type": "object", "properties": {"sessionToken": {"type": "string", "description": "AWS Session Key ID"}, "accessKey": {"type": "string", "description": "AWS Access Key ID"}, "secretKey": {"type": "string", "description": "AWS Secret Access Key"}, "queryOutputLocation": {"type": "string", "description": "S3 path for query results (e.g., s3://bucket-name/path/)"}, "workgroup": {"type": "string", "description": "Athena workgroup name", "default": "primary"}, "region": {"type": "string", "description": "AWS region (e.g., us-east-1)", "default": "us-east-1"}}, "required": ["accessKey", "secretKey", "queryOutputLocation"]}',
   true)
on
CONFLICT (kind,
type) DO
update
set
	display_name = EXCLUDED.display_name,
	config_schema = EXCLUDED.config_schema,
	is_active = EXCLUDED.is_active;

-- SINK: Apache Kafka
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

-- SINK: AWS S3 Folder
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

-- SINK: Webhook/HTTP API
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

-- Insert sample data sources
insert
	into
	data_sources (name,
	type_id,
	config,
	status,
	created_by)
values
(
  'Production Athena - User Events',
  (
select
	id
from
	data_connector_types
where
	kind = 'SOURCE'
	and type = 'ATHENA'
limit 1),
  '{"connectorType": "ATHENA", "accessKey": "ASIA54S6SGWZWNBLF73L", "secretKey": "P3o7Wd3BpeFjIsWcyYWW5iMANim7hSy0upafin2y", "queryOutputLocation": "s3://hascend/athena_out/", "workgroup": "primary", "region": "us-east-1"}',
  'ACTIVE',
  'system'
);

-- Insert sample data sinks
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

-- Lease for ExecuteRuleHandler (executes scheduled rules)
INSERT INTO distributed_lease (lease_key, holder_id, acquired_at, expires_at)
VALUES (
    'executeRuleHandler',
    'system-init',
    NOW() - INTERVAL '1 hour',  -- Set to past so it's immediately available
    NOW() - INTERVAL '55 minutes'  -- Expired so any handler can acquire it
)
ON CONFLICT (lease_key) DO NOTHING;

-- Lease for ReconcileJobHandler (reconciles job states)
INSERT INTO distributed_lease (lease_key, holder_id, acquired_at, expires_at)
VALUES (
    'reconcileJobHandler',
    'system-init',
    NOW() - INTERVAL '1 hour',  -- Set to past so it's immediately available
    NOW() - INTERVAL '55 minutes'  -- Expired so any handler can acquire it
)
ON CONFLICT (lease_key) DO NOTHING;