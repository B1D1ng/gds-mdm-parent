-- noinspection SqlNoDataSourceInspectionForFile
SET MODE MYSQL; /* another h2 way to set mode */

CREATE TABLE IF NOT EXISTS unstaged_contract (
    id BIGINT AUTO_INCREMENT,
    version INT DEFAULT 1 NOT NULL,
    revision INT DEFAULT 0 NOT NULL,
    name VARCHAR(255) NOT NULL,
    owners VARCHAR(255) NOT NULL,
    entity_type VARCHAR(50),
    domain VARCHAR(50) NOT NULL,
    dl VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    environment VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    comment VARCHAR(2048),
    create_by VARCHAR(50) NOT NULL,
    create_date TIMESTAMP NOT NULL,
    update_by VARCHAR(50) NOT NULL,
    update_date TIMESTAMP NOT NULL,
    PRIMARY KEY (id, version)
);

CREATE TABLE IF NOT EXISTS routing (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    revision INT DEFAULT 0 NOT NULL,
    contract_id BIGINT NOT NULL,
    contract_version INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    latency VARCHAR(50),
    readiness_time TIME,
    frequency VARCHAR(50),
    retention_period VARCHAR(50),
    create_by VARCHAR(50) NOT NULL,
    create_date TIMESTAMP NOT NULL,
    update_by VARCHAR(50) NOT NULL,
    update_date TIMESTAMP NOT NULL,
    FOREIGN KEY (contract_id, contract_version) REFERENCES unstaged_contract(id, version)
);

CREATE TABLE IF NOT EXISTS component (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    revision INT DEFAULT 0 NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    owners VARCHAR(255) NOT NULL,
    entity_type VARCHAR(50),
    dl VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    create_by VARCHAR(50) NOT NULL,
    create_date TIMESTAMP NOT NULL,
    update_by VARCHAR(50) NOT NULL,
    update_date TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS kafka_source (
    id BIGINT PRIMARY KEY,
    metadata_id VARCHAR(50) NOT NULL,
    connector_type VARCHAR(30) NOT NULL,
    schema_subject_name VARCHAR(50),
    FOREIGN KEY (id) REFERENCES component(id)
);

CREATE TABLE IF NOT EXISTS kafka_sink (
    id BIGINT PRIMARY KEY,
    metadata_id VARCHAR(50) NOT NULL,
    connector_type VARCHAR(30) NOT NULL,
    schema_subject_name VARCHAR(50),
    FOREIGN KEY (id) REFERENCES component(id)
);

CREATE TABLE IF NOT EXISTS transformer (
    id BIGINT PRIMARY KEY,
    FOREIGN KEY (id) REFERENCES component(id)
);

CREATE TABLE IF NOT EXISTS routing_component_mapping (
    routing_id BIGINT NOT NULL,
    component_id BIGINT NOT NULL,
    order_index INT DEFAULT 0 NOT NULL,
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    FOREIGN KEY (routing_id) REFERENCES routing(id),
    FOREIGN KEY (component_id) REFERENCES component(id)
);

CREATE TABLE IF NOT EXISTS pipeline (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    revision INT DEFAULT 0 NOT NULL,
    contract_id BIGINT NOT NULL,
    contract_version INT NOT NULL,
    dls_pipeline_id VARCHAR(50),
    environment VARCHAR(20) NOT NULL,
    deploy_scope VARCHAR(20) NOT NULL,
    create_by VARCHAR(50) NOT NULL,
    create_date TIMESTAMP NOT NULL,
    update_by VARCHAR(50) NOT NULL,
    update_date TIMESTAMP NOT NULL,
    FOREIGN KEY (contract_id, contract_version) REFERENCES unstaged_contract(id, version),
    UNIQUE (contract_id, contract_version, deploy_scope, environment)
);

-- Create transformation table
CREATE TABLE IF NOT EXISTS transformation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    revision INT DEFAULT 0 NOT NULL,
    component_id BIGINT NOT NULL,
    field VARCHAR(255) NOT NULL,
    expression_type VARCHAR(50) NOT NULL,
    expression TEXT NOT NULL,
    field_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    owners VARCHAR(255) NOT NULL,
    create_by VARCHAR(50) NOT NULL,
    create_date TIMESTAMP NOT NULL,
    update_by VARCHAR(50) NOT NULL,
    update_date TIMESTAMP NOT NULL,
    FOREIGN KEY (component_id) REFERENCES transformer(id)
);

