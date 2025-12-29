-- noinspection SqlNoDataSourceInspectionForFile
SET MODE MYSQL; /* another h2 way to set mode */

------------------------------------------------------------------ tables
DROP TABLE IF EXISTS dec_namespace;
CREATE TABLE dec_namespace
(
    id          bigint PRIMARY KEY AUTO_INCREMENT,
    revision    int NOT NULL DEFAULT 0,
    name        varchar(128) NOT NULL,
    owners      varchar(512),
    type        varchar(50),
    create_by   varchar(50),
    update_by   varchar(50),
    create_date timestamp,
    update_date timestamp
);

DROP TABLE IF EXISTS dec_physical_asset;
CREATE TABLE dec_physical_asset
(
    id              bigint PRIMARY KEY AUTO_INCREMENT,
    revision        int NOT NULL DEFAULT 0,
    asset_type      varchar(50) NOT NULL,
    asset_name      varchar(1200),
    asset_details   longtext,
    storage_environment     varchar(50),
    dec_environment       varchar(50),
    create_by       varchar(50),
    update_by       varchar(50),
    create_date     timestamp,
    update_date     timestamp
);

DROP TABLE IF EXISTS dec_physical_storage;
CREATE TABLE dec_physical_storage
(
    id              bigint PRIMARY KEY AUTO_INCREMENT,
    revision        int NOT NULL DEFAULT 0,
    access_type     varchar(50) NOT NULL,
    storage_details longtext,
    storage_environment     varchar(50),
    storage_context    varchar(50),
    asset_id        bigint,
    create_by       varchar(50),
    update_by       varchar(50),
    create_date     timestamp,
    update_date     timestamp
);

DROP TABLE IF EXISTS dec_pipeline;
CREATE TABLE dec_pipeline
(
    id              bigint PRIMARY KEY AUTO_INCREMENT,
    revision        int NOT NULL DEFAULT 0,
    pipeline_id     varchar(50) NOT NULL,
    workspace_id    varchar(50),
    name            varchar(255),
    tasks           varchar(1200),
    code            longtext,
    create_by       varchar(50),
    update_by       varchar(50),
    create_date     timestamp,
    update_date     timestamp
);

DROP TABLE IF EXISTS dec_physical_storage_pipeline_mapping;
CREATE TABLE dec_physical_storage_pipeline_mapping
(
    id                  bigint PRIMARY KEY AUTO_INCREMENT,
    pipeline_id         bigint NOT NULL,
    storage_id          bigint NOT NULL,
    CONSTRAINT fk_mapping_to_physical_storage FOREIGN KEY (storage_id) REFERENCES dec_physical_storage (id),
    CONSTRAINT fk_mapping_to_pipeline FOREIGN KEY (pipeline_id) REFERENCES dec_pipeline (id)
);

DROP TABLE IF EXISTS dec_ldm_change_request;
CREATE TABLE dec_ldm_change_request
(
    id              bigint PRIMARY KEY AUTO_INCREMENT,
    revision        int NOT NULL DEFAULT 0,
    action_type     varchar(50),
    action_target   varchar(50),
    request_details longtext,
    status          varchar(50),
    log_records     varchar(1200),
    create_by       varchar(50),
    update_by       varchar(50),
    create_date     timestamp,
    update_date     timestamp
);

DROP TABLE IF EXISTS dec_ldm_base_entity;
CREATE TABLE dec_ldm_base_entity
(
    id                 bigint PRIMARY KEY AUTO_INCREMENT,
    revision           int NOT NULL DEFAULT 0,
    name               varchar(128) NOT NULL,
    namespace_id       bigint,
    description        text,
    owners             VARCHAR(255),
    jira_project       VARCHAR(50),
    domain             VARCHAR(100),
    pk                 varchar(255),
    team               varchar(128),
    team_dl            varchar(255),
    create_by          varchar(50),
    update_by          varchar(50),
    create_date        timestamp,
    update_date        timestamp
);

