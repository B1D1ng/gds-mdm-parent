-- noinspection SqlNoDataSourceInspectionForFile
SET MODE MYSQL; /* another h2 way to set mode */
------------------------------------------------------------------ Sequence table
CREATE TABLE IF NOT EXISTS sequences (
    sequence_name VARCHAR(100) NOT NULL,
    next_val BIGINT NOT NULL,
    PRIMARY KEY (sequence_name)
);

INSERT INTO sequences (sequence_name, next_val) VALUES ('signal_seq', 3)
    ON DUPLICATE KEY UPDATE next_val = next_val;

------------------------------------------------------------------ lookup table

CREATE TABLE IF NOT EXISTS platform_lookup (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    revision            INT             DEFAULT 0 NOT NULL,
    name                VARCHAR(255)    UNIQUE NOT NULL,
    readable_name       VARCHAR(255)    UNIQUE NOT NULL,
    create_by           VARCHAR(50)     NOT NULL,
    create_date         TIMESTAMP       NOT NULL,
    update_by           VARCHAR(50)     NOT NULL,
    update_date         TIMESTAMP       NOT NULL
    );

------------------------------------------------------------------ Common tables
CREATE TABLE IF NOT EXISTS plan (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    revision            INT             DEFAULT 0 NOT NULL,
    name                VARCHAR(100)    NOT NULL,
    description         VARCHAR(255)    NOT NULL,
    team_dls            VARCHAR(255)    NOT NULL,
    owners              VARCHAR(255)    NOT NULL,
    jira_project        VARCHAR(10)     NOT NULL,
    domain              VARCHAR(100)    NOT NULL,
    platform_id         BIGINT          NOT NULL,
    status              VARCHAR(50)     NOT NULL,
    comment             VARCHAR(2048),
    create_by           VARCHAR(50)     NOT NULL,
    create_date         TIMESTAMP       NOT NULL,
    update_by           VARCHAR(50)     NOT NULL,
    update_date         TIMESTAMP       NOT NULL,
    FOREIGN KEY (platform_id) REFERENCES platform_lookup(id)
);
DROP INDEX IF EXISTS plan_status_idx ON plan;
DROP INDEX IF EXISTS plan_name_idx ON plan;
DROP INDEX IF EXISTS plan_jira_project_idx ON plan;
CREATE INDEX plan_status_idx ON plan (status);
CREATE INDEX plan_name_idx ON plan (name);
CREATE INDEX plan_jira_project_idx ON plan (jira_project);

CREATE TABLE IF NOT EXISTS signal_physical_storage (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    revision            INT             DEFAULT 0 NOT NULL,
    description         VARCHAR(255)    NOT NULL,
    environment         VARCHAR(50)     NOT NULL,
    kafka_topic         VARCHAR(100),
    kafka_schema        JSON,
    hive_table_name     VARCHAR(100),
    done_file_path      VARCHAR(500),
    create_by           VARCHAR(50)     NOT NULL,
    create_date         TIMESTAMP       NOT NULL,
    update_by           VARCHAR(50)     NOT NULL,
    update_date         TIMESTAMP       NOT NULL,
    UNIQUE              (kafka_topic, environment)
);

------------------------------------------------------------------ Template tables
CREATE TABLE IF NOT EXISTS event_template (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    revision            INT             DEFAULT 0 NOT NULL,
    name                VARCHAR(255)    NOT NULL,
    description         VARCHAR(750)    NOT NULL,
    type                VARCHAR(100)    NOT NULL,
    source              VARCHAR(50)     NOT NULL,
    fsm_order           INT             NOT NULL,
    cardinality         INT             NOT NULL,
    surface_type        VARCHAR(50),
    expression          TEXT,
    expression_type     VARCHAR(50),
    is_mandatory        BOOLEAN         NOT NULL,
    create_by           VARCHAR(50)     NOT NULL,
    create_date         TIMESTAMP       NOT NULL,
    update_by           VARCHAR(50)     NOT NULL,
    update_date         TIMESTAMP       NOT NULL
);
DROP INDEX IF EXISTS event_template_name_idx ON event_template;
DROP INDEX IF EXISTS event_template_type_idx ON event_template;
DROP INDEX IF EXISTS event_template_desc_idx ON event_template;
CREATE INDEX event_template_name_idx ON event_template (name);
CREATE INDEX event_template_type_idx ON event_template (type);
CREATE INDEX event_template_desc_idx ON event_template (description);

CREATE TABLE IF NOT EXISTS attribute_template (
    event_template_id   BIGINT          NOT NULL,
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    revision            INT             DEFAULT 0 NOT NULL,
    tag                 VARCHAR(255)    NOT NULL,
    description         VARCHAR(750),
    java_type           VARCHAR(50)     NOT NULL,
    schema_path         TEXT            NOT NULL,
    is_store_in_state   BOOLEAN         NOT NULL,
    create_by           VARCHAR(50)     NOT NULL,
    create_date         TIMESTAMP       NOT NULL,
    update_by           VARCHAR(50)     NOT NULL,
    update_date         TIMESTAMP       NOT NULL,
    FOREIGN KEY (event_template_id)     REFERENCES event_template(id)
);
DROP INDEX IF EXISTS attribute_template_tag_idx ON attribute_template;
DROP INDEX IF EXISTS attribute_template_desc_idx ON attribute_template;
CREATE INDEX attribute_template_tag_idx ON attribute_template (tag);
CREATE INDEX attribute_template_desc_idx ON attribute_template (description);

