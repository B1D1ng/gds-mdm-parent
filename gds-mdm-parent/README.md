# GDS-MDM

[![Build Status](https://ci.qa.ebay.com/cjsmdm-1485/buildStatus/icon?job=cjsmdm_ECD_PR)](https://ci.qa.ebay.com/cjsmdm-1485/job/cjsmdm_ECD_PR/)

> GDS Metadata management is a critical component of the GDS platform, providing key functions including clients onboarding,   
> metadata registration, governance, version control, audit, data lineage, discovery, and serving capabilities.

### Application Metadata

> Application name: `cjsmdm`  
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

### Project modules and dependencies

> common - common utilities, models and constants. Includes services without DB connection.
> common-svc - common DB services
> signal-common - signal models to be shared with facade
> signal - signal service module
> dec - dec service module
> contract - contract service module
> udf - udf service module
> hub - Signal Portal back-end application sharing all modules APIs
>
> common <- common-svc, signal-common
> common-svc <- dec, contract, udf
> signal-common <- signal
> dec, contract, udf, signal <- hub

### Jenkins

> Build: https://ci.qa.ebay.com/cjsmdm-1485/job/gds-mdm-build   
> Deploy: https://ci.qa.ebay.com/cjsmdm-1485/job/gds-mdm-deploy

### Run Parameters

> what run parameters (application and specific vm parameters, if applicable) are available and what do they do? what are the default parameters?

### Security

> Fidelius: https://sam.muse.vip.ebay.com/root/pmsvc/staging/site/tracking/serviceaccount   
> IDM: https://idm.vip.ebay.com/

### Firewall

> Use /go/marmot to configure such rule.  
> Use https://go/fpajobs to check for running or completed jobs that are related to the firewall rule configuration.

### Contributing

> Create a feature branch named with a Jira ticket prefix (and optional description): CJSONB-123_optional_ticket_description   
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

### Data Governance Portal

> UI:
> - Production: https://dgportal.muse.vip.ebay.com/
> - Staging: https://dgportal.muse.qa.ebay.com/metadata/data-lineage

### Documentation

> - Requirements:
> - UI mocks:
> - Nomenclature and Template:
> - HLD:
> - Backlog:
> - Execution plan:

### User Interface

> Application: https://cloud.ebay.com/muse/app/cjsportal
> - Dev: https://dev.cjsportal.muse.qa.ebay.com/plans  Points to feature pool 1
> - Staging: https://cjsportal.muse.qa.ebay.com/plans  Points to staging pool 4
> - Production: https://cjsportal.muse.vip.ebay.com/plans

### IDM Role based Security

> Playbook:
> https://docs.google.com/document/d/1Cn6WQFtN4BDxcM1vhx5J2tbBrcIp4pdHWMle4rbBp54/edit?pli=1&tab=t.0.
>
> Roles:
> https://idm.vip.ebay.com/zta/#/resources/application/ade423c1-866d-4862-a672-3df24b43d0c9?displayName=cjsmdm

### Support

> Jira project: https://jirap.corp.ebay.com/projects/CJSONB   
> Email DL: DL-eBay-CORE-CJS <DL-eBay-CORE-CJS@ebay.com>

### Raptor.IO documentation

Please visit Raptor.IO portal for the framework documentation: http://go/raptorio

### IDE configuration for JDK17

JDK17 migration:

* [IntelliJ Setup for JDK17](http://rapioprtl.vip.qa.ebay.com/raptor%20io/latest/docs/3.LocalDev/Raptor.io-Intellij/JDK17-intelij.md)

### Sync signals to github for Processor flink job
> https://docs.google.com/document/d/1n6Dt8i5sDG2HI8pUIEJfAdAJDbgPSnpZxhbOwCMo5jo/edit?tab=t.0#heading=h.revunq338ngi
