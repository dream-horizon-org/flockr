-- Seed data for Flockr PostgreSQL Database

SET search_path TO flockr, public;

-- Insert sample data sinks
INSERT INTO data_sinks (name, type, config, status) VALUES
('S3 Data Lake', 'S3', '{"bucket": "flockr-data", "region": "us-west-2"}', 'ACTIVE'),
('Kafka Stream', 'KAFKA', '{"brokers": ["localhost:9092"], "topic": "audience-events"}', 'ACTIVE');

-- Insert sample data connectors
INSERT INTO data_connectors (name, type, config, status) VALUES
('User Events Connector', 'KAFKA', '{"broker": ["localhost:9092"], "topic": "user-events"}', 'ACTIVE'),
('Analytics Connector', 'POSTGRES', '{"host": "localhost", "port": 5432, "database": "analytics"}', 'ACTIVE');

-- Insert sample audiences
INSERT INTO audiences (name, description, status, data_sink_id) VALUES
('High Value Users', 'Users with high engagement and purchase history', 'ACTIVE', 1),
('Inactive Users', 'Users who have not been active in the last 30 days', 'ACTIVE', 2),
('Premium Subscribers', 'Users with active premium subscriptions', 'ACTIVE', 3);

-- Insert sample audience rules
INSERT INTO audience_rules (audience_id, rule_type, rule_config, priority, status) VALUES
(1, 'ENGAGEMENT', '{"minEvents": 100, "timeWindow": "30d"}', 1, 'ACTIVE'),
(1, 'PURCHASE', '{"minAmount": 500, "currency": "USD"}', 2, 'ACTIVE'),
(2, 'INACTIVITY', '{"days": 30}', 1, 'ACTIVE'),
(3, 'SUBSCRIPTION', '{"tier": "premium", "status": "active"}', 1, 'ACTIVE');

-- Verify data
SELECT 'Data Sinks' as table_name, COUNT(*) as count FROM data_sinks
UNION ALL
SELECT 'Data Connectors', COUNT(*) FROM data_connectors
UNION ALL
SELECT 'Audiences', COUNT(*) FROM audiences
UNION ALL
SELECT 'Audience Rules', COUNT(*) FROM audience_rules;