CREATE TABLE IF NOT EXISTS signal_template (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    revision            INT             DEFAULT 0 NOT NULL,
    name                VARCHAR(255)    NOT NULL,
    description         VARCHAR(750)    NOT NULL,
    domain              VARCHAR(255),
    type                VARCHAR(100)    NOT NULL,
    retention_period    BIGINT,
    completion_status   VARCHAR(50)     NOT NULL,
    platform_id         BIGINT     NOT NULL,
    create_by           VARCHAR(50)     NOT NULL,
    create_date         TIMESTAMP       NOT NULL,
    update_by           VARCHAR(50)     NOT NULL,
    update_date         TIMESTAMP       NOT NULL,
    FOREIGN KEY (platform_id) REFERENCES platform_lookup(id)
);
DROP INDEX IF EXISTS signal_template_name_idx ON signal_template;
DROP INDEX IF EXISTS signal_template_domain_idx ON signal_template;
DROP INDEX IF EXISTS signal_template_type_idx ON signal_template;
DROP INDEX IF EXISTS signal_template_desc_idx ON signal_template;
CREATE INDEX signal_template_name_idx ON signal_template (name);
CREATE INDEX signal_template_domain_idx ON signal_template (domain);
CREATE INDEX signal_template_type_idx ON signal_template (type);
CREATE INDEX signal_template_desc_idx ON signal_template (description);

CREATE TABLE IF NOT EXISTS field_template (
    signal_template_id  BIGINT          NOT NULL,
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    revision            INT             DEFAULT 0 NOT NULL,
    name                VARCHAR(255)    NOT NULL,
    description         VARCHAR(750)    NOT NULL,
    tag                 VARCHAR(255)    NOT NULL,
    java_type           VARCHAR(50)     NOT NULL,
    avro_schema         TEXT            NOT NULL,
    expression          TEXT            NOT NULL,
    expression_type     VARCHAR(50)     NOT NULL,
    is_mandatory        BOOLEAN         NOT NULL,
    is_cached           BOOLEAN,
    create_by           VARCHAR(50)     NOT NULL,
    create_date         TIMESTAMP       NOT NULL,
    update_by           VARCHAR(50)     NOT NULL,
    update_date         TIMESTAMP       NOT NULL,
    FOREIGN KEY         (signal_template_id)    REFERENCES signal_template(id)
);
DROP INDEX IF EXISTS field_template_name_idx ON field_template;
DROP INDEX IF EXISTS field_template_desc_idx ON field_template;
DROP INDEX IF EXISTS field_template_tag_idx ON field_template;
CREATE INDEX field_template_name_idx ON field_template (name);
CREATE INDEX field_template_desc_idx ON field_template (description);
CREATE INDEX field_template_tag_idx ON field_template (tag);

CREATE TABLE IF NOT EXISTS field_attribute_template_map (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    field_id            BIGINT          NOT NULL,
    attribute_id        BIGINT          NOT NULL,
    FOREIGN KEY         (attribute_id)  REFERENCES attribute_template(id),
    FOREIGN KEY         (field_id)      REFERENCES field_template(id),
    UNIQUE              (field_id, attribute_id)
);

CREATE TABLE IF NOT EXISTS signal_event_template_map (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    signal_id           BIGINT          NOT NULL,
    event_id            BIGINT          NOT NULL,
    FOREIGN KEY         (signal_id)     REFERENCES signal_template(id),
    FOREIGN KEY         (event_id)      REFERENCES event_template(id),
    UNIQUE              (signal_id, event_id)
);

------------------------------------------------------------------ Unstaged tables
CREATE TABLE IF NOT EXISTS event (
    id                          BIGINT          AUTO_INCREMENT PRIMARY KEY,
    event_template_source_id    BIGINT,
    event_source_id             BIGINT,
    revision                    INT             DEFAULT 0 NOT NULL,
    name                        VARCHAR(255)    NOT NULL,
    description                 VARCHAR(750)    NOT NULL,
    type                        VARCHAR(100)    NOT NULL,
    source                      VARCHAR(50)     NOT NULL,
    fsm_order                   INT             NOT NULL,
    cardinality                 INT             NOT NULL,
    github_repository_url       VARCHAR(255),
    surface_type                VARCHAR(50),
    expression                  TEXT,
    expression_type             VARCHAR(50)     NOT NULL,
    create_by                   VARCHAR(50)     NOT NULL,
    create_date                 TIMESTAMP       NOT NULL,
    update_by                   VARCHAR(50)     NOT NULL,
    update_date                 TIMESTAMP       NOT NULL
);
DROP INDEX IF EXISTS event_name_idx ON event;
DROP INDEX IF EXISTS event_type_idx ON event;
DROP INDEX IF EXISTS event_desc_idx ON event;
CREATE INDEX event_name_idx ON event (name);
CREATE INDEX event_type_idx ON event (type);
CREATE INDEX event_desc_idx ON event (description);

CREATE TABLE IF NOT EXISTS event_page_map (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    event_id            BIGINT          NOT NULL,
    page_id             BIGINT          NOT NULL, -- we do not store pages in this database (they stored in Oracle)
    FOREIGN KEY         (event_id)      REFERENCES event(id),
    UNIQUE              (event_id, page_id)
);

CREATE TABLE IF NOT EXISTS event_module_map (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    event_id            BIGINT          NOT NULL,
    module_id           BIGINT          NOT NULL, -- we do not store modules in this database (they stored in Oracle)
    FOREIGN KEY         (event_id)      REFERENCES event(id),
    UNIQUE              (event_id, module_id)
);

CREATE TABLE IF NOT EXISTS event_click_map (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    event_id            BIGINT          NOT NULL,
    click_id            BIGINT          NOT NULL, -- we do not store clicks in this database (they stored in Oracle)
    FOREIGN KEY         (event_id)      REFERENCES event(id),
    UNIQUE              (event_id, click_id)
);

CREATE TABLE IF NOT EXISTS template_question (
    id                                      BIGINT          AUTO_INCREMENT PRIMARY KEY,
    revision                                INT             DEFAULT 0 NOT NULL,
    question                                VARCHAR(1024)   NOT NULL,
    description                             VARCHAR(255),
    answer_java_type                        VARCHAR(50)     NOT NULL,
    answer_property_name                    VARCHAR(50),
    answer_property_placeholder             VARCHAR(50),
    answer_property_setter_class            VARCHAR(512),
    is_list                                 BOOLEAN         NOT NULL,
    is_mandatory                            BOOLEAN         NOT NULL,
    create_by                               VARCHAR(50)     NOT NULL,
    create_date                             TIMESTAMP       NOT NULL,
    update_by                               VARCHAR(50)     NOT NULL,
    update_date                             TIMESTAMP       NOT NULL
);