DROP TABLE IF EXISTS dec_physical_asset_ldm_mapping;
CREATE TABLE dec_physical_asset_ldm_mapping
(
    id               bigint PRIMARY KEY AUTO_INCREMENT,
    asset_id         bigint NOT NULL,
    ldm_base_entity_id     bigint NOT NULL,
    CONSTRAINT fk_mapping_to_physical_asset FOREIGN KEY (asset_id) REFERENCES dec_physical_asset (id),
    CONSTRAINT fk_mapping_to_ldm FOREIGN KEY (ldm_base_entity_id) REFERENCES dec_ldm_base_entity (id)
);

DROP TABLE IF EXISTS dec_ldm_entity_index;
CREATE TABLE dec_ldm_entity_index
(
    id                 bigint PRIMARY KEY AUTO_INCREMENT,
    revision           int NOT NULL DEFAULT 0,
    base_entity_id     bigint NOT NULL,
    name               varchar(128), -- to remove
    view_type          varchar(50),
    current_version    int NOT NULL,
    create_by          varchar(50),
    update_by          varchar(50),
    create_date        timestamp,
    update_date        timestamp
);

DROP TABLE IF EXISTS dec_ldm_entity; -- could rename to dec_ldm
CREATE TABLE dec_ldm_entity
(
    id            bigint NOT NULL,
    version       int NOT NULL,
    revision      int NOT NULL DEFAULT 0,
    base_entity_id     bigint NOT NULL,
    name          varchar(128) NOT NULL,
    view_type     varchar(50),
    description   text,
    owners        VARCHAR(255),
    jira_project  VARCHAR(50),
    domain        VARCHAR(100),
    pk            varchar(255),
    namespace_id  bigint,
    upstream_ldm  varchar(1200),
    code_language             varchar(50),
    code_content              longtext,
    generated_sql             longtext,
    ir                        longtext, -- to be changed to binary
    language_frontend_version varchar(128),
    status        varchar(50),
    environment   varchar(50),
    request_id    bigint,
    team          varchar(128),
    team_dl       varchar(255),
    udfs          varchar(1200),
    is_dcs        BOOLEAN,
    dcs_fields    varchar(1200),
    create_by     varchar(50),
    update_by     varchar(50),
    create_date   timestamp,
    update_date   timestamp,
    primary key (id, version),
    CONSTRAINT fk_ldm_entity_version_to_ldm_entity FOREIGN KEY (id) REFERENCES dec_ldm_entity_index (id),
    CONSTRAINT fk_ldm_entity_version_to_namespace FOREIGN KEY (namespace_id) REFERENCES dec_namespace (id)
);

DROP TABLE IF EXISTS dec_ldm_field;
CREATE TABLE dec_ldm_field
(
    id                       bigint PRIMARY KEY AUTO_INCREMENT,
    revision                 int NOT NULL DEFAULT 0,
    ldm_entity_id            bigint NOT NULL,
    ldm_version              int NOT NULL,
    name                     varchar(128) NOT NULL,
    hierarchical_name        varchar(1200),
    description              text,
    data_type                varchar(1200),
    data_schema              text,
    value_function           text,
    signal_filter            varchar(1200),
    is_derived_field         tinyint(1) DEFAULT 0,
    derived_field_expression text,
    ordinal                  int,
    create_by                varchar(50),
    update_by                varchar(50),
    create_date              timestamp,
    update_date              timestamp,
    value_function_online    text,
    value_function_offline   text,
    CONSTRAINT fk_ldm_field_to_ldm_entity_version FOREIGN KEY (ldm_entity_id, ldm_version) REFERENCES dec_ldm_entity (id, version)
);

