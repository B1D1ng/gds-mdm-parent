package com.ebay.behavior.gds.mdm.udf.testUtil;

import com.ebay.behavior.gds.mdm.udf.common.model.Udf;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfArtifact;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfModule;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfStub;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfStubArtifact;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfStubModule;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfStubVersions;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfUsage;
import com.ebay.behavior.gds.mdm.udf.common.model.UdfVersions;
import com.ebay.behavior.gds.mdm.udf.common.model.enums.FunctionSourceType;
import com.ebay.behavior.gds.mdm.udf.common.model.enums.Language;
import com.ebay.behavior.gds.mdm.udf.common.model.enums.UdfStatus;
import com.ebay.behavior.gds.mdm.udf.common.model.enums.UdfStubLanguage;
import com.ebay.behavior.gds.mdm.udf.common.model.enums.UdfStubType;
import com.ebay.behavior.gds.mdm.udf.common.model.enums.UdfType;
import com.ebay.behavior.gds.mdm.udf.common.model.enums.UdfUsageType;

import lombok.experimental.UtilityClass;

import java.sql.Timestamp;

@UtilityClass
public class TestModelUtils {

    public static Udf udf() {
        return Udf.builder()
                .name("testUdf")
                .description("test Udf")
                .language(Language.JAVA)
                .type(UdfType.UDF)
                .code("test code")
                .parameters("{\"type\":\"record\",\"name\":\"FunctionSignature\",\"fields\":"
                        + "[{\"name\":\"AnyType\",\"type\":{\"type\":\"record\",\"name\":\"AnyType\",\"fields\":[]}},"
                        + "{\"name\":\"arguments\",\"type\":{\"type\":\"record\","
                        + "\"name\":\"FunctionArguments\",\"fields\":[{\"name\":\"arg0\",\"type\":\"AnyType\"},"
                        + "{\"name\":\"arg1\",\"type\":\"AnyType\"}]}},"
                        + "{\"name\":\"returnValue\",\"type\":\"AnyType\"}]}")
                .domain("tracking")
                .owners("ywang58@ebay.com")
                .currentVersionId(1L)
                .functionSourceType(FunctionSourceType.BUILT_IN_FUNC)
                .createBy("gmstestuser")
                .updateBy("gmstestuser")
                .build();
    }

    public static UdfVersions udfVersion(long udfId) {
        return UdfVersions.builder()
                .udfId(udfId)
                .version(1L)
                .gitCodeLink("1")
                .parameters("{\"type\":\"record\",\"name\":\"FunctionSignature\",\"fields\":"
                        + "[{\"name\":\"AnyType\",\"type\":{\"type\":\"record\",\"name\":\"AnyType\",\"fields\":[]}},"
                        + "{\"name\":\"arguments\",\"type\":{\"type\":\"record\","
                        + "\"name\":\"FunctionArguments\",\"fields\":[{\"name\":\"arg0\",\"type\":\"AnyType\"},"
                        + "{\"name\":\"arg1\",\"type\":\"AnyType\"}]}},"
                        + "{\"name\":\"returnValue\",\"type\":\"AnyType\"}]}")
                .status(UdfStatus.CREATED)
                .domain("tracking")
                .owners("ywang58@ebay.com")
                .functionSourceType(FunctionSourceType.BUILT_IN_FUNC)
                .createBy("gmstestuser")
                .updateBy("gmstestuser")
                .build();
    }

    public static UdfStub udfStub(long udfId) {
        return UdfStub.builder()
                .udfId(udfId)
                .stubName("initial_test_udf_stub")
                .description("test UdfStub")
                .language(UdfStubLanguage.JAVA)
                .stubCode("test code")
                .stubParameters("{\"type\":\"record\",\"name\":\"FunctionSignature\",\"fields\":"
                        + "[{\"name\":\"AnyType\",\"type\":{\"type\":\"record\",\"name\":\"AnyType\",\"fields\":[]}},"
                        + "{\"name\":\"arguments\",\"type\":{\"type\":\"record\","
                        + "\"name\":\"FunctionArguments\",\"fields\":[{\"name\":\"arg0\",\"type\":\"AnyType\"},"
                        + "{\"name\":\"arg1\",\"type\":\"AnyType\"}]}},"
                        + "{\"name\":\"returnValue\",\"type\":\"AnyType\"}]}")
                .stubRuntimeContext("test context")
                .owners("ywang58@ebay.com")
                .currentVersionId(1L)
                .currentUdfVersionId(1L)
                .stubType(UdfStubType.PUBLIC)
                .createBy("gmstestuser")
                .updateBy("gmstestuser")
                .build();
    }

    public static UdfStubVersions udfStubVersion(Long udfStubId) {
        return UdfStubVersions.builder()
                .udfStubId(udfStubId)
                .stubVersion(1L)
                .gitCodeLink("1")
                .stubParameters("{\"type\":\"record\",\"name\":\"FunctionSignature\",\"fields\":"
                        + "[{\"name\":\"AnyType\",\"type\":{\"type\":\"record\",\"name\":\"AnyType\",\"fields\":[]}},"
                        + "{\"name\":\"arguments\",\"type\":{\"type\":\"record\","
                        + "\"name\":\"FunctionArguments\",\"fields\":[{\"name\":\"arg0\",\"type\":\"AnyType\"},"
                        + "{\"name\":\"arg1\",\"type\":\"AnyType\"}]}},"
                        + "{\"name\":\"returnValue\",\"type\":\"AnyType\"}]}")
                .stubRuntimeContext("test context")
                .stubType(UdfStubType.PUBLIC)
                .createBy("gmstestuser")
                .updateBy("gmstestuser")
                .build();
    }

    public static UdfUsage udfUsage(long udfId) {
        return UdfUsage.builder()
                .udfId(udfId)
                .usageType(UdfUsageType.DEC)
                .udcId("123")
                .build();
    }

    public static UdfModule udfModule() {
        return UdfModule.builder()
                        .moduleName("tracking-lib")
                        .gitBranch("main")
                        .gitCommit("test")
                        .version("1.0.0-SNAPSHOT")
                        .snapshot("1758874466")
                        .build();
    }

    public static UdfStubModule udfStubModule() {
        return UdfStubModule.builder()
                            .udfModuleId(1L)
                            .udfStubModuleName("tracking-lib-spark")
                            .platform(UdfStubLanguage.SPARK_SQL)
                            .gitBranch("main")
                            .gitCommit("test")
                            .version("1.0.0-SNAPSHOT")
                            .snapshot("1758874466")
                            .build();
    }

    public static UdfArtifact udfArtifact() {
        return UdfArtifact.builder()
                          .udfModuleId(1L)
                          .version("1.0.0-SNAPSHOT")
                          .buildTime(new Timestamp(System.currentTimeMillis()))
                          .isLatest(true)
                          .build();
    }

    public static UdfStubArtifact udfStubArtifact() {
        return UdfStubArtifact.builder()
                              .udfStubId(1L)
                              .platform(UdfStubLanguage.SPARK_SQL)
                              .uri("hdfs://hubble-lvs/path/to/jar/artifact.jar")
                              .version("1.0.0-SNAPSHOT")
                              .buildTime(new Timestamp(System.currentTimeMillis()))
                              .deployTime(new Timestamp(System.currentTimeMillis()))
                              .isLatest(true)
                              .build();
    }
}