CREATE TABLE IF NOT EXISTS template_question_event_map (
    id                  BIGINT              AUTO_INCREMENT PRIMARY KEY,
    question_id         BIGINT              NOT NULL,
    event_template_id   BIGINT              NOT NULL,
    FOREIGN KEY         (question_id)       REFERENCES template_question(id),
    FOREIGN KEY         (event_template_id) REFERENCES event_template(id),
    UNIQUE              (question_id, event_template_id)
);

CREATE TABLE IF NOT EXISTS attribute (
    event_id            BIGINT          NOT NULL,
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    revision            INT             DEFAULT 0 NOT NULL,
    tag                 VARCHAR(255)    NOT NULL,
    description         VARCHAR(750),
    java_type           VARCHAR(50)     NOT NULL,
    schema_path         TEXT            NOT NULL,
    is_store_in_state   BOOLEAN         NOT NULL,
    create_by           VARCHAR(50)     NOT NULL,
    create_date         TIMESTAMP       NOT NULL,
    update_by           VARCHAR(50)     NOT NULL,
    update_date         TIMESTAMP       NOT NULL,
    FOREIGN KEY         (event_id)      REFERENCES event(id)
);
DROP INDEX IF EXISTS attribute_tag_idx ON attribute;
DROP INDEX IF EXISTS attribute_desc_idx ON attribute;
CREATE INDEX attribute_tag_idx ON attribute (tag);
CREATE INDEX attribute_desc_idx ON attribute (description);

CREATE TABLE IF NOT EXISTS signal_definition (
    plan_id                     BIGINT          NOT NULL,
    signal_template_source_id   BIGINT,
    signal_source_id            BIGINT,
    signal_source_version       INT,
    id                          BIGINT          NOT NULL,
    version                     INT             NOT NULL,
    revision                    INT             DEFAULT 0 NOT NULL,
    name                        VARCHAR(255)    NOT NULL,
    description                 VARCHAR(750)    NOT NULL,
    domain                      VARCHAR(255),
    owners                      VARCHAR(255),
    type                        VARCHAR(100)    NOT NULL,
    retention_period            BIGINT,
    completion_status           VARCHAR(50)     NOT NULL,
    environment                 VARCHAR(50)     NOT NULL,
    platform_id                 BIGINT     NOT NULL,
    uuid_generator_type         VARCHAR(50),
    uuid_generator_expression   VARCHAR(512),
    correlation_id_expression   VARCHAR(2048),
    need_accumulation           BOOLEAN,
    ref_version                 INT,
    legacy_id                   VARCHAR(50),
    udc_data_source             VARCHAR(50)     NOT NULL,
    create_by                   VARCHAR(50)     NOT NULL,
    create_date                 TIMESTAMP       NOT NULL,
    update_by                   VARCHAR(50)     NOT NULL,
    update_date                 TIMESTAMP       NOT NULL,
    PRIMARY KEY (id, version),
    FOREIGN KEY (plan_id)       REFERENCES plan(id),
    FOREIGN KEY (signal_source_id, signal_source_version)  REFERENCES signal_definition(id, version),
    FOREIGN KEY (platform_id) REFERENCES platform_lookup(id)
);
DROP INDEX IF EXISTS signal_name_idx ON signal_definition;
DROP INDEX IF EXISTS signal_domain_idx ON signal_definition;
DROP INDEX IF EXISTS signal_type_idx ON signal_definition;
DROP INDEX IF EXISTS signal_desc_idx ON signal_definition;
DROP INDEX IF EXISTS signal_platform_idx ON signal_definition;
DROP INDEX IF EXISTS signal_template_source_id_idx ON signal_definition;
CREATE INDEX signal_name_idx ON signal_definition (name);
CREATE INDEX signal_domain_idx ON signal_definition (domain);
CREATE INDEX signal_type_idx ON signal_definition (type);
CREATE INDEX signal_desc_idx ON signal_definition (description);
CREATE INDEX signal_platform_idx ON signal_definition (platform_id);
CREATE INDEX signal_template_source_id_idx ON signal_definition (signal_template_source_id);

CREATE TABLE IF NOT EXISTS field (
    signal_id               BIGINT          NOT NULL,
    signal_version          INT             NOT NULL,
    id                      BIGINT          AUTO_INCREMENT PRIMARY KEY,
    revision                INT             DEFAULT 0 NOT NULL,
    name                    VARCHAR(255)    NOT NULL,
    description             VARCHAR(750)    NOT NULL,
    tag                     VARCHAR(255)    NOT NULL,
    java_type               VARCHAR(50)     NOT NULL,
    avro_schema             TEXT            NOT NULL,
    expression              TEXT            NOT NULL,
    expression_type         VARCHAR(50)     NOT NULL,
    is_mandatory            BOOLEAN         NOT NULL,
    is_cached               BOOLEAN,
    event_types             VARCHAR(512)    NOT NULL,
    create_by               VARCHAR(50)     NOT NULL,
    create_date             TIMESTAMP       NOT NULL,
    update_by               VARCHAR(50)     NOT NULL,
    update_date             TIMESTAMP       NOT NULL,
    FOREIGN KEY (signal_id, signal_version) REFERENCES signal_definition(id, version)
);
DROP INDEX IF EXISTS field_name_idx ON field;
DROP INDEX IF EXISTS field_desc_idx ON field;
DROP INDEX IF EXISTS field_tag_idx ON field;
CREATE INDEX field_name_idx ON field (name);
CREATE INDEX field_desc_idx ON field (description);
CREATE INDEX field_tag_idx ON field (tag);

