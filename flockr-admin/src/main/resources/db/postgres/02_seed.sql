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
  ('SOURCE',
'ATHENA',
'AWS Athena',
   '{"type": "object", "properties": {"query": {"type": "string"}, "database": {"type": "string"}, "region": {"type": "string"}, "accessKey": {"type": "string"}, "secretKey": {"type": "string"}, "catalog": {"type": "string"}}, "required": ["query"]}',
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
  ('SOURCE',
'KAFKA',
'Apache Kafka (Source)',
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
-- Insert sample data sources (configs match KafkaSourceConfig and AthenaSourceConfig POJOs)
insert
	into
	data_sources (name,
	type_id,
	config,
	status,
	created_by)
values
(
  'User Events Kafka Source',
  (
select
	id
from
	data_connector_types
where
	kind = 'SOURCE'
	and type = 'KAFKA'
limit 1),
  '{"connectorType": "KAFKA", "topic": "user-events", "bootstrapServersUrl": "localhost:9092"}',
  'ACTIVE',
  'system'
),
(
  'Customer Data Athena Source',
  (
select
	id
from
	data_connector_types
where
	kind = 'SOURCE'
	and type = 'ATHENA'
limit 1),
  '{"connectorType": "ATHENA", "query": "SELECT * FROM customer_data WHERE active = true", "database": "flockr_db", "region": "us-east-1", "catalog": "AwsDataCatalog"}',
  'ACTIVE',
  'system'
);

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
  'Audience Events Kafka Sink',
  (
select
	id
from
	data_connector_types
where
	kind = 'SINK'
	and type = 'KAFKA'
limit 1),
  '{"connectorType": "KAFKA", "topic": "audience-events", "bootstrapServersUrl": "localhost:9092"}',
  'ACTIVE',
  'system'
),
(
  'S3 Data Lake Sink',
  (
select
	id
from
	data_connector_types
where
	kind = 'SINK'
	and type = 'S3_FOLDER'
limit 1),
  '{"connectorType": "S3_FOLDER", "bucket": "flockr-data-lake", "folderPath": "audiences/export", "region": "us-west-2", "accessKey": "AKIAIOSFODNN7EXAMPLE", "secretKey": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", "fileFormat": "json"}',
  'ACTIVE',
  'system'
),
(
  'S3 Parquet Export Sink',
  (
select
	id
from
	data_connector_types
where
	kind = 'SINK'
	and type = 'S3_FOLDER'
limit 1),
  '{"connectorType": "S3_FOLDER", "bucket": "flockr-exports", "folderPath": "parquet/audiences", "region": "us-east-1", "accessKey": "AKIAIOSFODNN7EXAMPLE", "secretKey": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", "fileFormat": "parquet"}',
  'ACTIVE',
  'system'
);