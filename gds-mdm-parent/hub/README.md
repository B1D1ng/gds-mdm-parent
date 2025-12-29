# GDS-MDM - Signal mdm application

[![Build Status](https://ci.qa.ebay.com/cjsmdm-1485/buildStatus/icon?job=cjsmdm_ECD_PR)](https://ci.qa.ebay.com/cjsmdm-1485/job/cjsmdm_ECD_PR/)

> Signal metadata management module is responsible for Signal onboarding, metadata registration,   
> governance, version control, audit, data lineage, discovery, and serving capabilities.

### Application Metadata

> Application name: `cjsmdm`  
> Module name: `signal`
> Cloud Console: https://cloud.ebay.com/app/detail/cjsmdm/overview

### Building and Running

> Use maven to build this project. Maven wrapper scripts located in project root directory   
> Use the following command to test (JUnit) the project: `mvn clean test`   
> Use the following command to test (JUnit and IT) the project: `mvn clean verify`   
> Use IntelliJ "run" button to run the "HubMdmApplication" configuration in order to run a project with a dev profile

### Prerequisites

> Java 17, lombok, Swagger, MySQL, SpringBoot 3, Caffeine cache, Data Governance Portal
> Please install lombok plugin and enable annotation processor

### Dependencies

> Lombok, UDC Portal, MySQL, Spring data

### Build

> Use maven to build this project. Maven wrapper scripts located in project root directory

### Run Parameters

> what run parameters (application and specific vm parameters, if applicable) are available and what do they do? what are the default parameters?

### Security

> Fidelius: https://sam.muse.vip.ebay.com/root/pmsvc/staging/site/tracking/serviceaccount   
> IDM: https://idm.vip.ebay.com/

### Firewall

> There is a firewall rule that allows access to the staging pool from prod pool in order to call UDC injections functionality on promote to staging.   
> Use /go/marmot to configure such rule.  
> Use https://go/fpajobs to check for running or completed jobs that are related to the firewall rule configuration.  
> A firewall rule JIRA ticket: https://jirap.corp.ebay.com/browse/SENTINEL-13884

### Contributing

> Create a feature branch named with a Jira ticket prefix (and optional description): CJS-123_optional_ticket_description   
> Create a PR against a develop branch   
> Ask for 2 PR code reviews

### Release

> Create a release_candidate branch   
> Deploy and test the rc branch in the feature pool    
> Update RELEASE version in the pom.xml   
> Release the rc branch using a release pipeline   
> Create a tag from the rc branch   
> Create a PR against a main branch

### Resources

> Jenkins: https://ci.qa.ebay.com/cjsmdm-1485     
> ECDX: https://cloud.ebay.com/app/detail/cjsmdm/ecd

### Application Architecture

> https://docs.google.com/document/d/12hiEObBOaUfWX0_MihRdqIvYMFimHStxU9IbYodMAa0/edit?usp=sharing   
> if using ADRs link to them here as well

### Unified Data Catalog (UDC)

> UI:
> - Production: https://dgportal.muse.vip.ebay.com/
> - Staging: https://dgportal.muse.qa.ebay.com/metadata/data-lineage
> - DevZone: https://secdev.dgportal.muse.qa.ebay.com/metadata/models
>
> Staged metadata stored (push injection) under UDC Portal and can be accessed only through DG elasticsearch or Graph DB
> - https://github.corp.ebay.com/DataGov/dgclients/tree/master/pushingestion
> - POC: https://docs.google.com/document/d/1NdVdmijucZvZj_IZZbvp1PfE3gVj39741L3qMXl1wXo/edit?pli=1#heading=h.7uhchxmilu5u
> - Data lineage UI: https://dgportal.muse.qa.ebay.com/metadata/data-lineage
>
> Schema:
> - Signal: https://dgportal.muse.qa.ebay.com/metadata/models/detail?entityType=Signal
> - Event: https://dgportal.muse.qa.ebay.com/metadata/models/detail?entityType=TrackingEvent
> - Attribute: https://dgportal.muse.qa.ebay.com/metadata/models/detail?entityType=TrackingEventAttribute
> - Field: https://dgportal.muse.qa.ebay.com/metadata/models/detail?entityType=SignalField
>
> Read API: https://wiki.corp.ebay.com/display/DATAGOV/Data+Governance+Platform+API
> - /metadata/details query from NuDocument
> - /metadata/bulk/entity query lineage from graphDB
> - /metadata/searchES runs search queries against elasticsearch
>
> Schema updates:
> - If metadata types (UnstagedSignal, UnstagedEvent, UnstagedField and UnstagedAttribute) are updated with new properties, the schema must be updated in
    UDC Portal.
> - The schema update process is manual (an email to Michelle) but will be changed to a self-service process in the future.
>
> Monitoring - kafka consumer lag:
> - http://rhs-monitoring.vip.ebay.com/d/CNcPZtviz/consumer-lag?var-streamName=ingestion&var-appIdentity=ingestion84cont&var-clusterType=active&var-zone=lvs&var-groupId=realtime_data_mod_consumer_group&orgId=1&from=now-3h&to=now&var-topic=All&var-partition=All

### Staged Signals Export to HDFS
> This feature offloads the getAll API for Staged Signals in the PRODUCTION environment by exporting signals to HDFS via a DLS pipeline.
> ### Export:
> 
> #### Details:
> * Endpoint: V1 + METADATA + "/export/signal"
> * DLS Pipeline: https://datalakestudio.muse.vip.ebay.com/workspace/45/pipeline/028c6995-edcd-4f50-9a39-23afdb6762af
> * HDFS path: /apps/b_trk/gds/metadata/
> 
> #### Process:
>  1. The pipeline calls the export endpoint hourly.
>  2. The endpoint fetches data from getAllProductionLatestVersions (invoked once per platform: CJS, EJS, ESP).
>  3. Results are stored in a map and written as JSON files in HDFS with the format: staged_signal_<platformType>_<yyyymmdd_hhmm>.json. This is done 3 times (once per platform type).   
>     Example: /apps/b_trk/gds/metadata/staged_signal_esp_20250829_0006.json   
>  4. Consumers can read the latest signals directly from HDFS (using timestamps and platform type in filenames), avoiding direct calls to getAll.   
> 
> ### Cleanup:
> 
> #### Details:
> * Endpoint: V1 + METADATA + "/export/signal/cleanup"
> * DLS Pipeline: https://datalakestudio.muse.vip.ebay.com/workspace/45/pipeline/fbe81fcc-3d44-4065-86e7-366c9862f606
> 
> #### Process:
> 1. The cleanup pipeline identifies stale files in HDFS (older than 7 days).
> 2. It invokes the delete API (via FileSystem library) to remove them, ensuring storage remains clean and up-to-date.

### Documentation

> - Requirements:   https://docs.google.com/document/d/1bfsKBLsqj6hMvV4rqZlb0lkhsxJzVckx2tHCDhi4ans/edit
> - UI mocks: https://www.figma.com/design/60qUGNe5bBRQiZE0RNXl8A/Tracking-Signal?node-id=2344-8109&node-type=canvas&t=p4Pqasiq01EZSDFo-0
> - Nomenclature and Template: https://docs.google.com/spreadsheets/d/1E6PJwNEXbx8OZlRD4yBZivwxVIfNRib1WAAfJwHO7Bw/edit?gid=1782165493#gid=1782165493
> - HLD: https://docs.google.com/document/d/12hiEObBOaUfWX0_MihRdqIvYMFimHStxU9IbYodMAa0/edit?usp=sharing
> - Backlog: https://docs.google.com/document/d/18cPVCiqiDv8D4aBgPDvcJmDY8rOoRYR_uj96B0c6jcM/edit#heading=h.cuu63wbk6z0m
> - Execution plan: https://docs.google.com/spreadsheets/d/1e8h5vTIq_7WpN7uc7gznu5K0ean-nOhsrj8a1dXL0fc/edit?usp=sharing
> - Authentication and Authorization design: https://docs.google.com/document/d/1NCh5Sp5TWFf_sWY367YlkWvTuyH-YkxSLaroi0pTQTc/edit?tab=t.0#heading=h.992x8esqsqcp

### User Interface

> Application: https://cloud.ebay.com/muse/app/cjsportal
> - Dev: https://dev.cjsportal.muse.qa.ebay.com/plans  Points to feature pool 1
> - Staging: https://cjsportal.muse.qa.ebay.com/plans  Points to staging pool 4
> - Production: https://cjsportal.muse.vip.ebay.com/plans
> - Go link: go/cjsportal

### IDM Role based Security

> Onboarding:
> - https://jirap.corp.ebay.com/browse/SIAM-4722
> - https://jirap.corp.ebay.com/browse/SIAM-5279
> - Security domain: See under the /src/test/resources/idm folder
>
> Playbook:
> https://docs.google.com/document/d/1Cn6WQFtN4BDxcM1vhx5J2tbBrcIp4pdHWMle4rbBp54/edit?pli=1&tab=t.0.
>
> Roles:
> https://idm.vip.ebay.com/zta/#/resources/application/ade423c1-866d-4862-a672-3df24b43d0c9?displayName=cjsmdm
>
> Onboarding step-by-step document:   
> https://docs.google.com/document/d/18SUwVpCyoWoRO4bP7azWw3qumzWkZ0sBS6QcIGv1tT4/edit?tab=t.0

### OpenAPI

> Swagger UI: .../v1/openapi.json and .../v1/openapi.yaml.   
> Controller: OpenApiSchemaResource.   
> Location (any module): root/{module}/openApi.   
> To regenerate jsom and yaml schema files please run the following test (per module): OpenApiSchemaResourceIT.   
> Schema files regenerated automatically with every full build.

### Support

> Jira project: https://jirap.corp.ebay.com/projects/CJSONB   
> Email DL: DL-eBay-CORE-CJS <DL-eBay-CORE-CJS@ebay.com>
