# API Documentation

## Overview

This API allows you to manage User-Defined Functions (UDFs) in the system. You can create, update, and retrieve information about UDFs using this API.

Note: Finally we should go through Facade to reach MDM API (no direct MDM API integration cross components)

## Base URL

The base URL for the UDF MDM API is:
${endpoint}/cjs/mdm/v1/udfmm

## Endpoints

- **Staging:** https://cjsmdm4.vip.qa.ebay.com
- **Production:** https://cjsmdm1.vip.ebay.com

### Create a UDF

- **URL:** `${endpoint}/cjs/mdm/v1/udfmm/udf`
- **Method:** `POST`
- **Content-Type:** `application/json`

#### Request Body

To create a UDF, send a JSON payload in the body of the request. Below is an example of how your JSON should be structured:

```json
{
    "name": "${FunctionName}",
    "description": "${FunctionDescription}",
    "type": "${FunctionType}",
    "language": "JAVA",
    "code": "${JavaUdfGithubLink}",
    "parameters": "${FunctionParameters}",
    "domain": "${Domain}",
    "owners": "${Owners}",
    "createBy": "${CreatorNT}",
    "updateBy": "${UpdatorNT}",
    "functionSourceType": "${FunctionSourceType}"
}
```
#### Function Name

Function name is global unique name for the UDF. If the UDF maps to different UDF stubs crossing platform, define a global name to register for the UDF first

#### Supported Function Type

- **UDF** 
- **UDAF**
- **UDTF**

