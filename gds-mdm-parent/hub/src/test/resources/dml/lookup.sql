--domain_lookup
DELETE FROM signal_dim_value_lookup;
INSERT INTO signal_dim_value_lookup (`id`, `dimension_type_id`, `revision`, `name`, `readable_name`, `create_by`, `create_date`, `update_by`,`update_date`) VALUES
--(0, 0, 0, 'VI', 'View Item', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
--(0, 0, 0, 'CART', 'Cart', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
(1, 0, 0, 'SEARCH1', 'Search', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp);

INSERT INTO channel_id_lookup (`revision`, `name`, `readable_name`, `channel_id`, `create_by`, `create_date`, `update_by`, `update_date`) VALUES
(0, 'Paid Search', 'Paid Search', '2', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp);

--business_outcome_lookup
INSERT INTO business_outcome_lookup (`revision`, `name`, `readable_name`, `event_type`, `create_by`, `create_date`, `update_by`,`update_date`) VALUES
--(0, 'bin', 'Buy it Now', 'BBOWAC:BIN', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
(0, 'buyer_unwatched', 'Item unwatched', 'BBOWAC:Watch', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
(0, 'bid', 'Bid on item', 'BBOWAC:Bid', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
(0, 'buyer_accepted_offer', 'Buyer accepted offer', 'BBOWAC:Offer', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
(0, 'buyer_created_offer', 'Buyer created offer', 'BBOWAC:Offer', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
(0, 'buyer_declined_offer', 'Buyer declined offer', 'BBOWAC:Offer', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
(0, 'asq', 'ask a seller question', 'BBOWAC:ASQ', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp);

--signal_type_lookup
INSERT INTO signal_type_lookup (`revision`, `name`, `platform_id`, `logical_data_entity`, `readable_name`, `create_by`, `create_date`, `update_by`,`update_date`) VALUES
--(0, 'PAGE_IMPRESSION', 'CJS', 'touchpoint', 'Module Impression', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
(0, 'MODULE_IMPRESSION', 1, 'touchpoint', 'Module Impression', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
(0, 'ONSITE_CLICK', 1, 'touchpoint', 'Onsite Click', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
(0, 'BUSINESS_OUTCOME', 1, 'touchpoint','Business Outcome', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp);

--event_type_lookup
INSERT INTO event_type_lookup (`revision`, `name`, `readable_name`, `create_by`, `create_date`, `update_by`,`update_date`) VALUES
--(0, 'PAGE_VIEW_ENTRY', 'Page view entry', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
--(0, 'PAGE_VIEW_EXIT', 'Page view exit', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
--(0, 'PAGE_SERVE', 'Page serve', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
(0, 'CLIENT_PAGE_VIEW', 'Client page view', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
(0, 'MODULE_CLICK', 'Module Click', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
(0, 'SOJ_CLICK', 'SOJ Click', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
(0, 'SERVICE_CALL', 'Service Call', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
(0, 'OFFSITE_EVENT', 'Offsite Event', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
(0, 'MODULE_VIEW', 'Module view', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
(0, 'ROI_EVENT', 'Roi event', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
(0, 'CSEVENT', 'CS event', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp);

--surface_type_lookup
INSERT INTO surface_type_lookup (`revision`, `name`, `readable_name`, `create_by`, `create_date`, `update_by`,`update_date`) VALUES
(0, 'IOS', 'iOS', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
(0, 'ANDROID', 'Android', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
(0, 'NODE_FE', 'Node.js Web', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
(0, 'NODE_SERVICE', 'Node.js Service', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
--(0, 'RAPTOR_IO', 'Raptor.io', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp),
(0, 'RAPTOR', 'Raptor', 'gdsmdm', current_timestamp, 'gdsmdm', current_timestamp);
