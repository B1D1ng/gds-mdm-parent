-- moved to satisfy the foreign key constraint in signal_type_lookup.

INSERT INTO platform_lookup (`id`,`revision`, `name`, `readable_name`, `create_by`, `create_date`, `update_by`,`update_date`) VALUES
(1,0, 'CJS', 'User Behavior', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
(2, 0, 'EJS', 'Internal Tools', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
(3, 0, 'ITEM', 'Item', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp);

INSERT INTO signal_type_lookup (`id`, `revision`, `name`, `platform_id`, `logical_data_entity`, `readable_name`, `create_by`, `create_date`, `update_by`,`update_date`) VALUES
(123, 0, 'PAGE_IMPRESSION', 1, 'touchpoint', 'Page Impression', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp);

INSERT INTO signal_physical_storage (`id`, `revision`, `description`, `environment`, `kafka_topic`, `kafka_schema`, `hive_table_name`, `done_file_path`, `create_by`, `create_date`, `update_by`, `update_date`) VALUES
(123, 0, 'CJS Touchpoint', 'UNSTAGED', 'touchpoint_signal_staging', 'schema...', 'touchpoint_signal', '/tmp/touchpoint_signal_done', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp);

INSERT INTO business_outcome_lookup (`revision`, `name`, `readable_name`, `event_type`, `create_by`, `create_date`, `update_by`,`update_date`) VALUES
(0, 'bin', 'Buy it Now', 'BBOWAC:BIN', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp);

INSERT INTO surface_type_lookup (`revision`, `name`, `readable_name`, `create_by`, `create_date`, `update_by`,`update_date`) VALUES
(0, 'RAPTOR_IO', 'Raptor.io', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp);

INSERT INTO soj_platform_tag (soj_name, name, description, data_type, schema_path) VALUES
('app', 'app_id', 'AppId of the mobile app (native and mweb). https://wiki.vip.corp.ebay.com/x/i5OrH', 'java.lang.String', 'event.getEventPayload().getEventProperties().get("app")'),
('viewedTs', 'viewedTs', 'Viewed timestamp, derived from domain client tracking or Surface Tracking view events.', 'java.lang.Long', 'event.eventPayload.timestamp');

INSERT INTO channel_id_lookup (`revision`, `name`, `readable_name`, `channel_id`, `create_by`, `create_date`, `update_by`, `update_date`) VALUES
(0, 'ePN', 'ePN', '1', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp);

INSERT INTO signal_type_physical_storage_map (`id`, `signal_type_id`, `physical_storage_id`) VALUES
(0, 123, 123);

INSERT INTO event_type_lookup (`revision`, `name`, `readable_name`, `create_by`, `create_date`, `update_by`,`update_date`) VALUES
(0, 'PAGE_VIEW_ENTRY', 'Page view entry', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
(0, 'PAGE_VIEW_EXIT', 'Page view exit', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
(0, 'PAGE_SERVE', 'Page serve', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
(0, 'ITEM_SERVE', 'Item serve', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp);

INSERT INTO signal_dim_type_lookup (`id`, `revision`, `name`, `readable_name`, `create_by`, `create_date`, `update_by`,`update_date`) VALUES
(0, 0, 'DOMAIN', 'Domain', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp);

insert into signal_dim_value_lookup (`id`, `dimension_type_id`, `revision`, `name`, `readable_name`, `create_by`, `create_date`, `update_by`,`update_date`) VALUES
(0, 0, 0,  'VI', 'View Item', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp);

--UPDATE sequences SET next_val = 300 WHERE sequence_name = 'signal_seq';
--ALTER TABLE event ALTER COLUMN id RESTART WITH 300;
--ALTER TABLE field ALTER COLUMN id RESTART WITH 300;
--ALTER TABLE attribute ALTER COLUMN id RESTART WITH 300;
