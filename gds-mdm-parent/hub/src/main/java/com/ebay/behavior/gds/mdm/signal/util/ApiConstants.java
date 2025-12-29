package com.ebay.behavior.gds.mdm.signal.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ApiConstants {

    // API categories
    public static final String TEMPLATE = "/template";
    public static final String DEFINITION = "/definition";
    public static final String METADATA = "/metadata";
    public static final String UDC = "/udc";
    public static final String LOOKUP = "/lookup";
    public static final String BUSINESS_TAGS = "/businessTags";

    // constants
    public static final String ELASTICSEARCH = "/elasticsearch";
    public static final String WITH_ASSOCIATIONS = "withAssociations";
    public static final String WITH_UNSTAGED_DETAILS = "withUnstagedDetails";
    public static final String WITH_LEGACY_FORMAT = "withLegacyFormat";
    public static final String WITH_LATEST_VERSIONS = "withLatestVersions";
    public static final String USE_CACHE = "useCache";

    // Ginger clients
    public static final String PMSVC_GINGER_CLIENT_NAME = "pmsvc";
    public static final String GDS_MDM_STAGING_CLIENT_NAME = "gds-mdm.staging";
    public static final String LEGACY_MDM_CLIENT_NAME = "legacyMdmService.legacyMdmClient";
}