-- Create udf_alias table
CREATE TABLE udf_alias (
    id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    func VARCHAR(255) NOT NULL,
    alias VARCHAR(255) NOT NULL,
    PRIMARY KEY (id, func, alias),
    FOREIGN KEY (id) REFERENCES transformer(id)
);

-- Create filter table
CREATE TABLE IF NOT EXISTS filter (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    revision INT DEFAULT 0 NOT NULL,
    component_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    statement VARCHAR(2048) NOT NULL,
    create_by VARCHAR(50) NOT NULL,
    create_date TIMESTAMP NOT NULL,
    update_by VARCHAR(50) NOT NULL,
    update_date TIMESTAMP NOT NULL,
    FOREIGN KEY (component_id) REFERENCES transformer(id)
);

CREATE TABLE IF NOT EXISTS entity_lookup (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    revision INT DEFAULT 0 NOT NULL,
    name VARCHAR(255) UNIQUE NOT NULL,
    readable_name VARCHAR(255) UNIQUE NOT NULL,
    create_by VARCHAR(50) NOT NULL,
    create_date TIMESTAMP NOT NULL,
    update_by VARCHAR(50) NOT NULL,
    update_date TIMESTAMP NOT NULL
);

CREATE TABLE contract_column (
    id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    comment VARCHAR(255),
    extra_opts VARCHAR(255),
    FOREIGN KEY (id) REFERENCES component(id)
);

--- History tables ---
CREATE TABLE IF NOT EXISTS contract_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    revision INT DEFAULT 0 NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    owners VARCHAR(255),
    entity_type VARCHAR(50) NOT NULL,
    dl VARCHAR(255) NOT NULL,
    environment VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    create_by VARCHAR(50) NOT NULL,
    create_date TIMESTAMP NOT NULL,
    update_by VARCHAR(50) NOT NULL,
    update_date TIMESTAMP NOT NULL,
    original_id BIGINT NOT NULL,
    original_version INT NOT NULL,
    original_revision INT NOT NULL,
    original_create_date TIMESTAMP NOT NULL,
    original_update_date TIMESTAMP NOT NULL,
    change_type VARCHAR(10) NOT NULL,
    change_reason VARCHAR(50)
);

-- New Tables
CREATE TABLE IF NOT EXISTS bes_source (
    id BIGINT PRIMARY KEY,
    metadata_id VARCHAR(50) NOT NULL,
    connector_type VARCHAR(30) NOT NULL,
    schema_subject_name VARCHAR(50),
    FOREIGN KEY (id) REFERENCES component(id)
);

CREATE TABLE IF NOT EXISTS streaming_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    component_id BIGINT,
    revision INT DEFAULT 0,
    group_id VARCHAR(255),
    env VARCHAR(255),
    schema_id BIGINT,
    format VARCHAR(255),
    scan_startup_mode VARCHAR(255),
    stream_name VARCHAR(255),
    properties json,
    topic_pattern VARCHAR(255),
    create_by VARCHAR(50) NOT NULL,
    create_date TIMESTAMP NOT NULL,
    update_by VARCHAR(50) NOT NULL,
    update_date TIMESTAMP NOT NULL,
    FOREIGN KEY (component_id) REFERENCES component(id),
    UNIQUE (env, component_id)
);

CREATE TABLE IF NOT EXISTS streaming_config_topic (
    id BIGINT,
    topic VARCHAR(255) NOT NULL,
    FOREIGN KEY (id) REFERENCES streaming_config(id)
);

-- To be dropped after migration
CREATE TABLE IF NOT EXISTS kafka_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    component_id BIGINT,
    revision INT DEFAULT 0,
    group_id VARCHAR(255),
    env VARCHAR(255),
    schema_id BIGINT,
    format VARCHAR(255),
    scan_startup_mode VARCHAR(255),
    stream_name VARCHAR(255),
    properties json,
    create_by VARCHAR(50) NOT NULL,
    create_date TIMESTAMP NOT NULL,
    update_by VARCHAR(50) NOT NULL,
    update_date TIMESTAMP NOT NULL,
    FOREIGN KEY (component_id) REFERENCES component(id),
    UNIQUE (env, component_id)
);

CREATE TABLE IF NOT EXISTS kafka_config_topic (
    id BIGINT,
    topic VARCHAR(255) NOT NULL,
    FOREIGN KEY (id) REFERENCES kafka_config(id)
);

CREATE TABLE IF NOT EXISTS ldm_view_sink(
    id BIGINT PRIMARY KEY,
    view_id BIGINT NOT NULL,
    FOREIGN KEY (id) REFERENCES component(id)
);

DROP TABLE IF EXISTS hive_source;
CREATE TABLE hive_source (
    id BIGINT PRIMARY KEY,
    FOREIGN KEY (id) REFERENCES component(id)
);