CREATE TABLE IF NOT EXISTS field_attribute_map (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    field_id            BIGINT          NOT NULL,
    attribute_id        BIGINT          NOT NULL,
    FOREIGN KEY         (attribute_id)  REFERENCES attribute(id),
    FOREIGN KEY         (field_id)      REFERENCES field(id),
    UNIQUE              (field_id, attribute_id)
);

CREATE TABLE IF NOT EXISTS signal_event_map (
    id                  BIGINT           AUTO_INCREMENT PRIMARY KEY,
    signal_id           BIGINT           NOT NULL,
    signal_version      INT              NOT NULL,
    event_id            BIGINT           NOT NULL,
    FOREIGN KEY         (signal_id, signal_version) REFERENCES signal_definition(id, version),
    FOREIGN KEY         (event_id)                  REFERENCES event(id),
    UNIQUE              (signal_id, signal_version, event_id)
);

------------------------------------------------------------------ Staged tables

CREATE TABLE IF NOT EXISTS staged_event (
    id                          BIGINT          AUTO_INCREMENT PRIMARY KEY,
    event_template_source_id    BIGINT,
    event_source_id             BIGINT,
    revision                    INT             DEFAULT 0 NOT NULL,
    name                        VARCHAR(255)    NOT NULL,
    description                 VARCHAR(750)    NOT NULL,
    type                        VARCHAR(100)    NOT NULL,
    source                      VARCHAR(50)     NOT NULL,
    fsm_order                   INT             NOT NULL,
    cardinality                 INT             NOT NULL,
    github_repository_url       VARCHAR(255),
    surface_type                VARCHAR(50),
    expression                  TEXT,
    expression_type             VARCHAR(50)     NOT NULL,
    create_by                   VARCHAR(50)     NOT NULL,
    create_date                 TIMESTAMP       NOT NULL,
    update_by                   VARCHAR(50)     NOT NULL,
    update_date                 TIMESTAMP       NOT NULL
);
DROP INDEX IF EXISTS staged_event_name_idx ON staged_event;
DROP INDEX IF EXISTS staged_event_type_idx ON staged_event;
DROP INDEX IF EXISTS staged_event_desc_idx ON staged_event;
CREATE INDEX staged_event_name_idx ON staged_event (name);
CREATE INDEX staged_event_type_idx ON staged_event (type);
CREATE INDEX staged_event_desc_idx ON staged_event (description);

CREATE TABLE IF NOT EXISTS staged_event_page_map (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    event_id            BIGINT          NOT NULL,
    page_id             BIGINT          NOT NULL, -- we do not store pages in this database (they stored in Oracle)
    FOREIGN KEY         (event_id)      REFERENCES staged_event(id),
    UNIQUE              (event_id, page_id)
);

CREATE TABLE IF NOT EXISTS staged_event_module_map (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    event_id            BIGINT          NOT NULL,
    module_id           BIGINT          NOT NULL, -- we do not store modules in this database (they stored in Oracle)
    FOREIGN KEY         (event_id)      REFERENCES staged_event(id),
    UNIQUE              (event_id, module_id)
);

CREATE TABLE IF NOT EXISTS staged_event_click_map (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    event_id            BIGINT          NOT NULL,
    click_id            BIGINT          NOT NULL, -- we do not store clicks in this database (they stored in Oracle)
    FOREIGN KEY         (event_id)      REFERENCES staged_event(id),
    UNIQUE              (event_id, click_id)
);

CREATE TABLE IF NOT EXISTS staged_attribute (
    event_id            BIGINT          NOT NULL,
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    revision            INT             DEFAULT 0 NOT NULL,
    tag                 VARCHAR(750)    NOT NULL,
    description         VARCHAR(750),
    java_type           VARCHAR(50)     NOT NULL,
    schema_path         TEXT            NOT NULL,
    is_store_in_state   BOOLEAN         NOT NULL,
    create_by           VARCHAR(50)     NOT NULL,
    create_date         TIMESTAMP       NOT NULL,
    update_by           VARCHAR(50)     NOT NULL,
    update_date         TIMESTAMP       NOT NULL,
    FOREIGN KEY (event_id)   REFERENCES staged_event(id)
);
DROP INDEX IF EXISTS staged_attribute_tag_idx ON staged_attribute;
DROP INDEX IF EXISTS staged_attribute_desc_idx ON staged_attribute;
CREATE INDEX staged_attribute_tag_idx ON staged_attribute (tag);
CREATE INDEX staged_attribute_desc_idx ON staged_attribute (description);

CREATE TABLE IF NOT EXISTS staged_signal (
    plan_id                     BIGINT          NOT NULL,
    signal_template_source_id   BIGINT,
    signal_source_id            BIGINT,
    signal_source_version       INT,
    id                          BIGINT          NOT NULL,
    version                     INT             NOT NULL,
    revision                    INT             DEFAULT 0 NOT NULL,
    name                        VARCHAR(255)    NOT NULL,
    description                 VARCHAR(750)    NOT NULL,
    domain                      VARCHAR(255),
    owners                      VARCHAR(255),
    type                        VARCHAR(100)    NOT NULL,
    retention_period            BIGINT,
    completion_status           VARCHAR(50)     NOT NULL,
    environment                 VARCHAR(50)     NOT NULL,
    platform_id                 BIGINT     NOT NULL,
    uuid_generator_type         VARCHAR(50),
    uuid_generator_expression   VARCHAR(512),
    correlation_id_expression   VARCHAR(2048),
    need_accumulation           BOOLEAN,
    ref_version                 INT,
    legacy_id                   VARCHAR(50),
    udc_data_source             VARCHAR(50)     NOT NULL,
    create_by                   VARCHAR(50)     NOT NULL,
    create_date                 TIMESTAMP       NOT NULL,
    update_by                   VARCHAR(50)     NOT NULL,
    update_date                 TIMESTAMP       NOT NULL,
    PRIMARY KEY (id, version),
    FOREIGN KEY (plan_id)       REFERENCES plan(id),
    FOREIGN KEY (platform_id) REFERENCES platform_lookup(id)
);
DROP INDEX IF EXISTS staged_signal_name_idx ON staged_signal;
DROP INDEX IF EXISTS staged_signal_domain_idx ON staged_signal;
DROP INDEX IF EXISTS staged_signal_type_idx ON staged_signal;
DROP INDEX IF EXISTS staged_signal_desc_idx ON staged_signal;
DROP INDEX IF EXISTS staged_signal_platform_idx ON staged_signal;
DROP INDEX IF EXISTS staged_signal_environment_idx ON staged_signal;
DROP INDEX IF EXISTS staged_signal_udc_data_source_idx ON staged_signal;
CREATE INDEX staged_signal_name_idx ON staged_signal (name);
CREATE INDEX staged_signal_domain_idx ON staged_signal (domain);
CREATE INDEX staged_signal_type_idx ON staged_signal (type);
CREATE INDEX staged_signal_desc_idx ON staged_signal (description);
CREATE INDEX staged_signal_environment_idx ON staged_signal (environment);
CREATE INDEX staged_signal_udc_data_source_idx ON staged_signal (udc_data_source);

