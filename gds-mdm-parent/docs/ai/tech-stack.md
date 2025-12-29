# Technology Stack

## GDS-MDM-Parent Tech Stack

This document outlines the technologies, frameworks, libraries, and tools used in the GDS-MDM-Parent project.

## Core Technologies

| Category | Technology | Purpose |
|----------|------------|---------|
| Programming Language | Java | Primary development language |
| Build System | Maven | Dependency management and build automation |
| Framework | Spring Boot | Application framework |
| API Documentation | OpenAPI/Swagger | API specification and documentation |
| Database | MySql SQL-based RDBMS for production and H2 in-memoty db for testing | Data persistence |

## Frameworks & Libraries

Based on the project structure, the following frameworks and libraries are likely used:

### Backend Frameworks
- **Spring Boot**: Application framework
- **Spring Web**: RESTful API development
- **Spring Data**: Database access abstraction

### Testing Frameworks
- **JUnit**: Unit testing
- **Mockito**: Mocking framework for tests (inferred from mockito-extensions directories)
- **AssertJ**: Fluent assertion framework for test validations

### Utilities
- **Lombok**: Reduces boilerplate code (inferred from lombok.config)

## Development Tools

| Tool | Purpose |
|------|---------|
| Maven | Build automation and dependency management |
| Checkstyle | Code style enforcement |
| PMD | Static code analysis |

## Configuration Management

The application uses YAML-based configuration with environment-specific profiles:
- Dev
- Feature
- Staging
- Pre-Production
- Production

## Database

The project appears to use SQL-based database(s) with schema definitions in schema-signal.sql files located in the test resources directories of specific modules:
- `contract/src/test/resources/schema-signal.sql`
- `dec/src/test/resources/schema-signal.sql`
- `udf/src/test/resources/schema-signal.sql`
- `signal/src/test/resources/schema-signal.sql`

These schema files are likely used for setting up test databases and defining the data structure for integration tests.

## Deployment & Infrastructure

Based on the configuration files, the application is likely deployed in a multi-environment setup, potentially using:
- Containerization (Docker)
- Cloud infrastructure
- CI/CD pipelines

## Additional Notes

This tech stack overview is based on the observed file structure. For more detailed information about specific technologies or implementation details, refer to the other documentation files in this directory.