DROP TABLE IF EXISTS dec_ldm_field_physical_storage_mapping;
CREATE TABLE dec_ldm_field_physical_storage_mapping
(
    id                           bigint PRIMARY KEY AUTO_INCREMENT,
    revision                     int NOT NULL DEFAULT 0,
    ldm_field_id                 bigint NOT NULL,
    physical_storage_id          bigint NOT NULL,
    physical_field_expression    text,
    -- data_availability_start_time timestamp,
    -- data_availability_end_time   timestamp,
    create_by                    varchar(50),
    update_by                    varchar(50),
    create_date                  timestamp,
    update_date                  timestamp,
    CONSTRAINT fk_ldm_field_storage_mapping_to_ldm_field FOREIGN KEY (ldm_field_id) REFERENCES dec_ldm_field (id),
    CONSTRAINT fk_ldm_field_storage_mapping_to_ldm_physical_storage FOREIGN KEY (physical_storage_id) REFERENCES dec_physical_storage (id)
);

DROP TABLE IF EXISTS dec_ldm_field_signal_mapping;
CREATE TABLE dec_ldm_field_signal_mapping
(
    id                                     bigint PRIMARY KEY AUTO_INCREMENT,
    revision                               int NOT NULL DEFAULT 0,
    ldm_field_id                           bigint NOT NULL,
    signal_def_id                          bigint NOT NULL,
    signal_version                         int NOT NULL,
    signal_name                            varchar(128),
    signal_type                            varchar(50),
    signal_field_name                      varchar(128) NOT NULL,
    signal_field_expression                text,
    signal_field_expression_online         text,
    signal_field_expression_offline        text,
    signal_field_latency                   varchar(32),
    create_by                              varchar(50),
    update_by                              varchar(50),
    create_date                            timestamp,
    update_date                            timestamp,
    CONSTRAINT fk_ldm_field_signal_mapping_to_ldm_field FOREIGN KEY (ldm_field_id) REFERENCES dec_ldm_field (id)
);


DROP TABLE IF EXISTS dec_dataset_index;
CREATE TABLE dec_dataset_index
(
    id                 bigint PRIMARY KEY AUTO_INCREMENT,
    revision           int NOT NULL DEFAULT 0,
    name               varchar(128),
    current_version    int,
    create_by          varchar(50),
    update_by          varchar(50),
    create_date        timestamp,
    update_date        timestamp
);

DROP TABLE IF EXISTS dec_dataset;
CREATE TABLE dec_dataset
(
    id                        bigint NOT NULL,
    version                   int NOT NULL,
    revision                  int NOT NULL DEFAULT 0,
    ldm_entity_id             bigint NOT NULL,
    ldm_version               int NOT NULL,
    name                      varchar(128) NOT NULL,
    owners                    varchar(255),
    status                    varchar(50) NOT NULL,
    access_details            longtext,
    ir                        longtext,
    consumption_parameters    longtext,
    runtime_configurations    longtext,
    namespace_id              bigint,
    is_dcs                    BOOLEAN,
    environment               varchar(50),
    create_by                 varchar(50),
    update_by                 varchar(50),
    create_date               timestamp,
    update_date               timestamp,
    primary key (id, version),
    CONSTRAINT fk_dataset_to_dataset_index FOREIGN KEY (id) REFERENCES dec_dataset_index (id),
    CONSTRAINT fk_dataset_to_ldm_entity FOREIGN KEY (ldm_entity_id, ldm_version) REFERENCES dec_ldm_entity (id, version)
);

DROP TABLE IF EXISTS dec_dataset_deployment;
CREATE TABLE dec_dataset_deployment
(
    id                  bigint PRIMARY KEY AUTO_INCREMENT,
    revision            int NOT NULL DEFAULT 0,
    dataset_id          bigint NOT NULL,
    dataset_version     int NOT NULL,
    environment         varchar(50),
    status              varchar(50),
    CONSTRAINT fk_dataset_deployment_mapping_to_dataset FOREIGN KEY (dataset_id, dataset_version) REFERENCES dec_dataset (id, version)
);