CREATE TABLE IF NOT EXISTS staged_field (
    signal_id               BIGINT          NOT NULL,
    signal_version          INT             NOT NULL,
    id                      BIGINT          AUTO_INCREMENT PRIMARY KEY,
    revision                INT             DEFAULT 0 NOT NULL,
    name                    VARCHAR(255)    NOT NULL,
    description             VARCHAR(750)    NOT NULL,
    tag                     VARCHAR(255)    NOT NULL,
    java_type               VARCHAR(50)     NOT NULL,
    avro_schema             TEXT            NOT NULL,
    expression              TEXT            NOT NULL,
    expression_type         VARCHAR(50)     NOT NULL,
    is_mandatory            BOOLEAN         NOT NULL,
    is_cached               BOOLEAN,
    event_types             VARCHAR(512)    NOT NULL,
    create_by               VARCHAR(50)     NOT NULL,
    create_date             TIMESTAMP       NOT NULL,
    update_by               VARCHAR(50)     NOT NULL,
    update_date             TIMESTAMP       NOT NULL,
    FOREIGN KEY (signal_id, signal_version) REFERENCES staged_signal(id, version)
);
DROP INDEX IF EXISTS staged_field_name_idx ON staged_field;
DROP INDEX IF EXISTS staged_field_desc_idx ON staged_field;
DROP INDEX IF EXISTS staged_field_tag_idx ON staged_field;
CREATE INDEX staged_field_name_idx ON staged_field (name);
CREATE INDEX staged_field_description_idx ON staged_field (description);
CREATE INDEX staged_field_tag_idx ON staged_field (tag);

CREATE TABLE IF NOT EXISTS staged_field_attribute_map (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    field_id            BIGINT          NOT NULL,
    attribute_id        BIGINT          NOT NULL,
    FOREIGN KEY         (attribute_id)  REFERENCES staged_attribute(id),
    FOREIGN KEY         (field_id)      REFERENCES staged_field(id),
    UNIQUE              (field_id, attribute_id)
);

CREATE TABLE IF NOT EXISTS staged_signal_event_map (
    id                  BIGINT           AUTO_INCREMENT PRIMARY KEY,
    signal_id           BIGINT           NOT NULL,
    signal_version      INT              NOT NULL,
    event_id            BIGINT           NOT NULL,
    FOREIGN KEY         (signal_id, signal_version) REFERENCES staged_signal(id, version),
    FOREIGN KEY         (event_id)                  REFERENCES staged_event(id),
    UNIQUE              (signal_id, signal_version, event_id)
);

------------------------------------------------------------------ History tables
CREATE TABLE IF NOT EXISTS plan_history (
    id                      BIGINT          AUTO_INCREMENT PRIMARY KEY,
    revision                INT             DEFAULT 0 NOT NULL,
    name                    VARCHAR(100)    NOT NULL,
    description             VARCHAR(255)    NOT NULL,
    team_dls                VARCHAR(255)    NOT NULL,
    owners                  VARCHAR(255)    NOT NULL,
    jira_project            VARCHAR(10)     NOT NULL,
    domain                  VARCHAR(100)    NOT NULL,
    status                  VARCHAR(50)     NOT NULL,
    comment                 VARCHAR(50),
    create_by               VARCHAR(50)     NOT NULL,
    create_date             TIMESTAMP       NOT NULL,
    update_by               VARCHAR(50)     NOT NULL,
    update_date             TIMESTAMP       NOT NULL,
    original_id             BIGINT          NOT NULL,
    original_revision       INT             NOT NULL,
    original_create_date    TIMESTAMP       NOT NULL,
    original_update_date    TIMESTAMP       NOT NULL,
    change_type             VARCHAR(10)     NOT NULL,
    change_reason           VARCHAR(50)
);
DROP INDEX IF EXISTS original_id_idx ON plan_history;
CREATE INDEX original_id_idx ON plan_history (original_id);

CREATE TABLE IF NOT EXISTS event_template_history (
    id                      BIGINT          AUTO_INCREMENT PRIMARY KEY,
    revision                INT             DEFAULT 0 NOT NULL,
    name                    VARCHAR(255)    NOT NULL,
    description             VARCHAR(750)    NOT NULL,
    type                    VARCHAR(100)    NOT NULL,
    source                  VARCHAR(50)     NOT NULL,
    fsm_order               INT             NOT NULL,
    cardinality             INT             NOT NULL,
    surface_type            VARCHAR(50),
    expression              TEXT,
    expression_type         VARCHAR(50),
    create_by               VARCHAR(50)     NOT NULL,
    create_date             TIMESTAMP       NOT NULL,
    update_by               VARCHAR(50)     NOT NULL,
    update_date             TIMESTAMP       NOT NULL,
    original_id             BIGINT          NOT NULL,
    original_revision       INT             NOT NULL,
    original_create_date    TIMESTAMP       NOT NULL,
    original_update_date    TIMESTAMP       NOT NULL,
    change_type             VARCHAR(10)     NOT NULL,
    change_reason           VARCHAR(50)
);
DROP INDEX IF EXISTS original_id_idx ON event_template_history;
CREATE INDEX original_id_idx ON event_template_history (original_id);

