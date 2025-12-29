package com.ebay.behavior.gds.mdm.dec.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ApiConstants {

    // API categories
    public static final String LDM_METADATA_API = "/ldms";
    public static final String LDM_BASE_ENTITY_METADATA_API = "/ldms/entities";
    public static final String DATASET_METADATA_API = "/datasets";
    public static final String NAMESPACE_METADATA_API = "/namespaces";
    public static final String PHYSICAL_STORAGE_METADATA_API = "/physical-storages";
    public static final String PHYSICAL_ASSET_METADATA_API = "/physical-assets";
    public static final String PHYSICAL_ASSET_INFRA_API = "/physical-assets-infra";
    public static final String PHYSICAL_ASSET_ATTRIBUTE_API = "/physical-asset-attributes";
    public static final String PHYSICAL_ASSET_INFRA_GP_API = "/physical-assets-infra-global-properties";
    public static final String PIPELINE_METADATA_API = "/pipelines";
    public static final String CHANGE_REQUEST_API = "/change-requests";
    public static final String UDC_API = "/udc";

    // Constants
    public static final String FIELD_GROUP_DATA_TYPE = "FIELD_GROUP";
    public static final int MIN_VERSION = 1;
    public static final String FIELDS = "fields";
    public static final String LDM_NAME = "name";
    public static final String LDM_NAMESPACE_ID = "namespaceId";
    public static final String LDM_TEAM = "team";
    public static final String LDM_TEAM_DL = "teamDl";
    public static final String LDM_OWNERS = "owners";
    public static final String LDM_JIRA_PROJECT = "jiraProject";
    public static final String LDM_PK = "pk";
    public static final String LDM_DOMAIN = "domain";

    // dcs constant
    public static final String DELTA_CHANGE_STREAM_SUFFIX = "_DeltaChangeStream";

    public static final String GDS_MDM_STAGING_CLIENT_NAME = "gds-mdm.staging";
}
