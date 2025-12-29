# Project Overview

## GDS-MDM-Parent Project

This document provides a high-level overview of the GDS-MDM-Parent project structure, purpose, and components to help AI assistants understand the codebase.

## Project Purpose

The GDS-MDM-Parent project is a Master Data Management (MDM) system as part of eBay's Global Data Services (GDS).

## Project Structure

This is a multi-module Maven project with the following main components:

- **common**: Shared utilities and base classes used across the project without database dependencies (since shared with mdm-facade that have no database)
- **common-svc**: Common service components and interfaces with database dependencies
- **contract**: API contracts and specifications
- **dec**: Data Enrichment and Consumption, or DEC component
- **signal**: Component for processing signals
- **signal-common**: Common utilities specific to signals
- **udf-common**: Common utilities for user-defined functions
- **udf**: User-defined functions component
- **hub**: Central hub/orchestration component

## Key Technologies

Based on the project structure, the following technologies appear to be used:

- **Java 17**: Primary programming language
- **Maven**: Build and dependency management
- **Spring Boot**: Application framework (inferred from application-*.yaml files)
- **MySQL**: Database interactions (based on schema-signal.sql files)
- **Raptor-io**: eBay's internal application framework

## Main Functionality

The system handles:

1. Signal metadata
2. Contract metadata
3. Data Enrichment and Consumption, or DEC metadata
4. udf metadata

## Additional Notes

This overview is based on the observed file structure. For more detailed information, refer to the other documentation files in this directory.