CREATE TABLE IF NOT EXISTS signal_template_history (
    id                      BIGINT          AUTO_INCREMENT PRIMARY KEY,
    revision                INT             DEFAULT 0 NOT NULL,
    name                    VARCHAR(255)    NOT NULL,
    description             VARCHAR(750)    NOT NULL,
    domain                  VARCHAR(255),
    type                    VARCHAR(100)    NOT NULL,
    retention_period        BIGINT,
    completion_status       VARCHAR(50)     NOT NULL,
    platform_id             BIGINT     NOT NULL,
    create_by               VARCHAR(50)     NOT NULL,
    create_date             TIMESTAMP       NOT NULL,
    update_by               VARCHAR(50)     NOT NULL,
    update_date             TIMESTAMP       NOT NULL,
    original_id             BIGINT          NOT NULL,
    original_revision       INT             NOT NULL,
    original_create_date    TIMESTAMP       NOT NULL,
    original_update_date    TIMESTAMP       NOT NULL,
    change_type             VARCHAR(10)     NOT NULL,
    change_reason           VARCHAR(50)
);
DROP INDEX IF EXISTS original_id_idx ON signal_template_history;
CREATE INDEX original_id_idx ON signal_template_history (original_id);

CREATE TABLE IF NOT EXISTS event_history (
    id                      BIGINT          AUTO_INCREMENT PRIMARY KEY,
    revision                INT             DEFAULT 0 NOT NULL,
    name                    VARCHAR(255)    NOT NULL,
    description             VARCHAR(750)    NOT NULL,
    type                    VARCHAR(100)    NOT NULL,
    source                  VARCHAR(50)     NOT NULL,
    fsm_order               INT             NOT NULL,
    cardinality             INT             NOT NULL,
    github_repository_url   VARCHAR(255),
    surface_type            VARCHAR(50),
    expression              TEXT,
    expression_type         VARCHAR(50)     NOT NULL,
    create_by               VARCHAR(50)     NOT NULL,
    create_date             TIMESTAMP       NOT NULL,
    update_by               VARCHAR(50)     NOT NULL,
    update_date             TIMESTAMP       NOT NULL,
    original_id             BIGINT          NOT NULL,
    original_revision       INT             NOT NULL,
    original_create_date    TIMESTAMP       NOT NULL,
    original_update_date    TIMESTAMP       NOT NULL,
    change_type             VARCHAR(10)     NOT NULL,
    change_reason           VARCHAR(50)
);
DROP INDEX IF EXISTS original_id_idx ON event_history;
CREATE INDEX original_id_idx ON event_history (original_id);

CREATE TABLE IF NOT EXISTS signal_history (
    plan_id                     BIGINT          NOT NULL,
    signal_template_source_id   BIGINT,
    signal_source_id            BIGINT,
    id                          BIGINT          AUTO_INCREMENT PRIMARY KEY,
    revision                    INT             DEFAULT 0 NOT NULL,
    name                        VARCHAR(255)    NOT NULL,
    description                 VARCHAR(750)    NOT NULL,
    domain                      VARCHAR(255),
    owners                      VARCHAR(255),
    type                        VARCHAR(100)    NOT NULL,
    retention_period            BIGINT,
    completion_status           VARCHAR(50)     NOT NULL,
    platform_id                 BIGINT     NOT NULL,
    environment                 VARCHAR(50)     NOT NULL,
    create_by                   VARCHAR(50)     NOT NULL,
    create_date                 TIMESTAMP       NOT NULL,
    update_by                   VARCHAR(50)     NOT NULL,
    update_date                 TIMESTAMP       NOT NULL,
    original_id                 BIGINT          NOT NULL,
    original_version            INT             NOT NULL,
    original_revision           INT             NOT NULL,
    ref_version                 INT,
    original_create_date        TIMESTAMP       NOT NULL,
    original_update_date        TIMESTAMP       NOT NULL,
    change_type                 VARCHAR(10)     NOT NULL,
    change_reason               VARCHAR(50)
);
DROP INDEX IF EXISTS original_id_idx ON signal_history;
CREATE INDEX original_id_idx ON signal_history (original_id, original_version);

CREATE TABLE IF NOT EXISTS field_history (
    signal_id               BIGINT          NOT NULL,
    id                      BIGINT          AUTO_INCREMENT PRIMARY KEY,
    revision                INT             DEFAULT 0 NOT NULL,
    name                    VARCHAR(255)    NOT NULL,
    description             VARCHAR(750)    NOT NULL,
    tag                     VARCHAR(255),
    java_type               VARCHAR(50)     NOT NULL,
    avro_schema             TEXT            NOT NULL,
    expression              TEXT            NOT NULL,
    expression_type         VARCHAR(50)     NOT NULL,
    is_mandatory            BOOLEAN         NOT NULL,
    is_cached               BOOLEAN,
    create_by               VARCHAR(50)     NOT NULL,
    create_date             TIMESTAMP       NOT NULL,
    update_by               VARCHAR(50)     NOT NULL,
    update_date             TIMESTAMP       NOT NULL,
    original_id             BIGINT          NOT NULL,
    original_revision       INT             NOT NULL,
    original_create_date    TIMESTAMP       NOT NULL,
    original_update_date    TIMESTAMP       NOT NULL,
    change_type             VARCHAR(10)     NOT NULL,
    change_reason           VARCHAR(50)
);
DROP INDEX IF EXISTS original_id_idx ON field_history;
CREATE INDEX original_id_idx ON field_history (original_id);