[FunctionType Definition](https://github.corp.ebay.com/customer-journey-signal/gds-mdm-parent/blob/develop/udf-common/src/main/java/com/ebay/behavior/gds/mdm/udf/common/model/enums/UdfType.java)

#### Java UDF Github Link

If the UDF has a shared Java implementation cross-platforms, put the github link. If you just want to register for a Platform specific UDF such as SparkUdf, FlinkUdf, put "N/A"

#### Function Parameters

Use avro schema to define the Function Parameters

- **FunctionSignature:** This is the schema for the function signature, which includes the arguments and return value.
- **FunctionArguments:** This is the schema for the function arguments, which can include multiple fields depending on the UDF's requirements (the order should be same as the argument order in Function definition)
- **ReturnValue:** This is the schema for the return value of the function.

If a struct is defined in the schema, you can define other record based on your real implementation. And if you leverage horizontal-UDF to develop the Java UDF, please use same Java Pojo class name for the record

```json
{
  "type": "record",
  "name": "FunctionSignature",
  "fields": [
    {
      "name": "arguments",
      "type": {
        "type": "record",
        "name": "FunctionArguments",
        "fields": [
          {"name": "arg0", "type": "string"},
          {"name": "arg1", "type": "long"}
        ]
      }
    },
    {
      "name": "returnValue",
      "type": "long"
    }
  ]
}
```

Example

```json
  "parameters": "{\"type\": \"record\", \"name\": \"FunctionSignature\", \"fields\": [{\"name\": \"arguments\", \"type\": {\"type\": \"record\", \"name\": \"FunctionArguments\", \"fields\": [{\"name\": \"arg0\", \"type\": \"string\"}, {\"name\": \"arg1\", \"type\": \"long\"}]}}, {\"name\": \"returnValue\", \"type\": \"long\"}]}"

```

#### Supported Function Source Type

- **BUILT_IN_FUNC**
- **USER_DEFINED_FUNC**
- **VALUE_FUNC**

[FunctionSourceTypeDefinition](https://github.corp.ebay.com/customer-journey-signal/gds-mdm-parent/blob/develop/udf-common/src/main/java/com/ebay/behavior/gds/mdm/udf/common/model/enums/FunctionSourceType.java)

#### Domain

Contact yuzhang2@ebay.com to define the domain first if the first time to register the UDF. Examples

- **common**
- **tracking**
- **item**

UDF Management System will rely on Domain to manage the UDFs and the domain owners.

#### Owners

Provide the email address list (delimited by comma) of the UDF owners.
Owners could be the UDF creator/updator or a group of contact persons who will maintain the UDFs in the future.

#### Response

- **id:** The unique identifier for the UDF (required for udf update or udf-stub creation)
- **revision:** The revision number of the UDF (required for udf update)
- **currentVersionId:** The current version ID of the UDF (required for udf update to update the record in UDF_VERSIONS)

```json
{
    "id": 17,
    "revision": 1,
    "createBy": "yuzhang2",
    "updateBy": "yuzhang2",
    "createDate": 1754044027000,
    "updateDate": 1754044027000,
    "name": "mytest",
    "description": "my test",
    "language": "JAVA",
    "type": "UDF",
    "code": "https://github.corp.ebay.com/GDS/horizontal-udf/blob/main/tracking-lib/src/main/java/com/ebay/gds/udf/tracking/lib/func/CiidExtractor.java",
    "parameters": "{\"type\": \"record\", \"name\": \"FunctionSignature\", \"fields\": [{\"name\": \"arguments\", \"type\": {\"type\": \"record\", \"name\": \"FunctionArguments\", \"fields\": [{\"name\": \"arg0\", \"type\": \"string\"}]}}, {\"name\": \"returnValue\", \"type\": \"long\"}]}",
    "domain": "tracking",
    "owners": "yuzhang2@ebay.com",
    "currentVersionId": 17,
    "functionSourceType": "VALUE_FUNC",
    "udfVersions": null,
    "udfStubs": null,
    "udfUsages": null
}
```

### Update a UDF

- **URL:** `${endpoint}/cjs/mdm/v1/udfmm/udf/${id}`
- **Method:** `PUT`
- **Content-Type:** `application/json`

#### Request Body

To update a UDF, send a JSON payload in the body of the request. Below is an example of how your JSON should be structured:

- **id:** You can only update an example UDF by its unique identifier (id)
- **revision:** The revision number of the UDF, you can only update the latest revision of the udf
- **currentVersionId:** The current version ID of the UDF, you can only update the latest version of the udf

```json
{
    "id": ${udfId},
    "revision": ${Revision},
    "name": "${FunctionName}",
    "description": "${FunctionDescription}",
    "type": "${FunctionType}",
    "language": "JAVA",
    "code": "${JavaUdfGithubLink}",
    "parameters": "${FunctionParameters}",
    "domain": "${Domain}",
    "owners": "${Owners}",
    "createBy": "${CreatorNT}",
    "updateBy": "${UpdatorNT}",
    "functionSourceType": "${FunctionSourceType}"
}
```

#### Response

- **revision:** The revision number will increase automatically

```json
{
    "id": 17,
    "revision": 2,
    "createBy": "yuzhang2",
    "updateBy": "yuzhang2",
    "createDate": 1752750248000,
    "updateDate": 1752750248000,
    "name": "mytest",
    "description": "my test (need an update)",
    "language": "JAVA",
    "type": "UDF",
    "code": "https://github.corp.ebay.com/GDS/horizontal-udf/blob/main/tracking-lib/src/main/java/com/ebay/gds/udf/tracking/lib/func/CiidExtractor.java",
    "parameters": "{\"type\": \"record\", \"name\": \"FunctionSignature\", \"fields\": [{\"name\": \"arguments\", \"type\": {\"type\": \"record\", \"name\": \"FunctionArguments\", \"fields\": [{\"name\": \"arg0\", \"type\": \"string\"}]}}, {\"name\": \"returnValue\", \"type\": \"long\"}]}",
    "domain": "item",
    "owners": "yuzhang2@ebay.com",
    "currentVersionId": 17,
    "functionSourceType": "VALUE_FUNC",
    "udfVersions": null,
    "udfStubs": null,
    "udfUsages": null
}
```

### Get UDFs by names / ids

- **URL1:** `${endpoint}/cjs/mdm/v1/udfmm/udf/names&names=${name1,name2...}&withAssociations=true`
- **URL2:** `${endpoint}/cjs/mdm/v1/udfmm/udf/ids&ids=${id1,id2...}&withAssociations=true`
- **Method:** `GET`
- **Content-Type:** `application/json`

#### Response

https://cjsmdm4.vip.qa.ebay.com/cjs/mdm/v1/udfmm/udf/ids?ids=17&withAssociations=true

```json
[
  {
    "id": 17,
    "revision": 3,
    "createBy": "yuzhang2",
    "updateBy": "yuzhang2",
    "createDate": 1752750248000,
    "updateDate": 1752750248000,
    "name": "mytest",
    "description": "my test (need an update)",
    "language": "JAVA",
    "type": "UDF",
    "code": "https://github.corp.ebay.com/GDS/horizontal-udf/blob/main/tracking-lib/src/main/java/com/ebay/gds/udf/tracking/lib/func/CiidExtractor.java",
    "parameters": "{\"type\": \"record\", \"name\": \"FunctionSignature\", \"fields\": [{\"name\": \"arguments\", \"type\": {\"type\": \"record\", \"name\": \"FunctionArguments\", \"fields\": [{\"name\": \"arg0\", \"type\": \"string\"}]}}, {\"name\": \"returnValue\", \"type\": \"long\"}]}",
    "domain": "item",
    "owners": "yuzhang2@ebay.com",
    "currentVersionId": 17,
    "functionSourceType": "VALUE_FUNC",
    "udfVersions": [
      {
        "id": 17,
        "revision": 2,
        "createBy": "yuzhang2",
        "updateBy": "yuzhang2",
        "createDate": 1754044027000,
        "updateDate": 1754044027000,
        "udfId": 17,
        "version": 17,
        "gitCodeLink": "https://github.corp.ebay.com/GDS/horizontal-udf/blob/main/tracking-lib/src/main/java/com/ebay/gds/udf/tracking/lib/func/CiidExtractor.java",
        "parameters": "{\"type\": \"record\", \"name\": \"FunctionSignature\", \"fields\": [{\"name\": \"arguments\", \"type\": {\"type\": \"record\", \"name\": \"FunctionArguments\", \"fields\": [{\"name\": \"arg0\", \"type\": \"string\"}]}}, {\"name\": \"returnValue\", \"type\": \"long\"}]}",
        "status": "CREATED",
        "domain": "item",
        "owners": "yuzhang2@ebay.com",
        "functionSourceType": "VALUE_FUNC"
      }
    ],
    "udfStubs": [],
    "udfUsages": []
  }
]
```

### Create a UDF Stub

- **URL:** `${endpoint}/cjs/mdm/v1/udfmm/udf-stub`
- **Method:** `POST`
- **Content-Type:** `application/json`

#### Request Body

To create a UDF Stub, send a JSON payload in the body of the request. Below is an example of how your JSON should be structured:

```json
{
    "udfId": ${udfId},
    "stubName": "${StubName}",
    "description": "${Description}",
    "language": "${StubLanguage}",
    "stubCode": "${StubCodeGithubLink}",
    "owners": "${Owners}",
    "stubParameters": "${FunctionParameters}",
    "stubRuntimeContext": "${RuntimeContext}",
    "currentUdfVersionId": ${UdfVersionId},
    "createBy": "${CreatorNT}",
    "updateBy": "${UpdatorNT}",
    "stubType": "${StubType}"
}
```
#### Stub Name

Stub name is the exact name registered in the platform such as Hadoop and Rheos, downstream will leverage the name to call the functions in different envs. For different platforms, the names can be different - suggest to use a consistent name crossing-platforms which is same as UDF function name, but based on different platform's naming convention it could be different due to database, namespace configuration required.

Example:

Function Name: soj_nvl -> Stub Name (Spark): sojlib.soj_nvl

#### Supported Stub Languages

- **SPARK_SQL**
- **FLINK_SQL**
- **IRIS**

#### Stub Code Github Link

Put the exact platform UDF function code github link

#### Function Parameters

Same as UDF Function Parameters, use avro schema to define the Function Parameters

#### Runtime Context

As different platforms requires different metadata to register the function, it might require different metadata designed for each type of Stub. We will define metadata in stubRuntimeContext in json format. Following are examples which is now required for Spark/Flink

- **jar:** Provide the Jar path for Spark/Flink Stubs.
- **classpath:** Provide the class path for Spark/Flink Stubs.
- **package:** Provide the package name for Flink stubs.

Note for spark:

- If you're using Hadoop ebay-udf for the stub, you don't need to provide jar/classPath. They're already managed by ebay-udf when creating permanent function in Spark env.
- If you're using horizontal-udf or self-deployed UDF for the stub, you need to provide the jar/classPath. The jar should be uploaded to the Spark cluster and the classpath should be the full class path of the function.

For ebay-udf, stub type will consider it as "PUBLIC" function which doesn't require any runtime context registered. For other cases, now they're all considered as "PRIVATE" functions and we need to register for additional runtimeContext to support the pipeline generator retrieve the infos to create temporary function in the code.

Note for flink:

- If you're using horizontal-udf or self-deployed UDF deployed by Rheos portal for the stub, you need to provide the classpath/package. The jar should be uploaded to the Rheos NuObject and configured in the package and the classpath should be the full class path of the function.
- (WIP, not available yet) If you're using next-gen horizontal-udf deployed by DLS ADLC, you need to provide the jar/classpath same as spark stubs. The jar should be uploaded to the DLS NuObject and the classpath should be the full class path of the function.

Example (Private Spark Stub)

```json
  "stubRuntimeContext": "{\"jar\": \"viewfs://apollo-rno/apps/b_adlc/b_trk/gdsudfsparkapp/1.0.0-SNAPSHOT/latest/tracking-lib-spark-adaptor-1.0-SNAPSHOT-with-dependencies.jar\", \"classpath\": \"com.ebay.gds.udf.adaptor.tracking.lib.func.CiidExtractorSpark\"}"
```

Example (Public Spark Stub)

```json
  "stubRuntimeContext": "{}"
```

Example (Private Flink Stub)

```json
  "stubRuntimeContext": "{\"classpath\": \"com.ebay.gds.udf.adaptor.tracking.lib.func.CiidExtractorFlink\",\"package\": \"gds-udf-tracking-lib\"}"
```

#### Supported Stub Type

- **PUBLIC**: Stub is a public function which can be directly used by all users in the platform.
- **PRIVATE**: Stub is a private function, any code need to use the function requires to create temporary function in the code first based on metadata from stubRuntimeContext

#### Request Example

```json
{
    "udfId": 17,
    "stubName": "trackinglib.mytest",
    "description": "mytest",
    "language": "SPARK_SQL",
    "stubCode": "https://github.corp.ebay.com/GDS/udf-adaptors/blob/main/spark/tracking-lib-spark/src/main/java/com/ebay/gds/udf/adaptor/tracking/lib/func/CiidExtractorSpark.java",
    "owners": "yuzhang2@ebay.com",
    "stubParameters": "{\"type\": \"record\", \"name\": \"FunctionSignature\", \"fields\": [{\"name\": \"arguments\", \"type\": {\"type\": \"record\", \"name\": \"FunctionArguments\", \"fields\": [{\"name\": \"arg0\", \"type\": \"string\"}]}}, {\"name\": \"returnValue\", \"type\": \"long\"}]}",
    "stubRuntimeContext": "{\"jar\": \"viewfs://apollo-rno/apps/b_adlc/b_trk/gdsudfsparkapp/1.0.0-SNAPSHOT/latest/tracking-lib-spark-adaptor-1.0-SNAPSHOT-with-dependencies.jar\", \"classpath\": \"com.ebay.gds.udf.adaptor.tracking.lib.func.CiidExtractorSpark\"}"
,
    "currentUdfVersionId": 17,
    "createBy": "yuzhang2",
    "updateBy": "yuzhang2",
    "stubType": "PRIVATE"
}
```

#### Response

```json
{
    "id": 19,
    "revision": 1,
    "createBy": "yuzhang2",
    "updateBy": "yuzhang2",
    "createDate": 1754283013000,
    "updateDate": 1754283013000,
    "udfId": 17,
    "stubName": "trackinglib.mytest",
    "description": "mytest",
    "language": "SPARK_SQL",
    "stubCode": "https://github.corp.ebay.com/GDS/udf-adaptors/blob/main/spark/tracking-lib-spark/src/main/java/com/ebay/gds/udf/adaptor/tracking/lib/func/CiidExtractorSpark.java",
    "stubParameters": "{\"type\": \"record\", \"name\": \"FunctionSignature\", \"fields\": [{\"name\": \"arguments\", \"type\": {\"type\": \"record\", \"name\": \"FunctionArguments\", \"fields\": [{\"name\": \"arg0\", \"type\": \"string\"}]}}, {\"name\": \"returnValue\", \"type\": \"long\"}]}",
    "stubRuntimeContext": "{\"jar\": \"viewfs://apollo-rno/apps/b_adlc/b_trk/gdsudfsparkapp/1.0.0-SNAPSHOT/latest/tracking-lib-spark-adaptor-1.0-SNAPSHOT-with-dependencies.jar\", \"classpath\": \"com.ebay.gds.udf.adaptor.tracking.lib.func.CiidExtractorSpark\"}",
    "owners": "yuzhang2@ebay.com",
    "currentVersionId": 19,
    "currentUdfVersionId": 17,
    "stubType": "PRIVATE",
    "udfStubVersions": null
}
```
