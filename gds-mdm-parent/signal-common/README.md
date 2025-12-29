# GDS-MDM - Signal mdm common module

[![Build Status](https://ci.qa.ebay.com/cjsmdm-1485/buildStatus/icon?job=cjsmdm_ECD_PR)](https://ci.qa.ebay.com/cjsmdm-1485/job/cjsmdm_ECD_PR/)

> Signal mdm common module is responsible for sharing the common functionality in between signal service and signal facade.   
> This module contains the model classes to be used in both services, since the facade redirect the requests to the service and the service returns    
> the response to the facade.   

### Application Metadata

> Application name: `cjsmdm`  
> Module name: `signal-common`
> Cloud Console: https://cloud.ebay.com/app/detail/cjsmdm/overview

### Building and Running

> Use maven to build this project. Maven wrapper scripts located in project root directory   
> Use the following command to test (JUnit) the project: `mvn clean test`   
> Use the following command to test (JUnit and IT) the project: `mvn clean verify`   
> Use IntelliJ "run" button to run the "HubMdmApplication" configuration in order to run a project with a dev profile

### Prerequisites

> Java 17, lombok
> Please install lombok plugin and enable annotation processor

### Dependencies

> Lombok, UDC Portal, Spring data

### Build

> Use maven to build this project. Maven wrapper scripts located in project root directory

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