CREATE TABLE IF NOT EXISTS attribute_history (
    event_id                BIGINT          NOT NULL,
    id                      BIGINT          AUTO_INCREMENT PRIMARY KEY,
    revision                INT             DEFAULT 0 NOT NULL,
    tag                     VARCHAR(255)    NOT NULL,
    description             VARCHAR(750),
    java_type               VARCHAR(50)     NOT NULL,
    schema_path             TEXT            NOT NULL,
    is_store_in_state       BOOLEAN         NOT NULL,
    create_by               VARCHAR(50)     NOT NULL,
    create_date             TIMESTAMP       NOT NULL,
    update_by               VARCHAR(50)     NOT NULL,
    update_date             TIMESTAMP       NOT NULL,
    original_id             BIGINT          NOT NULL,
    original_revision       INT             NOT NULL,
    original_create_date    TIMESTAMP       NOT NULL,
    original_update_date    TIMESTAMP       NOT NULL,
    change_type             VARCHAR(10)     NOT NULL,
    change_reason           VARCHAR(50)
);
DROP INDEX IF EXISTS original_id_idx ON attribute_history;
CREATE INDEX original_id_idx ON attribute_history (original_id);

------------------------------------------------------------------ Lookup tables

CREATE TABLE IF NOT EXISTS channel_id_lookup (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    revision            INT             DEFAULT 0 NOT NULL,
    name                VARCHAR(255)    UNIQUE NOT NULL,
    readable_name       VARCHAR(255)    UNIQUE NOT NULL,
    channel_id          INT             UNIQUE NOT NULL,
    create_by           VARCHAR(50)     NOT NULL,
    create_date         TIMESTAMP       NOT NULL,
    update_by           VARCHAR(50)     NOT NULL,
    update_date         TIMESTAMP       NOT NULL
    );

CREATE TABLE IF NOT EXISTS signal_type_lookup (
    id                      BIGINT          AUTO_INCREMENT PRIMARY KEY,
    revision                INT             DEFAULT 0 NOT NULL,
    name                    VARCHAR(255)    UNIQUE NOT NULL,
    readable_name           VARCHAR(255)    UNIQUE NOT NULL,
    platform_id             BIGINT     NOT NULL,
    logical_data_entity     VARCHAR(100)    NOT NULL,
    create_by               VARCHAR(50)     NOT NULL,
    create_date             TIMESTAMP       NOT NULL,
    update_by               VARCHAR(50)     NOT NULL,
    update_date             TIMESTAMP       NOT NULL,
    FOREIGN KEY (platform_id) REFERENCES platform_lookup(id)
);
DROP INDEX IF EXISTS signal_type_lookup_platform_idx ON signal_type_lookup;
CREATE INDEX signal_type_lookup_platform_idx ON signal_type_lookup (platform_id);

CREATE TABLE IF NOT EXISTS signal_type_physical_storage_map (
    id                      BIGINT          AUTO_INCREMENT PRIMARY KEY,
    signal_type_id          BIGINT          NOT NULL,
    physical_storage_id     BIGINT          NOT NULL,
    FOREIGN KEY             (signal_type_id)        REFERENCES signal_type_lookup(id),
    FOREIGN KEY             (physical_storage_id)   REFERENCES signal_physical_storage(id),
    UNIQUE                  (signal_type_id, physical_storage_id)
);

CREATE TABLE IF NOT EXISTS signal_dim_type_lookup (
    id                      BIGINT          AUTO_INCREMENT PRIMARY KEY,
    revision                INT             DEFAULT 0 NOT NULL,
    name                    VARCHAR(255)    UNIQUE NOT NULL,
    readable_name           VARCHAR(255)    UNIQUE NOT NULL,
    create_by               VARCHAR(50)     NOT NULL,
    create_date             TIMESTAMP       NOT NULL,
    update_by               VARCHAR(50)     NOT NULL,
    update_date             TIMESTAMP       NOT NULL
);

CREATE TABLE IF NOT EXISTS signal_dim_type_map (
    id                      BIGINT          AUTO_INCREMENT PRIMARY KEY,
    signal_type_id          BIGINT          NOT NULL,
    dimension_id            BIGINT          NOT NULL,
    is_mandatory            BOOLEAN         NOT NULL,
    FOREIGN KEY             (signal_type_id) REFERENCES signal_type_lookup(id),
    FOREIGN KEY             (dimension_id)   REFERENCES signal_dim_type_lookup(id),
    UNIQUE                  (signal_type_id, dimension_id)
);

CREATE TABLE IF NOT EXISTS event_type_lookup (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    revision            INT             DEFAULT 0 NOT NULL,
    name                VARCHAR(255)    UNIQUE NOT NULL,
    readable_name       VARCHAR(255)    UNIQUE NOT NULL,
    create_by           VARCHAR(50)     NOT NULL,
    create_date         TIMESTAMP       NOT NULL,
    update_by           VARCHAR(50)     NOT NULL,
    update_date         TIMESTAMP       NOT NULL
);

CREATE TABLE IF NOT EXISTS surface_type_lookup (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    revision            INT             DEFAULT 0 NOT NULL,
    name                VARCHAR(255)    UNIQUE NOT NULL,
    readable_name       VARCHAR(255)    UNIQUE NOT NULL,
    create_by           VARCHAR(50)     NOT NULL,
    create_date         TIMESTAMP       NOT NULL,
    update_by           VARCHAR(50)     NOT NULL,
    update_date         TIMESTAMP       NOT NULL
);

CREATE TABLE IF NOT EXISTS business_outcome_lookup (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    revision            INT             DEFAULT 0 NOT NULL,
    name                VARCHAR(255)    UNIQUE NOT NULL,
    readable_name       VARCHAR(255)    UNIQUE NOT NULL,
    event_type          VARCHAR(255)    NOT NULL,
    create_by           VARCHAR(50)     NOT NULL,
    create_date         TIMESTAMP       NOT NULL,
    update_by           VARCHAR(50)     NOT NULL,
    update_date         TIMESTAMP       NOT NULL
);

