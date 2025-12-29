package com.ebay.behavior.gds.mdm.common.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SiteSsoConstants {
    public static final String SITE_SSO_SESSION = "SITE-SSO-SESSION";
    public static final String SITE_SSO_APP_NAME = "cjsmdm";
    public static final String SITE_SSO_SCOPE_NAME = "cjsmdm";
    public static final boolean SITE_SSO_PATRONUS_KEY_AUTO_GENERATE = true;
    public static final boolean SITE_SSO_COOKIE_SECURE = true;
    public static final String SITE_SSO_HOME_URL = "/";
    public static final boolean SITE_SSO_AUTO_LOGIN_REDIRECT = false;
    public static final String SITE_SSO_ROLE_PREFIX = "ROLE_";

    // The following roles are defined in the IDM system and are used to manage user permissions within the application.
    public static final String ROLE_CJS_ADMIN = "cjs-admin";
    public static final String ROLE_GOVERNANCE_APPROVER = "governance-approver";
    public static final String ROLE_TRACKING_MODERATOR = "tracking-moderator";
}
