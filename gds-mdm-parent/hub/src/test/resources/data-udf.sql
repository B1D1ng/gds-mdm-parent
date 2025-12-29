INSERT INTO udf (`revision`, `name`, `description`, `type`, `language`, `code`, `parameters`, `domain`, `owners`, `current_version_id`, `create_date`, `create_by`, `update_date`, `update_by`, `function_source_type`) VALUES
(0, 'initial_test_udf', 'description','UDF', 'JAVA', 'github link', 'params', 'tracking', 'test@ebay.com', 1L, current_timestamp, 'gmstest', current_timestamp, 'gmstest', 'BUILT_IN_FUNC');

INSERT INTO udf_versions (`revision`, `udf_id`, `version`, `git_code_link`, `parameters`, `status`, `domain`, `owners`, `create_date`, `create_by`, `update_date`, `update_by`, `function_source_type`) VALUES
(0, 1L, 1L, '1', 'params', 'CREATED', 'tracking', 'test@ebay.com', current_timestamp, 'gmstest', current_timestamp, 'gmstest', 'BUILT_IN_FUNC');

INSERT INTO udf_stub (`revision`, `udf_id`, `stub_name`, `description`, `language`, `stub_code`, `stub_parameters`, `stub_runtime_context`, `create_date`, `create_by`, `update_date`, `update_by`, `owners`, `current_version_id`, `current_udf_version_id`, `stub_type`) VALUES
(0, 1L, 'initial_test_udf_stub', 'description', 'FLINK_SQL', 'github link', 'params', 'test',  current_timestamp, 'gmstest', current_timestamp, 'gmstest', 'test@ebay.com',1L,1L, 'PUBLIC');

INSERT INTO udf_stub_versions (`revision`, `udf_stub_id`, `stub_version`, `git_code_link`, `stub_parameters`, `stub_runtime_context`, `create_date`, `create_by`, `update_date`, `update_by`, `stub_type`) VALUES
(0, 1L, 1L, '1','params','text', current_timestamp, 'gmstest', current_timestamp, 'gmstest', 'PUBLIC');

INSERT INTO udf_usage (`revision`, `udf_id`, `usage_type`, `udc_id`, `create_date`, `create_by`, `update_date`, `update_by`) VALUES
(0, 1L, 'CONTRACT', '1',  current_timestamp, 'gmstest', current_timestamp, 'gmstest');