CREATE TABLE IF NOT EXISTS platform_lookup (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    revision            INT             DEFAULT 0 NOT NULL,
    name                VARCHAR(255)    UNIQUE NOT NULL,
    readable_name       VARCHAR(255)    UNIQUE NOT NULL,
    create_by           VARCHAR(50)     NOT NULL,
    create_date         TIMESTAMP       NOT NULL,
    update_by           VARCHAR(50)     NOT NULL,
    update_date         TIMESTAMP       NOT NULL
    );

CREATE TABLE IF NOT EXISTS signal_dim_value_lookup (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    dimension_type_id   BIGINT          NOT NULL,
    revision            INT             DEFAULT 0 NOT NULL,
    name                VARCHAR(255)    NOT NULL,
    readable_name       VARCHAR(255)    NOT NULL,
    create_by           VARCHAR(50)     NOT NULL,
    create_date         TIMESTAMP       NOT NULL,
    update_by           VARCHAR(50)     NOT NULL,
    update_date         TIMESTAMP       NOT NULL,
    FOREIGN KEY         (dimension_type_id) REFERENCES signal_dim_type_lookup(id),
    UNIQUE              (dimension_type_id, name),
    UNIQUE              (dimension_type_id, readable_name)
);

CREATE TABLE IF NOT EXISTS event_type_field_template_map (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    event_type_id       BIGINT          NOT NULL,
    field_id            BIGINT          NOT NULL,
    is_immutable        BOOLEAN         NOT NULL,
    FOREIGN KEY         (event_type_id) REFERENCES event_type_lookup(id),
    FOREIGN KEY         (field_id) REFERENCES field_template(id),
    UNIQUE              (event_type_id, field_id)
);

------------------------------------------------------------------ SOJ related tables
CREATE TABLE IF NOT EXISTS soj_event (
  id            BIGINT                  AUTO_INCREMENT PRIMARY KEY,
  revision      INT                     DEFAULT 0 NOT NULL,
  action        VARCHAR(100)            DEFAULT NULL,
  page_id       BIGINT                  NOT NULL,
  module_id     BIGINT                  DEFAULT NULL,
  click_id      BIGINT                  DEFAULT NULL
);
DROP INDEX IF EXISTS idx_action_page_module_click ON soj_event;
CREATE INDEX idx_action_page_module_click ON soj_event (action, page_id, module_id, click_id);

CREATE TABLE IF NOT EXISTS soj_tag (
  id            BIGINT                  AUTO_INCREMENT PRIMARY KEY,
  revision      INT                     DEFAULT 0 NOT NULL,
  soj_name      VARCHAR(100)            NOT NULL,
  name          VARCHAR(100)            NOT NULL,
  description   VARCHAR(255),
  data_type     VARCHAR(100)            NOT NULL,
  schema_path   TEXT                    NOT NULL
);
DROP INDEX IF EXISTS soj_name_idx ON soj_tag;
CREATE INDEX soj_name_idx ON soj_tag (soj_name);

CREATE TABLE IF NOT EXISTS soj_event_tag_map (
    id              BIGINT              AUTO_INCREMENT PRIMARY KEY,
    soj_event_id    BIGINT              NOT NULL,
    soj_tag_id      BIGINT              NOT NULL,
    FOREIGN KEY     (soj_event_id)      REFERENCES soj_event(id),
    FOREIGN KEY     (soj_tag_id)        REFERENCES soj_tag(id),
    UNIQUE          (soj_event_id, soj_tag_id)
);

CREATE TABLE IF NOT EXISTS soj_platform_tag (
  id            BIGINT                  AUTO_INCREMENT PRIMARY KEY,
  revision      INT                     DEFAULT 0 NOT NULL,
  soj_name      VARCHAR(100)            UNIQUE NOT NULL,
  name          VARCHAR(100)            NOT NULL,
  description   VARCHAR(255),
  data_type     VARCHAR(100)            NOT NULL,
  schema_path   TEXT                    NOT NULL
);
DROP INDEX IF EXISTS soj_name_idx ON soj_platform_tag;
CREATE INDEX soj_name_idx ON soj_platform_tag (soj_name);

CREATE TABLE IF NOT EXISTS signal_migration_job (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    revision            INT             DEFAULT 0 NOT NULL,
    job_id              BIGINT          UNIQUE NOT NULL,
    status              VARCHAR(50)     NOT NULL,
    create_by           VARCHAR(50)     NOT NULL,
    create_date         TIMESTAMP       NOT NULL,
    update_by           VARCHAR(50)     NOT NULL,
    update_date         TIMESTAMP       NOT NULL
);

CREATE VIEW staged_signal_staging_view AS
SELECT ss.*, dl.readable_name as domain_readable_name FROM staged_signal ss
JOIN (
    SELECT id, MAX(version) AS max_version
    FROM staged_signal
    GROUP BY id
) mv ON ss.id = mv.id AND ss.version = mv.max_version
LEFT JOIN signal_dim_value_lookup dl
    ON ss.domain = dl.name
LEFT JOIN signal_dim_type_lookup dt
    ON dl.dimension_type_id = dt.id AND dt.name = 'DOMAIN';

CREATE VIEW staged_signal_production_view AS
SELECT ss.*, dl.readable_name as domain_readable_name FROM staged_signal ss
JOIN (
    SELECT id, MAX(version) AS max_version
    FROM staged_signal
    WHERE environment = 'PRODUCTION'
    GROUP BY id
) mv ON ss.id = mv.id AND ss.version = mv.max_version
LEFT JOIN signal_dim_value_lookup dl
    ON ss.domain = dl.name
LEFT JOIN signal_dim_type_lookup dt
    ON dl.dimension_type_id = dt.id AND dt.name = 'DOMAIN';
