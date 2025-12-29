package com.ebay.behavior.gds.mdm.signal.resource;

import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Suits significantly speed up integration tests by sharing spring context across all test files and all tests inside each file.
 */
@Suite
@IncludeClassNamePatterns({"^.*IT$"})
@SuiteDisplayName("Resource IT Suite")
@SelectPackages("com.ebay.behavior.gds.mdm.signal.resource")
public class ResourceItSuite {
}