DROP TABLE IF EXISTS hive_config;
CREATE TABLE hive_config (
     id BIGINT AUTO_INCREMENT PRIMARY KEY,
     revision INT DEFAULT 0 NOT NULL,
     component_id BIGINT NOT NULL,
     environment VARCHAR(50),
     create_by VARCHAR(50) NOT NULL,
     create_date TIMESTAMP NOT NULL,
     update_by VARCHAR(50) NOT NULL,
     update_date TIMESTAMP NOT NULL,
     FOREIGN KEY (component_id) REFERENCES hive_source(id)
);

DROP TABLE IF EXISTS hive_storage;
CREATE TABLE hive_storage (
      id BIGINT AUTO_INCREMENT PRIMARY KEY,
      revision INT DEFAULT 0 NOT NULL,
      db_name VARCHAR(255) NOT NULL,
      table_name VARCHAR(255) NOT NULL,
      data_center VARCHAR(50) NOT NULL,
      format VARCHAR(255) NOT NULL,
      primary_keys VARCHAR(255),
      partition_columns VARCHAR(255),
      done_file_type VARCHAR(50),
      done_file_path VARCHAR(255),
      create_by VARCHAR(50) NOT NULL,
      create_date TIMESTAMP NOT NULL,
      update_by VARCHAR(50) NOT NULL,
      update_date TIMESTAMP NOT NULL
);

DROP TABLE IF EXISTS config_storage_mapping;
CREATE TABLE config_storage_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    revision INT DEFAULT 0 NOT NULL,
    create_by VARCHAR(50) NOT NULL,
    create_date TIMESTAMP NOT NULL,
    update_by VARCHAR(50) NOT NULL,
    update_date TIMESTAMP NOT NULL,
    config_id BIGINT NOT NULL,
    storage_id BIGINT NOT NULL,
    CONSTRAINT hive_config_mapping_id FOREIGN KEY (config_id) REFERENCES hive_config(id),
    CONSTRAINT hive_storage_mapping_id FOREIGN KEY (storage_id) REFERENCES hive_storage(id),
    UNIQUE (config_id, storage_id)
);

-- View for searching unstaged_contract by config
CREATE OR REPLACE VIEW contract_config_view AS
SELECT
    uc.id AS id,
    uc.version AS version,
    uc.revision as revision,
    uc.name AS name,
    uc.owners AS owners,
    uc.entity_type AS entity_type,
    uc.domain AS domain,
    uc.dl AS dl,
    uc.description AS description,
    uc.environment AS environment,
    uc.status AS status,
    GROUP_CONCAT(DISTINCT c.type ORDER BY c.type SEPARATOR ',') AS source_type,
    GROUP_CONCAT(DISTINCT sct.topic ORDER BY sct.topic SEPARATOR ',') AS topic,
    GROUP_CONCAT(DISTINCT hs.table_name ORDER BY hs.table_name SEPARATOR ',') AS table_name
FROM unstaged_contract uc
INNER JOIN routing r ON r.contract_id = uc.id AND r.contract_version = uc.version
INNER JOIN routing_component_mapping rcm ON rcm.routing_id = r.id
INNER JOIN component c ON c.id = rcm.component_id
LEFT JOIN streaming_config sc ON sc.component_id = c.id
LEFT JOIN streaming_config_topic sct ON sct.id = sc.id
LEFT JOIN hive_config hc ON hc.component_id = c.id
LEFT JOIN config_storage_mapping csm ON csm.config_id = hc.id
LEFT JOIN hive_storage hs ON hs.id = csm.storage_id
WHERE c.type IN ('KafkaSource', 'BesSource', 'HiveSource')
GROUP BY uc.id, uc.version, uc.revision, uc.name, uc.owners, uc.entity_type, uc.domain, uc.dl, uc.description, uc.environment, uc.status;

DROP TABLE IF EXISTS contract_signal_mapping;
CREATE TABLE contract_signal_mapping (
    contract_id BIGINT NOT NULL,
    contract_version INT NOT NULL,
    signal_id BIGINT NOT NULL,
    signal_version INT NOT NULL,
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    CONSTRAINT contract_signal_mapping_unique UNIQUE (contract_id, contract_version, signal_id, signal_version),
    CONSTRAINT contract_mapping_id FOREIGN KEY (contract_id, contract_version) REFERENCES unstaged_contract(id, version),
    CONSTRAINT signal_mapping_id FOREIGN KEY (signal_id, signal_version) REFERENCES signal_definition(id, version)
);