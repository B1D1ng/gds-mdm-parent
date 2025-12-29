package com.ebay.behavior.gds.mdm.signal.repository;

import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Suits significantly speed up integration tests by sharing spring context across all test files and all tests inside each file.
 *
 * <p>This suite runs all repository IT classes with IT profile, and so loads application-IT.yaml, where the datasource is configured to use H2.
 * Any repository IT test can be run without IT profile, and if so, will load application-dev.yaml, where the datasource is configured to use MySQL.
 */
@Suite
@IncludeClassNamePatterns({"^.*IT$"})
@SuiteDisplayName("Repository H2 IT Suite")
@SelectPackages("com.ebay.behavior.gds.mdm.signal.repository")
public class RepositoryItSuite {
}
