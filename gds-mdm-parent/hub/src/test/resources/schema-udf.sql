-- noinspection SqlNoDataSourceInspectionForFile
SET MODE MYSQL; /* another h2 way to set mode */

CREATE TABLE IF NOT EXISTS udf (
  id                      BIGINT             AUTO_INCREMENT PRIMARY KEY,
  revision                INT                DEFAULT 0 NOT NULL,
  name                    VARCHAR(255)       NOT NULL,
  description             VARCHAR(2048)      DEFAULT NULL,
  language                VARCHAR(50)        NOT NULL,
  type                    VARCHAR(50)        NOT NULL,
  code                    VARCHAR(2048)      NOT NULL,
  parameters              VARCHAR(2048)      NOT NULL,
  create_date             TIMESTAMP          NOT NULL,
  create_by               VARCHAR(50)        NOT NULL,
  update_date             TIMESTAMP          NOT NULL,
  update_by               VARCHAR(50)        NOT NULL,
  domain                  VARCHAR(255)       NOT NULL,
  owners                  VARCHAR(255)       NOT NULL,
  current_version_id      BIGINT,
  function_source_type    VARCHAR(255)       NOT NULL
);

CREATE TABLE IF NOT EXISTS udf_versions (
  id                      BIGINT             AUTO_INCREMENT PRIMARY KEY,
  revision                INT                DEFAULT 0 NOT NULL,
  udf_id                  BIGINT             NOT NULL,
  version                 BIGINT,
  git_code_link           VARCHAR(2048),
  parameters              VARCHAR(2048)      NOT NULL,
  status                  VARCHAR(50)        NOT NULL,
  domain                  VARCHAR(255)       NOT NULL,
  owners                  VARCHAR(255)       NOT NULL,
  create_date             TIMESTAMP          NOT NULL,
  create_by               VARCHAR(50)        NOT NULL,
  update_date             TIMESTAMP          NOT NULL,
  update_by               VARCHAR(50)        NOT NULL,
  function_source_type    VARCHAR(255)       NOT NULL,
  FOREIGN KEY             (udf_id)           REFERENCES udf(id)
);

CREATE TABLE IF NOT EXISTS udf_stub (
  id                         BIGINT             AUTO_INCREMENT PRIMARY KEY,
  revision                   INT                DEFAULT 0 NOT NULL,
  udf_id                     BIGINT             NOT NULL,
  stub_name                  VARCHAR(255)       NOT NULL,
  description                VARCHAR(2048)      DEFAULT NULL,
  language                   VARCHAR(50)        NOT NULL,
  stub_code                  VARCHAR(2048)      NOT NULL,
  stub_parameters            VARCHAR(2048),
  stub_runtime_context       VARCHAR(2048),
  create_date                TIMESTAMP          NOT NULL,
  create_by                  VARCHAR(50)        NOT NULL,
  update_date                TIMESTAMP          NOT NULL,
  update_by                  VARCHAR(50)        NOT NULL,
  owners                     VARCHAR(255),
  current_version_id         BIGINT,
  current_udf_version_id     BIGINT,
  stub_type                  VARCHAR(255)       NOT NULL,
  FOREIGN KEY                (udf_id)           REFERENCES udf(id)
);

CREATE TABLE IF NOT EXISTS udf_stub_versions (
  id                         BIGINT             AUTO_INCREMENT PRIMARY KEY,
  revision                   INT                DEFAULT 0 NOT NULL,
  udf_stub_id                BIGINT             NOT NULL,
  stub_version               BIGINT,
  git_code_link              VARCHAR(2048),
  stub_parameters            VARCHAR(2048)      NOT NULL,
  stub_runtime_context       VARCHAR(2048)      NOT NULL,
  create_date                TIMESTAMP          NOT NULL,
  create_by                  VARCHAR(50)        NOT NULL,
  update_date                TIMESTAMP          NOT NULL,
  update_by                  VARCHAR(50)        NOT NULL,
  stub_type                  VARCHAR(255)       NOT NULL,
  FOREIGN KEY                (udf_stub_id)      REFERENCES udf_stub(id)
);

CREATE TABLE IF NOT EXISTS udf_usage (
  id                         BIGINT             AUTO_INCREMENT PRIMARY KEY,
  revision                   INT                DEFAULT 0 NOT NULL,
  udf_id                     BIGINT             NOT NULL,
  usage_type                 VARCHAR(255)       NOT NULL,
  udc_id                     VARCHAR(255),
  create_date                TIMESTAMP          NOT NULL,
  create_by                  VARCHAR(50)        NOT NULL,
  update_date                TIMESTAMP          NOT NULL,
  update_by                  VARCHAR(50)        NOT NULL,
  FOREIGN KEY                (udf_id)           REFERENCES udf(id)
);

CREATE TABLE IF NOT EXISTS udf_module (
    id          BIGINT AUTO_INCREMENT           PRIMARY KEY,
    revision    INT         DEFAULT 0           NOT NULL,
    module_name VARCHAR(50)                     NOT NULL,
    git_branch  VARCHAR(50)                     NOT NULL,
    git_commit  VARCHAR(255)                    NOT NULL,
    version     VARCHAR(50)                     NOT NULL,
    snapshot    VARCHAR(255)                    NOT NULL,
    create_date TIMESTAMP                       NOT NULL,
    create_by   VARCHAR(50)                     NOT NULL,
    update_date TIMESTAMP                       NOT NULL,
    update_by   VARCHAR(50)                     NOT NULL
);

CREATE TABLE IF NOT EXISTS udf_stub_module (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    revision             INT DEFAULT 0 NOT NULL,
    udf_module_id        BIGINT        NOT NULL,
    udf_stub_module_name VARCHAR(50)   NOT NULL,
    platform             VARCHAR(50)   NOT NULL,
    git_branch           VARCHAR(50)   NOT NULL,
    git_commit           VARCHAR(255)  NOT NULL,
    version              VARCHAR(50)   NOT NULL,
    snapshot             VARCHAR(255)  NOT NULL,
    create_date          TIMESTAMP     NOT NULL,
    create_by            VARCHAR(50)   NOT NULL,
    update_date          TIMESTAMP     NOT NULL,
    update_by            VARCHAR(50)   NOT NULL
);

CREATE TABLE IF NOT EXISTS udf_artifact (
    id            BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    revision      INT DEFAULT 0 NOT NULL,
    udf_module_id BIGINT        NOT NULL,
    version       VARCHAR(50)   NOT NULL,
    build_time    TIMESTAMP     NOT NULL,
    is_latest     TINYINT       NOT NULL,
    create_date   TIMESTAMP     NOT NULL,
    create_by     VARCHAR(50)   NOT NULL,
    update_date   TIMESTAMP     NOT NULL,
    update_by     VARCHAR(50)   NOT NULL
);

CREATE TABLE IF NOT EXISTS udf_stub_artifact (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    revision           INT DEFAULT 0 NOT NULL,
    udf_stub_module_id BIGINT        NULL,
    udf_stub_id        BIGINT        NOT NULL,
    platform           VARCHAR(50)   NOT NULL,
    uri                VARCHAR(2048) NOT NULL,
    version            VARCHAR(50)   NOT NULL,
    build_time         TIMESTAMP     NULL,
    deploy_time        TIMESTAMP     NULL,
    is_latest          TINYINT       NOT NULL,
    create_date        TIMESTAMP     NOT NULL,
    create_by          VARCHAR(50)   NOT NULL,
    update_date        TIMESTAMP     NOT NULL,
    update_by          VARCHAR(50)   NOT NULL
);