DROP TABLE IF EXISTS dec_dataset_physical_storage_mapping;
CREATE TABLE dec_dataset_physical_storage_mapping
(
    id                  bigint PRIMARY KEY AUTO_INCREMENT,
    revision            int NOT NULL DEFAULT 0,
    dataset_id          bigint NOT NULL,
    dataset_version     int NOT NULL,
    physical_storage_id bigint NOT NULL,
    status              varchar(50),
    create_by           varchar(50),
    update_by           varchar(50),
    create_date         timestamp,
    update_date         timestamp,
    CONSTRAINT fk_dataset_storage_mapping_to_dataset FOREIGN KEY (dataset_id, dataset_version) REFERENCES dec_dataset (id, version),
    CONSTRAINT fk_dataset_storage_mapping_to_physical_storage FOREIGN KEY (physical_storage_id) REFERENCES dec_physical_storage (id)
);

DROP TABLE IF EXISTS dec_ldm_error_handling_storage_mapping;
CREATE TABLE dec_ldm_error_handling_storage_mapping
(
    id                  bigint PRIMARY KEY AUTO_INCREMENT,
    revision            int NOT NULL DEFAULT 0,
    physical_storage_id bigint NOT NULL,
    ldm_entity_id       bigint NOT NULL,
    ldm_version         int NOT NULL,
    create_by           varchar(50),
    update_by           varchar(50),
    create_date         timestamp,
    update_date         timestamp,
    CONSTRAINT fk_error_handling_mapping_to_ldm_entity FOREIGN KEY (ldm_entity_id, ldm_version) REFERENCES dec_ldm_entity (id, version),
    CONSTRAINT fk_error_handling_mapping_to_physical_storage FOREIGN KEY (physical_storage_id) REFERENCES dec_physical_storage (id)
);

DROP TABLE IF EXISTS dec_physical_asset_infra;
CREATE TABLE dec_physical_asset_infra
(
    id                   bigint PRIMARY KEY AUTO_INCREMENT,
    revision             int NOT NULL DEFAULT 0,
    infra_type           varchar(50) NOT NULL,
    property_type        varchar(50) NOT NULL,
    property_details     longtext,
    platform_environment varchar(50),
    create_by            varchar(50),
    update_by            varchar(50),
    create_date          timestamp,
    update_date          timestamp
);

DROP TABLE IF EXISTS dec_physical_asset_infra_mapping;
CREATE TABLE dec_physical_asset_infra_mapping
(
    id                   bigint PRIMARY KEY AUTO_INCREMENT,
    revision             int NOT NULL DEFAULT 0,
    asset_id             bigint NOT NULL,
    asset_infra_id       bigint NOT NULL,
    create_by            varchar(50),
    update_by            varchar(50),
    create_date          timestamp,
    update_date          timestamp,
    CONSTRAINT fk_mapping_to_physical_asset_infra FOREIGN KEY (asset_id) REFERENCES dec_physical_asset (id),
    CONSTRAINT fk_mapping_to_physical_asset_infrastructure FOREIGN KEY (asset_infra_id) REFERENCES dec_physical_asset_infra (id)
);

DROP TABLE IF EXISTS dec_physical_asset_infra_global_property;
CREATE TABLE dec_physical_asset_infra_global_property
(
    id                   bigint PRIMARY KEY AUTO_INCREMENT,
    revision             int NOT NULL DEFAULT 0,
    infra_type           varchar(50) NOT NULL,
    property_type        varchar(50) NOT NULL,
    property_details     text,
    create_by            varchar(50),
    update_by            varchar(50),
    create_date          timestamp,
    update_date          timestamp,
    UNIQUE (infra_type, property_type)
);

DROP TABLE IF EXISTS dec_physical_asset_attributes;
CREATE TABLE dec_physical_asset_attributes
(
    id bigint NOT NULL AUTO_INCREMENT,
    revision int NOT NULL DEFAULT '0',
    asset_id bigint NOT NULL,
    attribute_name varchar(1200) NOT NULL,
    attribute_value longtext NOT NULL,
    create_by varchar(50) DEFAULT NULL,
    update_by varchar(50) DEFAULT NULL,
    create_date timestamp NULL DEFAULT NULL,
    update_date timestamp NULL DEFAULT NULL,
    CONSTRAINT fk_mapping_to_physical_asset_attributes FOREIGN KEY (asset_id) REFERENCES dec_physical_asset (id)
);