# Key Files

## GDS-MDM-Parent Key Files

This document highlights the most important files in the project with brief descriptions to help AI assistants understand the codebase structure.

## Configuration Files

| File | Description |
|------|-------------|
| `pom.xml` | Root Maven project configuration defining dependencies and build settings |
| `*/pom.xml` | Module-specific Maven configurations |
| `*/src/main/resources/application-*.yaml` | Environment-specific application configurations |
| `lombok.config` | Lombok configuration for code generation |
| `build-config/checkstyle/*.xml` | Code style enforcement rules |
| `build-config/pmd/*.xml` | Static code analysis rules |

## API Definitions

| File | Description |
|------|-------------|
| `contract/openApi/schema.yaml` | OpenAPI specification for the contract API |
| `signal/openApi/schema.yaml` | OpenAPI specification for the signal API |
| `dec/openApi/schema.yaml` | OpenAPI specification for DEC API |
| `ufd/openApi/schema.yaml` | OpenAPI specification for DEC API |

## Database Scripts

| File | Description |
|------|-------------|
| `signal/src/test/resources/schema-signal.sql` | Database schema definition for signal module |
| `contract/src/test/resources/schema-signal.sql` | Database schema definition for contract module |
| `dec/src/test/resources/schema-signal.sql` | Database schema definition for DEC |
| `udf/src/test/resources/schema-signal.sql` | Database schema definition for udf module |
| `*/src/test/resources/data-signal.sql` | Test data initialization scripts |


## Common Utilities

| File | Description |
|------|-------------|
| `common/src/main/java/com/**/*.java` | Common utility classes and shared functionality |
| `common-svc/src/main/java/com/**/*.java` | Common service interfaces and implementations |

## Test Configuration

| File | Description |
|------|-------------|
| `*/src/test/resources/application-IT.yaml` | Integration test configurations |
| `*/src/test/resources/mockito-extensions/*` | Mockito extensions for testing |

## Additional Notes

This key files overview is based on the observed file structure. The actual importance of specific files may vary based on the project's implementation details. For more detailed information about the project structure and architecture, refer to the other documentation files in this directory.
